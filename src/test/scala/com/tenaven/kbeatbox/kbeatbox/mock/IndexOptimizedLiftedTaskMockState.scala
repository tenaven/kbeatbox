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

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, AckEventCondition, Condition, Fact, HeartBeat, HeartBeatCondition}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{IndexOptimizedLiftedTaskStateInterface, PriorityQueueTaskState}

import scala.collection.immutable.TreeMap

/**
  * This mock test allows testing of the IndexOptimizedLiftedTaskStateFiniteStateMachine
  */
class IndexOptimizedLiftedTaskMockState(val ackIndex :  Map[AckEventCondition, Set[String]] = Map.empty,
                                        val hbIndex: TreeMap[HeartBeatCondition, Set[String]] = TreeMap.empty,
                                        override val taskStates : Map[String, PriorityQueueTaskState])
            extends LiftedTaskMockState(taskStates)
            with IndexOptimizedLiftedTaskStateInterface {

  override def getTaskNamesForFactFromIndex(fact : Fact): Set[String] = {
    val sourceState = this
    fact match {
      case HeartBeat(fromTime) => {
        val cond = HeartBeatCondition(fromTime)
        sourceState.hbIndex.to(cond).flatMap(_._2).toSet
      }
      case AckEvent(eventId) => {
        val cond = AckEventCondition(eventId)
        Set(eventId) ++ sourceState.ackIndex.getOrElse(cond, Set.empty)  // Add the eventId to the Set is important to allow for FinishedTask trace to appear.
      }
    }
  }

  def applyFunctionOnTaskConditionsInIndex(taskName: String, condition: Condition, function: (Set[String], String) => Set[String]): this.type = {
    val sourceState = this
    condition match {
      case cond: HeartBeatCondition => {
        val taskNameList = function.apply(sourceState.hbIndex.getOrElse(cond, Set.empty), taskName)
        val newIndex = if (taskNameList.isEmpty) {
          sourceState.hbIndex - cond
        } else {
          sourceState.hbIndex.updated(cond, taskNameList)
        }
        updateHbIndex(sourceState.asInstanceOf[this.type], newIndex)
      }
      case cond: AckEventCondition => {
        val taskNameList = function.apply(sourceState.ackIndex.getOrElse(cond, Set.empty), taskName)
        val newIndex: Map[AckEventCondition, Set[String]] = if (taskNameList.isEmpty){
          sourceState.ackIndex - cond
        } else {
          sourceState.ackIndex.updated(cond, taskNameList)
        }
        updateAckIndex(sourceState.asInstanceOf[this.type], newIndex)
      }
    }
  }

  def updateHbIndex(sourceState : this.type, hbIndex : TreeMap[HeartBeatCondition, Set[String]]) : this.type = {
    new IndexOptimizedLiftedTaskMockState(ackIndex = sourceState.ackIndex,
      hbIndex = hbIndex,
      taskStates = sourceState.taskStates).asInstanceOf[this.type]
  }

  def updateAckIndex(sourceState : this.type, ackIndex : Map[AckEventCondition, Set[String]]) : this.type = {
    new IndexOptimizedLiftedTaskMockState(ackIndex = ackIndex,
      hbIndex = sourceState.hbIndex,
      taskStates = sourceState.taskStates).asInstanceOf[this.type]
  }

  override def updateStateWithTasks(sourceState: this.type, updatedTasks: Map[String, PriorityQueueTaskState]): this.type = {
    new IndexOptimizedLiftedTaskMockState(ackIndex = sourceState.ackIndex,
      hbIndex = sourceState.hbIndex,
      taskStates = updatedTasks).asInstanceOf[this.type]
  }

  override def hashCode(): Int = {
    this.ackIndex.hashCode() ^ this.hbIndex.hashCode() ^ super.hashCode()
  }

  override def equals(obj: scala.Any): Boolean = super.equals(obj)
}
