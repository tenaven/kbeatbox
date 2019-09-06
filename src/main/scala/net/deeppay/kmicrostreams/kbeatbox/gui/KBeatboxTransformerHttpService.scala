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
package net.deeppay.kmicrostreams.kbeatbox.gui

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





