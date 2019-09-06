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