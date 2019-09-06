package com.tenaven.kbeatbox.kbeatbox.taskmanager

class TaskManager {

  sealed trait TaskManagerCommand

  case class AddTask(taskId: String, taskName: String) extends TaskManagerCommand
  case class RemoveTask(taskName: String) extends TaskManagerCommand

  sealed trait TaskManagerRequest

  case object ListTasks extends TaskManagerRequest
  case object ListTasksById extends TaskManagerRequest
  case class ListTasksOf(taskIds: List[String]) extends TaskManagerRequest
  case object ListTasksAvailableTasks extends TaskManagerRequest
  case class ListTasksAvailableTasksFrom(taskMap: Map[String, List[String]]) extends TaskManagerRequest

  sealed trait TaskManagerResponse

  case class TaskList(tasks: List[String]) extends TaskManagerResponse
  case class TaskListById(tasks: Map[String, List[String]]) extends TaskManagerResponse
  case class AvailableTaskList(tasks: List[String]) extends TaskManagerResponse
  case class AvailableTaskMap(taskMap: Map[String, List[String]]) extends TaskManagerResponse

  sealed trait TaskManagerEvent

  case class TaskAdded(taskId: String, taskName: String) extends TaskManagerEvent
  case class TaskRemoved(taskName: String) extends TaskManagerEvent
}
