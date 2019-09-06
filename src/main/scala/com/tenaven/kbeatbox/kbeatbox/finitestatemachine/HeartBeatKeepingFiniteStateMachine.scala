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
package com.tenaven.kbeatbox.kbeatbox.finitestatemachine

import com.tenaven.kbeatbox.kbeatbox.api.{Fact, HeartBeat, ScheduleTask, Trace}
import com.tenaven.kbeatbox.kbeatbox.stateapi.HeartBeatKeepingStateInterface


/**
  * This FiniteStateMachine is specifically responsible of keeping the most recent heartBeat.
  * It builds on top of the IndexOptimizedLiftedTaskStateFiniteStateMachine
  */
abstract class HeartBeatKeepingFiniteStateMachine extends IndexOptimizedLiftedTaskStateFiniteStateMachine[HeartBeatKeepingStateInterface] {


  override def acceptFact(sourceState: HeartBeatKeepingStateInterface, fact: Fact) : (HeartBeatKeepingStateInterface, List[Trace]) = {

    val newState = (fact, obtainLastHeartBeatFromState(sourceState)) match {
      case (HeartBeat(currentTime), Some(lastReceivedHeartBeat)) if currentTime > lastReceivedHeartBeat => {
        updateStateWithLastReceivedHeartBeat(sourceState, currentTime)
      }
      case (HeartBeat(currentTime), None) => {
        updateStateWithLastReceivedHeartBeat(sourceState, currentTime)
      }
      case _ => sourceState
    }

    val underlyingResult = super.acceptFact(newState, fact)
    (
      underlyingResult._1,
      underlyingResult._2.map((trace: Trace) => trace.update(execTime = obtainLastHeartBeatFromState(newState)))
    )
  }

  def updateStateWithLastReceivedHeartBeat(sourceState: HeartBeatKeepingStateInterface, currentTime: Long): HeartBeatKeepingStateInterface = {
    sourceState.update(currentTime)
  }

  def obtainLastHeartBeatFromState(sourceState: HeartBeatKeepingStateInterface): Option[Long] = {
    sourceState.getLastReceivedHeartBeatTime()
  }

  override def updateTask(sourceState: HeartBeatKeepingStateInterface, task: ScheduleTask): (HeartBeatKeepingStateInterface, List[Trace]) = {
    val underlyingResult = super.updateTask(sourceState, task)
    (
      underlyingResult._1,
      underlyingResult._2.map((trace: Trace) => trace.update(execTime = obtainLastHeartBeatFromState(sourceState)))
    )
  }
}
