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
package net.deeppay.kmicrostreams.kbeatbox.api

import java.util

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

/**
  * The base class for all events that come into the scheduler.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[ScheduleTask], name = "ScheduleTask"),
  new Type(value = classOf[HeartBeat], name = "HeartBeat"),
  new Type(value = classOf[AckEvent], name = "AckEvent")
))
sealed abstract class InputEvent

/**
  * the base class for Facts (which can be either a heartbeat or AckEvent. A ScheduleTask Event is not
  * a Fact.
  */
sealed abstract class Fact extends InputEvent

case class HeartBeat(@JsonProperty("currentTime") currentTime :Long) extends Fact
case class AckEvent(@JsonProperty("taskId") taskId : String) extends Fact

/**
  * The root class for conditions
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[HeartBeatCondition], name = "HeartBeatCondition"),
  new Type(value = classOf[AckEventCondition], name = "AckEventCondition")
))
sealed abstract class Condition {
  def matches(fact : Fact) : Boolean
}

/**
  * a condition that triggers the task when a heartbeat arrives with a currentTime later than fromTime parameter.
  * HeartBeatCondition is ordered along the fromTime parameter.
  * @param fromTime
  */
case class HeartBeatCondition(@JsonProperty("fromTime") fromTime : Long) extends Condition with Ordered[HeartBeatCondition] {
  def matches(fact: Fact): Boolean = fact match {
    case HeartBeat(currentTime) if currentTime >= fromTime => true
    case _ => false
  }
  override def hashCode(): Int = fromTime.hashCode()

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case HeartBeatCondition(oFromTime) => this.fromTime.equals(oFromTime)
      case _ => super.equals(obj)
    }
  }

  override def compare(that: HeartBeatCondition): Int = {
    this.fromTime.compareTo(that.fromTime)
  }
}

/**
  * A condition that's triggered when a AckEvent arrives.
  * @param matchId the name of the AckEvent to be matched
  */
case class AckEventCondition(@JsonProperty("matchId") matchId : String) extends Condition {
  override def matches(fact: Fact): Boolean = fact match {
    case AckEvent(eventId) if eventId.equals(matchId) => true
    case _ => false
  }
  override def hashCode(): Int = matchId.hashCode()
  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case AckEventCondition(oMatchId) => this.matchId.equals(oMatchId)
      case _ => super.equals(obj)
    }
  }
}
/**
  * Commands the scheduling of a task in the future.
  * @param taskId the unique Id of the task
  * @param conditions the condition that are to be met for the task to be started.
  */
case class ScheduleTask(@JsonProperty("taskId") taskId : String,
                        @JsonProperty("conditions") conditions : Set[Condition],
                        @JsonProperty("priority") priority : Int = 0) extends InputEvent

/**
  * A serializer for Event
  */
class InputEventSerializer extends Serializer[InputEvent] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(topic: String, data: InputEvent) : Array[Byte]= {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.canSerialize(classOf[InputEvent])
    mapper.writeValueAsString(data).getBytes()
  }

  override def close(): Unit = {}
}
/**
  * A deserializer for Event
  */
class InputEventDeserializer extends Deserializer[InputEvent] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]) : InputEvent = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    if (data != null) {
      mapper.readValue(data, classOf[InputEvent])
    } else {
      null
    }
  }
}