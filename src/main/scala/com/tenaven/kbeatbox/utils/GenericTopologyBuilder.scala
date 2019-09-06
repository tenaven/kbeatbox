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
package com.tenaven.kbeatbox.utils

import com.lightbend.kafka.scala.streams.{KStreamS, StreamsBuilderS}
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.{Consumed, Topology}

abstract class GenericTopologyBuilder[InputValue, OutputValue] {

  val stringSerdes: Serde[String] = Serdes.String()

  def inputValueSerdes: Serde[InputValue]
  def outputValueSerdes: Serde[OutputValue]

  def completeExec: KStreamS[String, InputValue] => KStreamS[String, OutputValue] = ???

  def configureStreamsBuilder(streamBuilder: StreamsBuilderS)

  /**
    * Build a position topology
    * @param inTopic
    * @param outTopic
    * @return
    */
  def buildTopology(inTopic : String, outTopic : String) : Topology = {
    val streamBuilder = new StreamsBuilderS
    configureStreamsBuilder(streamBuilder)

    val inputStream: KStreamS[String, InputValue] =
      streamBuilder.stream(inTopic, Consumed.`with`(stringSerdes, inputValueSerdes))

    val resultStream: KStreamS[String, OutputValue] = completeExec.apply(inputStream)
    resultStream.to(outTopic, Produced.`with`(stringSerdes, outputValueSerdes))
    streamBuilder.build()
  }
}
