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
package net.deeppay.kmicrostreams.utils

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
