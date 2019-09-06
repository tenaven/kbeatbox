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
