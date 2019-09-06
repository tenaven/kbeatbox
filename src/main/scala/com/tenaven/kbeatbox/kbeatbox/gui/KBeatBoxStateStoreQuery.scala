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

import akka.actor.ActorSystem
import com.lightbend.kafka.scala.iq.services.LocalStateStoreQuery
import org.apache.kafka.streams.KafkaStreams

import scala.concurrent.{ExecutionContext, Future}
/**
  * defines a future dummy state store query
  * */
class KBeatBoxStateStoreQuery extends LocalStateStoreQuery[String, Long] {

  def queryStateStore(streams: KafkaStreams, store: String, value: Long)
                     (implicit ex: ExecutionContext,  as: ActorSystem): Future[Long] = {

    Future {
      0
    }
  }
}
