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

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, AckEventCondition, HeartBeat, HeartBeatCondition, InputEvent, ScheduleTask}
import com.typesafe.scalalogging.Logger
import net.manub.embeddedkafka.Codecs.{stringKeyValueCrDecoder, _}
import net.manub.embeddedkafka.ConsumerExtensions._
import net.manub.embeddedkafka.EmbeddedKafkaConfig
import org.scalatest.{Matchers, WordSpec}

/**
  * Tests the integration of kafka with the FSM.
  */
class KbeatboxKafkaIntegrationSpec extends WordSpec
  with Matchers
  with BaseSpec {

  val logger = Logger(classOf[KbeatboxKafkaIntegrationSpec])


  implicit val config: EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(kafkaPort = 7333, zooKeeperPort = 7001)

  val (inTopic, outTopic) = ("in", "out")


  "Kbeatbox stream topology" should {
    "accept message and send back message" in {
      val topologyBuilder = new KBeatboxTopologyBuilder()


      createStreams(Seq(inTopic, outTopic), topologyBuilder.buildTopology(inTopic, outTopic)) {
        streams => {

          val eventList: Stream[InputEvent] = Stream(ScheduleTask("A", Set(HeartBeatCondition(1L))),
            ScheduleTask("B", Set(HeartBeatCondition(2L))),
            ScheduleTask("C", Set(AckEventCondition("A"), AckEventCondition("B"))),
            HeartBeat(1L),
            HeartBeat(2L),
            AckEvent("A"),
            AckEvent("B"),
            AckEvent("C"),
            ScheduleTask("A", Set(HeartBeatCondition(3L))),
            ScheduleTask("A", Set(HeartBeatCondition(4L))),
            ScheduleTask("A", Set())
          )
          eventList.foreach(
            event => publishToKafka[String, InputEvent](inTopic, "a",
              event)(config, stringSerializer,
              topologyBuilder.inputValueSerdes.serializer())
          )

          withConsumer[String, String, Unit] { consumer =>
            val consumedMessages: Stream[(String, String)] =
              consumer.consumeLazily(outTopic)
            consumedMessages.take(1).map(_._2) should be(
              Stream("{\"type\":\"StartedTask\",\"taskId\":\"A\",\"execTime\":1}"))
          }
        }
      }
    }
  }
}
