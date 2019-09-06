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

import java.util.stream.Collectors

import org.apache.kafka.common.serialization.Serializer
import com.lightbend.kafka.scala.iq.services.HostStoreInfo
import net.manub.embeddedkafka.Codecs.stringSerializer
import org.apache.kafka.connect.errors.NotFoundException
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.StreamsMetadata

import scala.collection.JavaConverters._

class MetadataService(val streams: KafkaStreams) {

  /**
    * Get the metadata for all of the instances of this Kafka Streams application
    *
    * @return List of { @link HostStoreInfo}
    */
  def streamsMetadata() : List[HostStoreInfo] = {

    // Get metadata for all of the instances of this Kafka Streams application
    val metadata = streams.allMetadata
    mapInstancesToHostStoreInfo(metadata)
  }

  /**
    * Get the metadata for all instances of this Kafka Streams application that currently
    * has the provided store.
    *
    * @param store The store to locate
    * @return List of { @link HostStoreInfo}
    */
  def streamsMetadataForStore(store: String) : List[HostStoreInfo] = {

    // Get metadata for all of the instances of this Kafka Streams application hosting the store
    val metadata = streams.allMetadataForStore(store)
    mapInstancesToHostStoreInfo(metadata)
  }

  /**
    * Find the metadata for the instance of this Kafka Streams Application that has the given
    * store and would have the given key if it exists.
    * @return { @link HostStoreInfo}
    */
  def streamsMetadataForStoreAndKey[T](store: String, key: T, serializer: Serializer[T]) : HostStoreInfo = {
    // Get metadata for the instances of this Kafka Streams application hosting the store and
    // potentially the value for key
    val metadata = streams.metadataForKey(KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore, KBeatboxKafkaStateStoreConstants.lastReceivedHeartBeatKeyConstant, stringSerializer)
    if (metadata == null)
      throw new NotFoundException(
        s"No metadata could be found for store : ${KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore}, and key type : ${KBeatboxKafkaStateStoreConstants.lastReceivedHeartBeatKeyConstant.getClass.getName}")

    HostStoreInfo(metadata.host, metadata.port, metadata.stateStoreNames.asScala.toSet)
  }

  def mapInstancesToHostStoreInfo(metadatas : java.util.Collection[StreamsMetadata]) : List[HostStoreInfo] = {

    metadatas.stream.map[HostStoreInfo](metadata =>
      HostStoreInfo(
        metadata.host(),
        metadata.port,
        metadata.stateStoreNames.asScala.toSet))
      .collect(Collectors.toList())
      .asScala.toList
  }

}
