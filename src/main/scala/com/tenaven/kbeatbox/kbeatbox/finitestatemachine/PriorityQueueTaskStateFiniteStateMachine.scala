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

import com.tenaven.kbeatbox.kbeatbox.api.{Condition, Fact, InputEvent, ScheduleTask, Trace}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{PriorityQueueTaskState, SingleTaskState}

import scala.collection.immutable.TreeMap

/**
  * Responsible for allowing different priorities of task overriding one another.
  */
class PriorityQueueTaskStateFiniteStateMachine extends MealyFiniteStateMachine[PriorityQueueTaskState, InputEvent, Trace] {
  val underlyingFiniteStateMachine = new SingleTaskStateFiniteStateMachine()

  override def zero(): PriorityQueueTaskState = new PriorityQueueTaskState(taskName="",
                                            TreeMap.empty, Set.empty[Condition], isTriggered = false)

  override def acceptFact(sourceState: PriorityQueueTaskState, fact: Fact): (PriorityQueueTaskState, List[Trace]) = {
    val (resultUnderlyingState, traces) = sourceState.priorityQueue.lastOption match {
      case None => {
        underlyingFiniteStateMachine.acceptFact(underlyingFiniteStateMachine.zero(), fact)
      }

      case Some((priorityKey, conditionSet)) => {
        val singleTaskState = SingleTaskState(sourceState.taskName,
                                              conditionSet,
                                              sourceState.matchedConditions,
                                              sourceState.isTriggered)
        underlyingFiniteStateMachine.acceptFact(singleTaskState, fact)
      }
    }
    (
      sourceState.copy(matchedConditions = resultUnderlyingState.matchedConditions,
              isTriggered = resultUnderlyingState.isTriggered),
      traces
    )
  }

  override def updateTask(sourceState: PriorityQueueTaskState, task: ScheduleTask): (PriorityQueueTaskState, List[Trace]) = {

    val destinationState = task match {

      case ScheduleTask(_, conditions, _) if conditions.isEmpty => {
        sourceState.copy(priorityQueue = sourceState.priorityQueue - task.priority)
      }

      case _ => {
        sourceState.copy(taskName = task.taskId,
          priorityQueue = sourceState.priorityQueue + (task.priority -> task.conditions)
        )
      }
    }

    (
      destinationState,
      List.empty[Trace]
    )
  }
}
