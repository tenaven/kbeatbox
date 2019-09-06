/*
 *  _
 * / |_ ___ _ __   __ ___   _____ _ __
 * | __/ _ \ '_ \ / _` \ \ / / _ \ '_ \
 * | ||  __/ | | | (_| |\ V /  __/ | | |
 * |_| \___|_| |_|\__,_| \_/ \___|_| |_|
 *
 *        Copyright 2019 TENAVEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tenaven.kbeatbox.kbeatbox.kafkaintegration

import com.lightbend.kafka.scala.streams.KStreamS
import com.tenaven.kbeatbox.kbeatbox.api.{HeartBeat, HeartBeatCondition, InputEvent, ScheduleTask, Trace}
import com.typesafe.scalalogging.Logger
import net.manub.embeddedkafka.Codecs.{stringKeyValueCrDecoder, _}
import net.manub.embeddedkafka.ConsumerExtensions._
import net.manub.embeddedkafka.{EmbeddedKafkaConfig, UUIDs}
import org.apache.kafka.streams.KafkaStreams
import org.scalatest.{Matchers, WordSpec}

// Take inspiration from :  https://github.com/apache/kafka/blob/trunk/streams/src/test/java/org/apache/kafka/streams/integration/EosIntegrationTest.java
class KBeatboxKafkaIntegrationAtomicitySpec extends WordSpec
  with Matchers
  with BaseSpec {

  val logger = Logger(classOf[KBeatboxKafkaIntegrationAtomicitySpec])
  implicit val config: EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(kafkaPort = 7333, zooKeeperPort = 7001)

  val (inTopic, outTopic) = ("in", "out")

  "Kbeatbox stream topology" should {
    "be able to restart without duplicate " +
      "outputs when exception is thrown" in {

      // https://developer.jboss.org/wiki/BMUnitUsingBytemanWithJUnitOrTestNGFromMavenAndAnt?_sscc=t#top
      class KbeatboxKafkaTransformerException extends KbeatboxKafkaTransformer {
        var traceNumber = 0

        override def forwardTrace(key: String, trace: Trace): Unit = {
          logger.warn("injection")
          traceNumber += 1
          if (traceNumber == 1) {
            throw new RuntimeException(s"Should stop process... on trace [$trace]")
          } else {
            // nothing to do
            super.forwardTrace(key, trace)
          }

        }
      }
      class KBeatboxTopologyBuilderException extends KBeatboxTopologyBuilder {

        override def transformInputEventToTraceFromState(inputStream : KStreamS[String, InputEvent]) : KStreamS[String, Trace] =
          inputStream transform(
            () =>new KbeatboxKafkaTransformerException(),
            super.stateStoreNames : _*
          )
      }
      // throws topology exception that interrupts KStream
      val topologyBuilder = new KBeatboxTopologyBuilderException()

      val eventList = List(
        ScheduleTask("a", Set(HeartBeatCondition(1L))),
        ScheduleTask("b", Set(HeartBeatCondition(2L))),
        HeartBeat(1L),
        ScheduleTask("b", Set(HeartBeatCondition(3L))),
        HeartBeat(2L),
        HeartBeat(3L))


      val topicsToCreate = Seq(inTopic, outTopic)
      val topology = topologyBuilder.buildTopology(inTopic, outTopic)
      val extraConfig = Map.empty[String, AnyRef]

      withRunningKafka {
        topicsToCreate.foreach(topic => createCustomTopic(topic))
        eventList.foreach(
          event => publishToKafka[String, InputEvent](inTopic, "a",
            event)(config, stringSerializer,
            topologyBuilder.inputValueSerdes.serializer())
        )


        var streamId = UUIDs.newUuid().toString
        var streams = new KafkaStreams(topology, streamConfig(streamId, extraConfig))
        streams.start()


        var state: KafkaStreams.State = streams.state()
        while (state.isRunning) {

          Thread.sleep(100)
          println(s"state = [$state]")

          state = streams.state()
        }

        streams.close()
        streams.cleanUp()


        streamId = UUIDs.newUuid().toString
        streams = new KafkaStreams(new KBeatboxTopologyBuilder().buildTopology(inTopic, outTopic), streamConfig(streamId, extraConfig))
        streams.start()

        try {

          Thread.sleep(1000)
          withConsumer[String, String, Unit] { consumer =>
            val consumedMessages: Stream[(String, String)] =
              consumer.consumeLazily(outTopic)
            consumedMessages.take(3).map(_._2) should be(
              Stream("{\"type\":\"StartedTask\",\"taskId\":\"a\",\"execTime\":1}",
              "{\"type\":\"StartedTask\",\"taskId\":\"b\",\"execTime\":3}"))// returns no duplicate output stream after re-initialization
          }
        } finally {
          streams.close()
        }
      }(config)
    }
  }

}





