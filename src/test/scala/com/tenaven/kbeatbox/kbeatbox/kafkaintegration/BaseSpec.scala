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

import com.typesafe.scalalogging.Logger
import net.manub.embeddedkafka.{EmbeddedKafkaConfig, UUIDs}
import net.manub.embeddedkafka.streams.EmbeddedKafkaStreamsAllInOne
import org.apache.kafka.streams.{KafkaStreams, Topology}
import org.scalatest.Suite

/**
  * creates stream by initiating the embedded kafka methods
  * */
trait BaseSpec extends Suite with EmbeddedKafkaStreamsAllInOne{
  val logger : Logger

  def createStreams(topicsToCreate: Seq[String], topology: Topology, extraConfig: Map[String, AnyRef] = Map.empty)
                   (block: KafkaStreams => Any)
                   (implicit config: EmbeddedKafkaConfig): Any =
    withRunningKafka {
      topicsToCreate.foreach(topic => createCustomTopic(topic))
      val streamId = UUIDs.newUuid().toString
      logger.debug(s"Creating stream with Application ID: [$streamId]")

      val streams = new KafkaStreams(topology, streamConfig(streamId, extraConfig))

      streams.start()

      Thread.sleep(2000)

      try {
        block(streams)
      } finally {
        streams.close()
      }
    }(config)
}

