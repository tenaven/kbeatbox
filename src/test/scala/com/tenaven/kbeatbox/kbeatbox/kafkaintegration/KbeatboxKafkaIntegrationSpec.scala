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
