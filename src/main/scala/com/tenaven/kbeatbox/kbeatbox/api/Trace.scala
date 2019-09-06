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
package com.tenaven.kbeatbox.kbeatbox.api

import java.util

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[StartedTask], name = "StartedTask"),
  new Type(value = classOf[FinishedTask], name = "FinishedTask"),
  new Type(value = classOf[RejectedHeartBeat], name = "RejectedHeartBeat"),
  new Type(value = classOf[RejectedTask], name = "RejectedTask")
))
abstract sealed class Trace {
  def update(execTime : Option[Long]) : Trace
}

case class StartedTask(@JsonProperty("taskId") taskId : String,
                       @JsonProperty("execTime") execTime : Option[Long] = None)
  extends Trace {
  override def update(execTime: Option[Long]): Trace = this.copy(execTime = execTime)
}

case class FinishedTask(@JsonProperty("taskId") taskId : String,
                        @JsonProperty("execTime") execTime : Option[Long] = None)
  extends Trace {
  override def update(execTime: Option[Long]): Trace = this.copy(execTime = execTime)
}

case class RejectedHeartBeat(@JsonProperty("warning") warningMessage : String,
                             @JsonProperty("execTime") execTime : Option[Long] = None)
  extends Trace {
  override def update(execTime: Option[Long]): Trace = this.copy(execTime = execTime)
}

case class RejectedTask(@JsonProperty("warningMessage") warningMessage : String,
                        @JsonProperty("taskId") taskId : String,
                        @JsonProperty("execTime") execTime : Option[Long] = None)
  extends Trace {
  override def update(execTime: Option[Long]): Trace  = this.copy(execTime = execTime)
}

case class RejectedAckEvent(@JsonProperty("warningMessage") warningMessage : String,
                            @JsonProperty("taskId") taskId : String,
                            @JsonProperty("execTime") execTime : Option[Long] = None)
  extends Trace {
  override def update(execTime: Option[Long]): Trace = this.copy(execTime = execTime)
}



/**
  * A serializer for Trace
  */
class TraceSerializer extends Serializer[Trace] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def serialize(topic: String, data: Trace) : Array[Byte]= {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.canSerialize(classOf[Trace])
    mapper.writeValueAsString(data).getBytes()
  }

  override def close(): Unit = {}
}
/**
  * A deserializer for Trace
  */
class TraceDeserializer extends Deserializer[Trace] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {}

  override def close(): Unit = {}

  override def deserialize(topic: String, data: Array[Byte]) : Trace = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    if (data != null) {
      mapper.readValue(data, classOf[Trace])
    } else {
      null
    }
  }
}