/*
 *      _                 ____               ____    _    ____
 *   __| | ___  ___ _ __ |  _ \ __ _ _   _  / ___|  / \  / ___|
 *  / _` |/ _ \/ _ \ '_ \| |_) / _` | | | | \___ \ / _ \ \___ \
 * | (_| |  __/  __/ |_) |  __/ (_| | |_| |  ___) / ___ \ ___) |
 *  \__,_|\___|\___| .__/|_|   \__,_|\__, | |____/_/   \_\____/
 *                 |_|               |___/
 *
 * (c) 2018 deepPay SAS
 * All rights reserved
 *
 * This software is made available to third parties by deepPay free of charge and subject to the following license and conditions :
 * (i) it is provided independently of any existing contract(s) or relationships;
 * (ii) it is provided as-is and for educational purposes only;
 * (iii) it is provided without any warranty or support;
 * (iv) deepPay accepts no liability in relation to the use of this software whatsoever; and
 * (v) this license shall continue until terminated, in deepPay sole discretion, on notice to the user
 */
package net.deeppay.kmicrostreams.kbeatbox.kafkaintegration

import com.lightbend.kafka.scala.streams.KStreamS
import com.typesafe.scalalogging.Logger
import net.deeppay.kmicrostreams.kbeatbox.api._
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





