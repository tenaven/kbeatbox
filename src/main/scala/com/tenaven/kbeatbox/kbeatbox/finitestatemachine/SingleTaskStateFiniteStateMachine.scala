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

import com.tenaven.kbeatbox.kbeatbox.api.{AckEvent, Condition, Fact, FinishedTask, InputEvent, ScheduleTask, StartedTask, Trace}
import com.tenaven.kbeatbox.kbeatbox.stateapi
import com.tenaven.kbeatbox.kbeatbox.stateapi.SingleTaskState


/**
  * this is the most simple Finite State Machine declared in kbeatbox.
  * It only accepts one task with its condition. If any more task arrives,
  * it erases the one stored previously.
  */
class SingleTaskStateFiniteStateMachine extends MealyFiniteStateMachine[SingleTaskState, InputEvent, Trace] {
  def zero() = stateapi.SingleTaskState("", Set.empty[Condition], Set.empty[Condition], isTriggered = false)

  override def acceptFact(sourceState: SingleTaskState, fact: Fact): (SingleTaskState, List[Trace]) = {
    fact match {
      case AckEvent(ackName) if ackName.equals(sourceState.taskName) && sourceState.isTriggered => {
        (
          zero(),
          List(FinishedTask(sourceState.taskName))
        )
      }
      case _ => {
        val matchedConditions = sourceState.taskConditions.filter(condition => condition.matches(fact))
        val finalState = sourceState.copy(matchedConditions = sourceState.matchedConditions ++ matchedConditions)
        if (isReadyForTrigger(finalState)) {
          (
            finalState.copy(isTriggered = true),
            List(StartedTask(sourceState.taskName))
          )
        } else {
          (
            finalState,
            List.empty[Trace]
          )
        }
      }
    }

  }

  private def isReadyForTrigger(destinationState: SingleTaskState): Boolean = {
    destinationState.taskConditions.subsetOf(destinationState.matchedConditions) &&
      !destinationState.isTriggered &&
      destinationState.taskConditions.nonEmpty
  }

  override def updateTask(sourceState: SingleTaskState, task: ScheduleTask): (SingleTaskState, List[Trace]) = {
    if (task.conditions.isEmpty) { // An empty set of conditions means removing the task from scheduling.
      (
        zero(),
        List.empty[Trace]
      )
    } else {
      (
        sourceState.copy(taskName = task.taskId,
          taskConditions = task.conditions,
          matchedConditions = Set.empty[Condition]),
        List.empty[Trace]
      )
    }
  }
}
