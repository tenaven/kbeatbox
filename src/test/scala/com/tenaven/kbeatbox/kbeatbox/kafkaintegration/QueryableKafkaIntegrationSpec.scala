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
package com.tenaven.kbeatbox.kbeatbox.kafkaintegration

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, AckEventCondition, HeartBeat, HeartBeatCondition, InputEvent, ScheduleTask}
import com.tenaven.kbeatbox.kbeatbox.gui.InitiateRestProxy
import com.typesafe.scalalogging.Logger
import net.manub.embeddedkafka.EmbeddedKafkaConfig
import org.scalatest.{Matchers, WordSpec}
import net.manub.embeddedkafka.Codecs.stringSerializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.state.HostInfo
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.state.QueryableStoreTypes

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class QueryableKafkaIntegrationSpec extends WordSpec with Matchers with BaseSpec {

  val logger: Logger = Logger(classOf[QueryableKafkaIntegrationSpec])
  implicit val config: EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(kafkaPort = 7333, zooKeeperPort = 7001)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: concurrent.ExecutionContextExecutor = system.dispatcher

  val (inTopic, outTopic) = ("in", "out")

  "streams topology" should ("accept message, send back message, and be queryable" in {

    val topologyBuilder: KBeatboxTopologyBuilder = new KBeatboxTopologyBuilder()

    val extraConfig: _root_.scala.collection.immutable.Map[_root_.java.lang.String, _root_.java.lang.String] = Map(
      StreamsConfig.APPLICATION_SERVER_CONFIG -> s"localhost:8080",
      StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG -> Serdes.String.getClass.getName,
      StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG -> Serdes.String.getClass.getName
    )

    createStreams(Seq(inTopic, outTopic), topologyBuilder.buildTopology(inTopic, outTopic), extraConfig = extraConfig)(block = (streams: KafkaStreams) => {

      val eventList: Stream[InputEvent] = Stream(ScheduleTask("A", Set(HeartBeatCondition(1L))),
        ScheduleTask("B", Set(HeartBeatCondition(2L))),
        ScheduleTask("C", Set(AckEventCondition("A"), AckEventCondition("B"))),
        HeartBeat(1L),
        HeartBeat(2L),
        AckEvent("A"),
        AckEvent("B"),
        AckEvent("C"),
        ScheduleTask("A", Set(HeartBeatCondition(3L))),
        ScheduleTask("A", Set(HeartBeatCondition(4L))),
        ScheduleTask("A", Set())
      )

      eventList.foreach(
        (event: InputEvent) => publishToKafka[String, InputEvent](inTopic, "a",
          event)(config, stringSerializer,
          topologyBuilder.inputValueSerdes.serializer())
      )

      Thread.sleep(2000)

      val restEndpointPort: Int = 8080
      val restEndpointHostName: String = "localhost"
      val restEndpoint: HostInfo = new HostInfo(restEndpointHostName, restEndpointPort)

      val initiateRestProxy: InitiateRestProxy = new InitiateRestProxy
      initiateRestProxy.defaultExecutionContext(2)
      initiateRestProxy.startRestProxy(streams, restEndpoint, system, materializer)

      val metadataService: MetadataService = new MetadataService(streams)

      logger.info(s"metadatas: ${metadataService.streamsMetadataForStore(KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore)}. ")

      val streamsMetadata: com.lightbend.kafka.scala.iq.services.HostStoreInfo = metadataService.streamsMetadataForStoreAndKey(KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore, KBeatboxKafkaStateStoreConstants.lastReceivedHeartBeatKeyConstant, Serdes.String().serializer())
      logger.info(s"streamsMetadata : host: ${streamsMetadata.host}, port: ${streamsMetadata.port}, store: ${streamsMetadata.storeNames}")

      val path: String = "/weblog/access/check/localhost"
      val eventualResponse: _root_.scala.concurrent.Future[_root_.akka.http.scaladsl.model.HttpResponse] = Http().singleRequest(HttpRequest(uri = s"http://${restEndpointHostName}:${restEndpointPort}$path"))
      val response: HttpResponse = Await.result(eventualResponse, 2000 millis)

      logger.info("http response : " + response.toString())
      val contentTypeResponse: ContentType = response.entity.contentType
      logger.info("Content type response :" + contentTypeResponse)


      val expectedLastHeartBeatReceived: _root_.scala.concurrent.Future[_root_.scala.Predef.String] = Unmarshal(response.entity).to[String]
      logger.info("Content type response :" + expectedLastHeartBeatReceived)
      val expected: String = "FulfilledFuture(2)"
      expectedLastHeartBeatReceived.toString() shouldBe expected

      QueryStore(KBeatboxKafkaStateStoreConstants.KBeatboxTaskStore)

      def QueryStore (store: String){
      val keyValueStore = streams.store(store, QueryableStoreTypes.keyValueStore)
      val range = keyValueStore.all
      range.hasNext
      while (range.hasNext ) {
        val next = range.next
        logger.info(String.format("key: %s | value: %s", next.key, next.value))
        }
      }

    })
  })
}
