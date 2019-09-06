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
package com.tenaven.kbeatbox.kbeatbox.hbgenerator

import java.util.{Properties, UUID}

import com.tenaven.kbeatbox.kbeatbox.api.HeartBeat
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig

object BeatGenerator {

  def main(args: Array[String]): Unit = {
    val generator = new BeatGenerator(BeatGeneratorConfiguration())
    generator.run()
  }
}

case class BeatGeneratorConfiguration(restingPeriod: Long = 150,
                                      topic: String = "heartBeatEvent",
                                      bootstrapServer:String = "localhost:9092")


class BeatGenerator(val configuration : BeatGeneratorConfiguration) {

  val logger = Logger(classOf[BeatGenerator])

  var running = true

  def run(): Unit = {

    val producer: KafkaProducer[String, String] = createProducer(configuration)

    while (running) {
      val currentTime = System.currentTimeMillis()
      sendRecord(producer,
      createRecord(configuration, currentTime))
      Thread.sleep(configuration.restingPeriod)
    }
  }

  private def sendRecord(producer: KafkaProducer[String, String], record: ProducerRecord[String, String]): Unit = {
    producer.beginTransaction()

    // sending heartbeats to kafka using a producer
    val send = producer.send(record).get()

    producer.commitTransaction()

    logger.info(s"sent record(key = ${record.key()}, value = ${record.value()} "
      + s"meta(partition = ${send.partition()}, " +
      s"offset = ${send.offset()}) timestamp = ${send.timestamp()}\n")

  }
  // creates record from generated heartbeat
  private def createRecord(configurationPeriod: BeatGeneratorConfiguration, currentTime: Long) = {
    val heartBeatEvent = HeartBeat(currentTime)

    val index = UUID.randomUUID().toString
    val record = new ProducerRecord[String, String](configurationPeriod.topic, index, heartBeatEvent.toString)
    record
  }

  private def createProducer(configurationPeriod: BeatGeneratorConfiguration) = {
    val producerProps = new Properties()
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configurationPeriod.bootstrapServer)
    producerProps.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE) // Initiates exactly once property
    producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "heartBeatEvent") // this has to be set!!! (unique for each producer you're having)
    producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true") // has to be idempotent
    val producer = new KafkaProducer[String, String](producerProps, new StringSerializer, new StringSerializer)
    producer.initTransactions()
    producer
  }

}