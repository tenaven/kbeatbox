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
package com.tenaven.kbeatbox.kbeatbox.hbgenerator

import net.manub.embeddedkafka.{Consumers, EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.{Matchers, WordSpec}
import net.manub.embeddedkafka.ConsumerExtensions._
import net.manub.embeddedkafka.Codecs._
import net.manub.embeddedkafka.streams.EmbeddedKafkaStreamsAllInOne

import scala.util.Random

// TODO : This is a Single Point Of Failure (Spof). Distribute it so it is resilient.
//        But beware : the various machines on which all the node are running might not be
//                     synchronized in time. Maybe research subject on internet.



class BeatGeneratorIntegrationTest extends WordSpec with Matchers with EmbeddedKafkaStreamsAllInOne {

  private val port: Int = Random.nextInt(10000) + 7000 // FIXME why is the port random ?
  implicit val config: EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(kafkaPort = port, zooKeeperPort = 7001)
  val heartbeatOutputTopic = "heartBeatEvent"

  "beat generator" should {
    "generate and send heartbeats" in {

      withRunningKafka({

        EmbeddedKafka.createCustomTopic(heartbeatOutputTopic)

        val generator = new BeatGenerator(BeatGeneratorConfiguration(bootstrapServer = s"localhost:$port"))
        // exit thread
        val thread = new Thread(() => {
          generator.run()
        })

        thread.start()
        Thread.sleep(1000 * 5)
        generator.running = false

        withConsumer[String, String, Unit] { consumer =>
          val consumedMessages: Stream[(String, String)] =
            consumer.consumeLazily(heartbeatOutputTopic)
          consumedMessages.take(5).map(_._2).foreach(heartbeat => {
            heartbeat should startWith("HeartBeat(")
          })
        }
      })
    }
  }
}


