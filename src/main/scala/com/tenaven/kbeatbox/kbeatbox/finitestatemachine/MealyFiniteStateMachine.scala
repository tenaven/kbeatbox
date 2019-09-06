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

import com.tenaven.kbeatbox.kbeatbox.api.{Fact, ScheduleTask}

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
