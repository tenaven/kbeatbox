/*Copyright 2019 TENAVEN

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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