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






