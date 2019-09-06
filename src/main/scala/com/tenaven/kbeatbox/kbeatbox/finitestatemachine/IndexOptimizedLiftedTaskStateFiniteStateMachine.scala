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

import com.tenaven.kbeatbox.kbeatbox.api.{Condition, ScheduleTask, Trace}
import com.tenaven.kbeatbox.kbeatbox.stateapi.{IndexOptimizedLiftedTaskStateInterface, PriorityQueueTaskState}
import com.typesafe.scalalogging.Logger


/**
  * This is an index optimized FiniteStateMachine that improves in term of efficiency the
  * LiftedTaskStateFiniteStateMachine while mimicking it.
  */
abstract class IndexOptimizedLiftedTaskStateFiniteStateMachine[T <: IndexOptimizedLiftedTaskStateInterface]
  extends LiftedTaskStateFiniteStateMachine[T] {

  val logger = Logger(classOf[IndexOptimizedLiftedTaskStateFiniteStateMachine[_]])

  override def updateIndexForResultingTasks(sourceState: T, updatedTasksAndTraces: List[(String, PriorityQueueTaskState, List[Trace], Boolean)]) : T = {
    val triggeredOrFinishedTaskToClearFromIndex =
      updatedTasksAndTraces.filter({
        case (_, priorityQueueTaskState, _, isFinished) => priorityQueueTaskState.isTriggered || isFinished
      })

    triggeredOrFinishedTaskToClearFromIndex.foldLeft(sourceState)(
      (fromState: T, tuple: (String, PriorityQueueTaskState, _, _)) => {
        val (taskName, priorityQueueTaskState, _, _) = tuple
        removeOldConditionsFromIndex(fromState, taskName, Some(priorityQueueTaskState))
      }
    )
  }


  override def updateIndexOnTaskIfNecessary(sourceState: T, task: ScheduleTask, maybeInitialPriorityQueueState: Option[PriorityQueueTaskState]) : T = {
    if (isToUpdateIndexBasedOnPriorities(task, maybeInitialPriorityQueueState)) {
      updateIndexOnTask(sourceState, task, maybeInitialPriorityQueueState)
    } else {
      sourceState
    }
  }

  private def isToUpdateIndexBasedOnPriorities(task: ScheduleTask, maybeInitialPriorityQueueState: Option[PriorityQueueTaskState]) = {
    maybeInitialPriorityQueueState.flatMap {
      _.priorityQueue.headOption.map(_._1 <= task.priority)
    }.getOrElse(true)
  }

  private def updateIndexOnTask(sourceState: T, task: ScheduleTask, maybeOldSingleTaskState: Option[PriorityQueueTaskState]) = {
    logger.debug("start updating index")
    val indexWithRemovedOldConditions = removeOldConditionsFromIndex(sourceState, task.taskId, maybeOldSingleTaskState)

    val resultingConditionToAdd: Set[Condition] = evaluateConditionsToAddInIndex(task, maybeOldSingleTaskState)
    logger.debug(s"resulting condition to add: [$resultingConditionToAdd]")

    val result = addNewConditionsToIndex(task.taskId, indexWithRemovedOldConditions, resultingConditionToAdd)
    logger.debug("finished updating index")
    result
  }

  private def addNewConditionsToIndex(taskId: String, indexWithRemovedOldConditions: T, resultingConditionToAdd: Set[Condition]): T = {
    resultingConditionToAdd.foldLeft(indexWithRemovedOldConditions)(
      (currentState, condition) => updateIndexWithNewTaskCondition(currentState, taskId, condition)
    )
  }

  private def evaluateConditionsToAddInIndex(task: ScheduleTask, maybePriorityQueueTaskState: Option[PriorityQueueTaskState]): Set[Condition] = {
    if (task.conditions.isEmpty) {
      evaluateConditionsToAddInIndexWhenHighestPriorityIsRemoved(maybePriorityQueueTaskState)
    } else {
      task.conditions
    }
  }

  private def evaluateConditionsToAddInIndexWhenHighestPriorityIsRemoved(maybePriorityQueueTaskState: Option[PriorityQueueTaskState]) = {
    maybePriorityQueueTaskState.flatMap { currentPriorityQueueState => {
      val priorityQueue = currentPriorityQueueState.priorityQueue

      priorityQueue.lastOption.flatMap {
        last => {
          (priorityQueue - (last._1)).values.lastOption
        }
      }
    }
    }.getOrElse(Set.empty[Condition])
  }

  private def removeOldConditionsFromIndex(sourceState: T, taskId: String, maybeOldState: Option[PriorityQueueTaskState]): T = {
    val oldConditionsToRemoveFromIndex: Set[Condition] = maybeOldState.flatMap { priorityQueueTaskState => {
        priorityQueueTaskState.priorityQueue.lastOption.map(_._2)
      }
    }.getOrElse(Set.empty[Condition])

    oldConditionsToRemoveFromIndex.foldLeft(sourceState)(
      (currentState, condition) => removeTaskIdFromIndexAtCondition(currentState, taskId, condition)
    )
  }

  def removeTaskIdFromIndexAtCondition(currentIndex: T, taskName: String, condition: Condition) : T = {
    currentIndex.applyFunctionOnTaskConditionsInIndex(taskName, condition, _ - _)
  }

  def updateIndexWithNewTaskCondition(currentIndex : T, taskName: String, condition : Condition) : T = {
    currentIndex.applyFunctionOnTaskConditionsInIndex(taskName, condition, _ + _)
  }
}
