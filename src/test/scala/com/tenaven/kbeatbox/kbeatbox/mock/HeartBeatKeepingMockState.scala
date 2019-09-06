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
package com.tenaven.kbeatbox.kbeatbox.mock

import com.tenaven.kbeatbox.kbeatbox.api.{AckEventCondition, HeartBeatCondition}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{HeartBeatKeepingStateInterface, PriorityQueueTaskState}

import scala.collection.immutable.TreeMap

/**
  * This mock state allows for testing of the HeartBeatKeepingFiniteStateMachine.
  */
class HeartBeatKeepingMockState(val lastReceivedHeartBeatTime : Option[Long],
                                override val ackIndex :  Map[AckEventCondition, Set[String]] = Map.empty,
                                override val hbIndex: TreeMap[HeartBeatCondition, Set[String]] = TreeMap.empty,
                                override val taskStates : Map[String, PriorityQueueTaskState])
              extends IndexOptimizedLiftedTaskMockState(ackIndex, hbIndex, taskStates)
              with HeartBeatKeepingStateInterface {

  def getLastReceivedHeartBeatTime(): Option[Long] = this.lastReceivedHeartBeatTime

  def update(currentTime : Long) : this.type = {
    new HeartBeatKeepingMockState(
      lastReceivedHeartBeatTime = Some(currentTime),
      ackIndex = this.ackIndex,
      hbIndex = this.hbIndex,
      taskStates = this.taskStates
    ).asInstanceOf[this.type]
  }

  override def updateHbIndex(sourceState : this.type, hbIndex : TreeMap[HeartBeatCondition, Set[String]]) : this.type = {
    new HeartBeatKeepingMockState(lastReceivedHeartBeatTime = sourceState.lastReceivedHeartBeatTime,
      ackIndex = sourceState.ackIndex,
      hbIndex = hbIndex,
      taskStates = sourceState.taskStates).asInstanceOf[this.type]
  }

  override def updateAckIndex(sourceState : this.type, ackIndex : Map[AckEventCondition, Set[String]]) : this.type = {
    new HeartBeatKeepingMockState(lastReceivedHeartBeatTime = sourceState.lastReceivedHeartBeatTime,
      ackIndex = ackIndex,
      hbIndex = sourceState.hbIndex,
      taskStates = sourceState.taskStates).asInstanceOf[this.type]
  }

  override def updateStateWithTasks(sourceState: this.type, updatedTasks: Map[String, PriorityQueueTaskState]): this.type = {
    new HeartBeatKeepingMockState(lastReceivedHeartBeatTime = sourceState.lastReceivedHeartBeatTime,
      ackIndex = sourceState.ackIndex,
      hbIndex = sourceState.hbIndex,
      taskStates = updatedTasks).asInstanceOf[this.type]
  }

  override def hashCode(): Int = lastReceivedHeartBeatTime.hashCode() ^ super.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case heartBeat : HeartBeatKeepingMockState =>
      heartBeat.lastReceivedHeartBeatTime.equals(this.lastReceivedHeartBeatTime) && super.equals(obj)

    case _ => super.equals(obj)
  }
}
