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
package net.deeppay.kmicrostreams.kbeatbox.kafkaintegration

import java.{lang, util}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.lightbend.kafka.scala.streams.{KStreamS, StreamsBuilderS}
import net.deeppay.kmicrostreams.kbeatbox.api._
import net.deeppay.kmicrostreams.kbeatbox.finitestatemachine._
import net.deeppay.kmicrostreams.kbeatbox.stateapi._
import net.deeppay.kmicrostreams.utils.GenericTopologyBuilder
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
