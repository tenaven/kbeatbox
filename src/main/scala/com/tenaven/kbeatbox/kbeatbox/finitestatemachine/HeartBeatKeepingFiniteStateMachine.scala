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
