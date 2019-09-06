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
package net.deeppay.kmicrostreams.kbeatbox.finitestatemachine

import com.typesafe.scalalogging.Logger
import net.deeppay.kmicrostreams.kbeatbox.api.{Condition, ScheduleTask, Trace}
import net.deeppay.kmicrostreams.kbeatbox.stateapi._


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
