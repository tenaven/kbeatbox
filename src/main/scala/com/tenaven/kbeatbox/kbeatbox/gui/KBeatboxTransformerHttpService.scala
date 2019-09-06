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
import akka.stream.ActorMaterializer
import com.lightbend.kafka.scala.iq.http.InteractiveQueryHttpService
import io.circe.syntax._
import org.apache.kafka.streams.state.HostInfo

import scala.concurrent.ExecutionContext


/**
  * defines path and fetches states from path
  * */

class KBeatboxTransformerHttpService(
                                      hostInfo: HostInfo,
                                      kbeatboxStateFetcher: KBeatBoxStateFetcher,
                                      actorSystem: ActorSystem,
                                      actorMaterializer: ActorMaterializer,
                                      ec: ExecutionContext
                                    ) extends InteractiveQueryHttpService(hostInfo, actorSystem, actorMaterializer, ec) {

  // define the routes
  val routes = handleExceptions(myExceptionHandler) {

    pathPrefix("weblog") {

      (get & pathPrefix("access" / "check") & path(Segment)) { _ =>

        complete {

          kbeatboxStateFetcher.getTaskStore().map(_.asJson)
          kbeatboxStateFetcher.getHeartbeatIndex().map(_.asJson)
          kbeatboxStateFetcher.getAckIndex().map(_.asJson)
          kbeatboxStateFetcher.getLastReceivedHeartBeat().map(_.asJson)

        }
      }
    }
  }
}





