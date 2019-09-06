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
