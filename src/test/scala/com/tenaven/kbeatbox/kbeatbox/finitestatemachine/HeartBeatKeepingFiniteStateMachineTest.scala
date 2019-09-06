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

import com.tenaven.kbeatbox.kbeatbox.api.{HeartBeat, HeartBeatCondition, InputEvent, ScheduleTask, StartedTask}
import com.tenaven.kbeatbox.kbeatbox.mock.MockConcreteHeartBeatKeepingFiniteStateMachine
import org.junit.Test




class HeartBeatKeepingFiniteStateMachineTest extends RunFiniteStateMachineTestBase {
  override val finiteStateMachines = List(new MockConcreteHeartBeatKeepingFiniteStateMachine())

  @Test def verifyLiftTasks(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("myname", Set(HeartBeatCondition(1L))), HeartBeat(1L))

    val expectedTraces = Stream(List(), List(), List(StartedTask("myname", Some(1L))))

    runTestAndAssertTraces(s, expectedTraces, currentTimeRemoved = false)
  }

  @Test def verifyRemoveTaskNameWhenTaskIsEmpty(): Unit = {

    val s: Stream[InputEvent] = Stream(ScheduleTask("myname", Set()))

    val expectedTraces = Stream(List(), List())

    runTestAndAssertTraces(s, expectedTraces, currentTimeRemoved = false)
  }
}


