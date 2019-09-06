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
