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

import net.deeppay.kmicrostreams.kbeatbox.api.{Fact, ScheduleTask}
import net.deeppay.kmicrostreams.kbeatbox.api.ScheduleTask

/**
  * Abstract version of the FiniteStateMachine
  *
  *
  * This describes the entire state of the scheduler as FiniteStateMachine :
  *
  *     ScheduleTask
  * Ack    |  HeartBeat (not included)
  * /   \  |  /
  * |  | Input |      +------------+    +----------+
  * |  +-------+     /            /|<=> |stateStore|
  * |      |        +------------+ +    +----------+
  * |      \--------| KScheduler |/
  * |               +------------+
  * |                      |
  * |                      v
  * |                  | Output |
  * |                  +--------+
  * |                    | | |
  * |
  * |                    Running Tasks (Not included)
  * |                    | | |
  * |                  +--------+
  * |                      |
  * \----------------------/
  *
  */
abstract class MealyFiniteStateMachine[State, Event, T] {
  def zero() : State
  def transition(sourceState: State, event: Event) : (State, List[T]) = {
    event match {
      case task : ScheduleTask => updateTask(sourceState, task)
      case fact : Fact => acceptFact(sourceState, fact)
    }
  }
  def acceptFact(sourceState: State, fact: Fact) : (State, List[T])
  def updateTask(sourceState: State, task: ScheduleTask) : (State, List[T])
}
