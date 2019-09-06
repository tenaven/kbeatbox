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
package com.tenaven.kbeatbox.kbeatbox.hbgenerator

import java.util

import com.typesafe.scalalogging.Logger
import net.manub.embeddedkafka.{EmbeddedKafkaConfig, UUIDs}
import org.scalatest.{Matchers, WordSpec}
import net.manub.embeddedkafka.streams.EmbeddedKafkaStreamsAllInOne
import org.apache.kafka.streams.processor.ThreadMetadata
import org.apache.kafka.streams.{KafkaStreams, Topology}

import scala.collection.{JavaConverters, immutable, mutable}

class DistributedHeartbeatIntegrationTest extends WordSpec with
  Matchers with EmbeddedKafkaStreamsAllInOne {

  val logger = Logger(classOf[DistributedHeartbeatIntegrationTest])
  val (inTopic, outTopic) = ("in", "out")

  "Distributed heartbeat generator" should {
    "generate and send heartbeats" in {

      val topologyBuilder = new DistributedHeartbeatTopologyBuilder

      runParallelStreams(Seq(inTopic, outTopic), topologyBuilder.buildTopology(inTopic, outTopic))( (streams : Seq[KafkaStreams]) => {
        Thread.sleep(10000)
        logger.warn("Stopping prematurely Stream with Task")
        val streamWithOneActiveTask: Seq[KafkaStreams] = streams.filter(stream => {
          val threadMetadatas = JavaConverters.asScalaSet(stream.localThreadsMetadata())
          threadMetadatas.exists(threadMetadata => threadMetadata.activeTasks().size() > 0)
        })
        streamWithOneActiveTask.head.close()

        Thread.sleep(10000)
        // TODO add assert on consuming message.
      })
    }
  }

  def runParallelStreams(topicsToCreate: Seq[String], topology: Topology,
                         extraConfig: Map[String, AnyRef] = Map.empty,
                         nbParallelStreams : Int = 3)
                                 (block: Seq[KafkaStreams] => Any)
                                 (implicit config: EmbeddedKafkaConfig): Any = {
  withRunningKafka {
      topicsToCreate.foreach(topic => createCustomTopic(topic))
      val streamId = UUIDs.newUuid().toString
      logger.debug(s"Creating stream with Application ID: [$streamId]")
      val streams: immutable.Seq[KafkaStreams] = (1 to nbParallelStreams).map(x => new KafkaStreams(topology, streamConfig(streamId, extraConfig)))
      streams.foreach(
        _.start()
      )

      try {
        block(streams)
      } finally {
        streams.foreach( _.close())
      }
    }(config)
  }
}