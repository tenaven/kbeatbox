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

import com.lightbend.kafka.scala.iq.http.KeyValueFetcher
import com.tenaven.kbeatbox.kbeatbox.kafkaintegration.KBeatboxKafkaStateStoreConstants

import scala.concurrent.Future

/**
  * defines states to be fetched: lastHeartbeatReceived,
  * taskStore, hbIndex, and ackIndex
  * */

class KBeatBoxStateFetcher(fetcher: KeyValueFetcher) {

  def getLastReceivedHeartBeat(): Future[Long] = {
    fetcher.fetch(KBeatboxKafkaStateStoreConstants.lastReceivedHeartBeatKeyConstant, KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore, "/weblog/access/check/localhost" + KBeatboxKafkaStateStoreConstants.lastReceivedHeartBeatKeyConstant)
  }

  def getTaskStore(): Future[Long] = {
    fetcher.fetch(KBeatboxKafkaStateStoreConstants.KBeatboxTaskStore, KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore, "/weblog/access/check/localhost" + KBeatboxKafkaStateStoreConstants.KBeatboxTaskStore)
  }

  def getHeartbeatIndex(): Future[Long] = {
    fetcher.fetch(KBeatboxKafkaStateStoreConstants.KBeatboxhbIndex, KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore, "/weblog/access/check/localhost" + KBeatboxKafkaStateStoreConstants.KBeatboxhbIndex)
  }

  def getAckIndex(): Future[Long] = {
    fetcher.fetch(KBeatboxKafkaStateStoreConstants.KBeatboxAckEventIndex, KBeatboxKafkaStateStoreConstants.KBeatboxHeartBeatStore, "/weblog/access/check/localhost" + KBeatboxKafkaStateStoreConstants.KBeatboxAckEventIndex)
  }
}
