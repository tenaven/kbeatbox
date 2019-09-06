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
package com.tenaven.kbeatbox.kbeatbox.gui

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.lightbend.kafka.scala.iq.http.{HttpRequester, InteractiveQueryHttpService, KeyValueFetcher}
import com.lightbend.kafka.scala.iq.services.{LocalStateStoreQuery, MetadataService}
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.HostInfo

import scala.concurrent.{ExecutionContext, Future}
/**
  * initiates the rest proxy, metadataService, stateStore, and state query
  * */
class InitiateRestProxy {

  lazy val defaultParallelism: Int = {
    val rt = Runtime.getRuntime()
    rt.availableProcessors() * 4
  }
  def defaultExecutionContext(parallelism: Int = defaultParallelism): ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(parallelism))

  def startRestProxy(streams: KafkaStreams, hostInfo: HostInfo,
      actorSystem: ActorSystem, materializer: ActorMaterializer): InteractiveQueryHttpService = {

    implicit val system = actorSystem
    implicit  val keySerializer = Serdes.String().serializer()

    val executionContext = defaultExecutionContext()

    // service for fetching metadata information
    val metadataService = new MetadataService(streams)

    // service for fetching from local state store
    val localStateStoreQuery = new KBeatBoxStateStoreQuery

    // http service for request handling
    val httpRequester = new HttpRequester(system, materializer, executionContext)

    val restService = new KBeatboxTransformerHttpService(
      hostInfo,
      new KBeatBoxStateFetcher(new KeyValueFetcher(metadataService,
                                                   localStateStoreQuery,
                                                   httpRequester,
                                                   streams, executionContext, hostInfo)),
      system, materializer, executionContext
    )
    restService.start()
    restService
  }
}






