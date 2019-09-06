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

import java.{lang, util}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.lightbend.kafka.scala.streams.{KStreamS, StreamsBuilderS}
import com.tenaven.kbeatbox.kbeatbox.api.{InputEvent, InputEventDeserializer, InputEventSerializer, Trace, TraceDeserializer, TraceSerializer}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{PriorityQueueTaskState, PriorityQueueTaskStateDeserializer, PriorityQueueTaskStateSerializer}
import com.tenaven.kbeatbox.utils.GenericTopologyBuilder
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serdes, Serializer}
import org.apache.kafka.streams.state.Stores

/**
  * A serializer for Trace
  */
class SetStringSerializer extends Serializer[Set[String]] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(topic: String, data: Set[String]) : Array[Byte]= {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.canSerialize(classOf[Set[String]])
    mapper.writeValueAsString(data).getBytes()
  }

  override def close(): Unit = {}
}
/**
  * A deserializer for Trace
  */
class SetStringDeserializer extends Deserializer[Set[String]] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]) : Set[String] = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    if (data != null) {
      mapper.readValue(data, classOf[Set[String]])
    } else {
      null
    }
  }
}
/**
  * initialize a KBeatboxTopology to use in KafkaStreams
  */
class KBeatboxTopologyBuilder extends GenericTopologyBuilder[InputEvent, Trace] {

  val priorityQueueTaskStateSerdes: Serde[PriorityQueueTaskState] = Serdes.serdeFrom[PriorityQueueTaskState](new PriorityQueueTaskStateSerializer(), new PriorityQueueTaskStateDeserializer())

  override def inputValueSerdes: Serde[InputEvent] = Serdes.serdeFrom[InputEvent](new InputEventSerializer(), new InputEventDeserializer())

  override def outputValueSerdes: Serde[Trace] = Serdes.serdeFrom[Trace](new TraceSerializer(), new TraceDeserializer())

  val longSerdes: Serde[lang.Long] = Serdes.Long()

  val setStringSerdes: Serde[Set[String]] = Serdes.serdeFrom[Set[String]](new SetStringSerializer(), new SetStringDeserializer())


  val stateStores = List(
    (KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore, stringSerdes, longSerdes),
    (KBeatboxKafkaStateStoreConstants.KBeatboxTaskStore, stringSerdes, priorityQueueTaskStateSerdes),
    (KBeatboxKafkaStateStoreConstants.KBeatboxhbIndex, longSerdes, setStringSerdes),
    (KBeatboxKafkaStateStoreConstants.KBeatboxAckEventIndex, stringSerdes, setStringSerdes)
  )

  def stateStoreNames: List[String] = stateStores.map(_._1)
  override def configureStreamsBuilder(streamBuilder: StreamsBuilderS): Unit = {
    stateStores.foreach(
      stateStoreTuple => {
        val (stateStoreName, keySerdes, valueSerdes) = stateStoreTuple
        createStateStore(streamBuilder, stateStoreName, keySerdes, valueSerdes)
      }
    )
  }

  private def createStateStore(streamBuilder: StreamsBuilderS, stateStoreName: String, keySerdes: Serde[_], valueSerdes: Serde[_]) = {
    val storeBuilder = Stores.keyValueStoreBuilder(
      Stores.persistentKeyValueStore(stateStoreName),
      keySerdes,
      valueSerdes).withCachingEnabled()
    streamBuilder.addStateStore(storeBuilder)
  }

  def transformInputEventToTraceFromState(inputStream : KStreamS[String, InputEvent]) : KStreamS[String, Trace] = {
    inputStream transform(
      () => new KbeatboxKafkaTransformer(),
      stateStoreNames: _*
    )
  }


  override def completeExec: KStreamS[String, InputEvent] => KStreamS[String, Trace] = transformInputEventToTraceFromState
}
