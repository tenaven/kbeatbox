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
package com.tenaven.kbeatbox.kbeatbox.stateapi

import java.util
import java.util.Map

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.tenaven.kbeatbox.kbeatbox.api.{AckEventCondition, Condition, HeartBeatCondition}
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

import scala.collection.JavaConverters
import scala.collection.immutable.TreeMap

/**
  * Implements a priority queue on top of the SingleTaskState.
  */
case class PriorityQueueTaskState(@JsonProperty("taskName") taskName : String,
                                  @JsonProperty("priorityQueue")
                                  @JsonDeserialize(using = classOf[PriorityQueueDeserializer]) priorityQueue : TreeMap[Int, Set[Condition]] = TreeMap.empty,
                                  @JsonProperty("matchedConditions") matchedConditions : Set[Condition],
                                  @JsonProperty("isTriggered") isTriggered : Boolean)

/**
  * A deserializer for priorityQueue
  */

class PriorityQueueDeserializer extends JsonDeserializer[TreeMap[Int, Set[Condition]]] {
  val logger = Logger(classOf[PriorityQueueDeserializer])

  private def deserializeCondition(conditionNode : JsonNode) : Option[Condition] = {
    val maybeConditionNodeType = Option(conditionNode.get("type")).map(_.asText())

    maybeConditionNodeType match {
      case Some("AckEventCondition") => Some(AckEventCondition(conditionNode.get("matchId").asText()))
      case Some("HeartBeatCondition") => Some(HeartBeatCondition(conditionNode.get("fromTime").asLong()))
      case Some(unknownType) => {
        logger.warn(s" type [$unknownType] unknown")
        None
      }
      case _ => {
        logger.warn("no type defined")
        None
      }
    }
  }

  private def deserializePriorityEntry(entry: Map.Entry[String, JsonNode]): (Int, Set[Condition]) = {

    val conditionNodeIterator: Iterator[JsonNode] = JavaConverters.asScalaIterator(entry.getValue.elements())
    val conditions: Set[Condition] = conditionNodeIterator.flatMap(deserializeCondition _).toSet
    val priorityKey = entry.getKey.toInt

    priorityKey -> conditions
  }

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): TreeMap[Int, Set[Condition]] = {
    val tree : JsonNode = p.getCodec.readTree(p)
    val iterator = JavaConverters.asScalaIterator(tree.fields())

    TreeMap[Int, Set[Condition]](iterator.map(deserializePriorityEntry _).toSeq : _*)
  }
}


/**
  * A serializer for PriorityQueueTaskState
  */
class PriorityQueueTaskStateSerializer extends Serializer[PriorityQueueTaskState] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(topic: String, data: PriorityQueueTaskState) : Array[Byte]= {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.canSerialize(classOf[PriorityQueueTaskState])
    mapper.writeValueAsString(data).getBytes()
  }

  override def close(): Unit = {}
}
/**
  * A deserializer for PriorityQueueTaskState
  */
class PriorityQueueTaskStateDeserializer extends Deserializer[PriorityQueueTaskState] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]) : PriorityQueueTaskState = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    if (data != null) {
      mapper.readValue(data, classOf[PriorityQueueTaskState])
    } else {
      null
    }
  }
}