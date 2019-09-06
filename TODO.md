# TODO

## kbeatbox

 * [ ] create an interface project
 * [ ] Revoir l'utilisation de CoreEvent au sein du moteur (reflechir avec Yutao)
 * [ ] Test of performance(volume test, processing time, latency, scalability, response time, throughput ).
 * [ ] idempotence of heartbeats ( What happens if heartbeat is idempotent ?)
 * [ ] Taskrunner Ack Timeout component
 * [ ] Gui – be able to display states, current events, past events, ackevent, schedule, and reschedule. ( https://developer.lightbend.com/blog/2018-01-05-kafka-streams-scala-goodies-part-2/index.html - I am unsure that the interactive lib from lightbend is still current : 
it looks that people at apache kafka are currently integrating directly the code inside the kafka-streams code for interactive queries :
https://github.com/confluentinc/kafka-streams-examples/tree/5.0.0-post/src/main/java/io/confluent/examples/streams/interactivequeries
It might be the reason for our problem. Also lightbend streams query lib is not current in version 0.1.0 ... Could move to 0.1.1)
 * [x] In method [...].kbeatbox.kafkaintegration.HeartBeatKeepingKafkaState.updateAListOfTasks, the "case None =>  [...] taskStates.delete(taskName)" is never tested. Arrange so [...].kbeatbox.kafkaintegration.KbeatboxKafkaIntegrationSpec tests that case.
 * [x] TODO Remove a task from the task list on a remove task. (stack pop on priorityQueue ?)
 * [x] the message s"[$task] rejected as it contains multiple heartbeats" is never tested.
 * [x] TODO How can this happen ? => Check when this happens and whether the code here is ok. Add a comment to explain when this happens.
 * [x] TODO Update index to remove condition if task was triggered (in IndexOptimizedLiftedTaskStateFiniteStateMachine)
 * [x] Add some more testing on corner cases. (Dig in the old tests before the isolation and lifting)
 * [x] Do code coverage to check whether tests are testing enough.( use coverage to discover new interesting corner cases. No point in having 100% code coverage)
 * [x] Distributed heartbeat
 * [x] move the State case class that can be moved away into src/test/scala source directory as it is not intended to be production code. ( PB : def zero() call them in the FSM )
 * [x] salience with version.
 * [x] IndexOptimizedLiftedTaskStateInterface.updateAListOfTasks et LiftedTaskStateInterface.updateMapWithTask doivent être factorisés
 * [x] Rename initialState as sourceState in all `transition(initialState: State, event: Event)` methods (to remove confusion with the initial state of a FSM)
 * [x] Add an armoring layer to raise warning when the input is not smooth.
 * [x] Test injection
 * [x] Add an integration test with an Exception that shows a consistency issue. 
 * [x] Decouple concerns in the Finite State Machines (SingleTask, Lifted, Indexed, HeartBeat ...)
 * [x] Actual deployment on kafkastreams
 * [x] Heartbeat generator (simple then distributed. Search on internet previous work )
 * [x] Make serialisation of State working including underlying objects
 * [x] Integration of the current business logic in a kafka streams topology (including a persistent KeyValue RockDB store for the state)
 * [x] Prepare a documentation to roll out for customer

TESTS

* [x] removes one task that was previously scheduled
* [x] be able to update an event
* [x] does not run a scheduled task before it received a heartbeat
* [x] run a scheduled task even when a heartbeat is skipped
* [x] be able to reschedule an event even when it is started
* [ ] TODO run ScheduleOrRemoveTaskEvent in a idempotent way"
* [ ] TODO run HeartBeatEvent in a idempotent way
* [x] be able to receive Ack events
* [x] TODO be able to receive Ack events, and chain with an other event
* [X] TODO be able to receive Ack events, and chain multiple AckEvents with an other event
* [x] Raising a warning when a heartbeat comes from the past
* [ ] TODO Can reschedule a task that was acknowledged
* [x] Cannot reschedule a task to an earlier time
* [x] Cannot reschedule back in time
