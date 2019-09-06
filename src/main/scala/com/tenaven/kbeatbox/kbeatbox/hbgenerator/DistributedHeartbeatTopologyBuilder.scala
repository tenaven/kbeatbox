package com.tenaven.kbeatbox.kbeatbox.hbgenerator

import com.lightbend.kafka.scala.streams.{KStreamS, StreamsBuilderS}
import com.tenaven.kbeatbox.kbeatbox.api.{InputEvent, InputEventDeserializer, InputEventSerializer}
import com.tenaven.kbeatbox.utils.GenericTopologyBuilder
import org.apache.kafka.common.serialization.{Serde, Serdes}

class DistributedHeartbeatTopologyBuilder extends GenericTopologyBuilder[String, InputEvent] {
  override def inputValueSerdes: Serde[String]= Serdes.String()
   def transformDistributedHeartbeat(inputStream: KStreamS[String, String]): KStreamS[String, InputEvent] = {
     inputStream transform (
       () => new DistributedHeartbeatTransformer(DBGeneratorConfiguration(interval = 1000))
       )
   }
     override def completeExec: KStreamS[String, String] => KStreamS[String, InputEvent] = transformDistributedHeartbeat(_)
     override def outputValueSerdes: Serde[InputEvent] = Serdes.serdeFrom[InputEvent](new InputEventSerializer(), new InputEventDeserializer )
     override def configureStreamsBuilder(streamBuilder: StreamsBuilderS): Unit = {

     }
}
