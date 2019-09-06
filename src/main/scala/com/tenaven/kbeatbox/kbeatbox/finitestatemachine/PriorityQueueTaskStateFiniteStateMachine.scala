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
