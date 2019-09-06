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

import net.deeppay.kmicrostreams.kbeatbox.api._
import net.deeppay.kmicrostreams.kbeatbox.stateapi._


/**
  * this is the most simple Finite State Machine declared in kbeatbox.
  * It only accepts one task with its condition. If any more task arrives,
  * it erases the one stored previously.
  */
class SingleTaskStateFiniteStateMachine extends MealyFiniteStateMachine[SingleTaskState, InputEvent, Trace] {
  def zero() = SingleTaskState("", Set.empty[Condition], Set.empty[Condition], isTriggered = false)

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
