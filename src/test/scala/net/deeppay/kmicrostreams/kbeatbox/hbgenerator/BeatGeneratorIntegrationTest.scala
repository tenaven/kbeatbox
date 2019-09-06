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
package net.deeppay.kmicrostreams.kbeatbox.hbgenerator

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


