/*
 * Copyright 2023 Joel Kanyi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.joelkanyi.focusbloom.feature.taskprogress

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import com.joelkanyi.focusbloom.core.domain.model.SessionType
import com.joelkanyi.focusbloom.core.domain.model.Task
import com.joelkanyi.focusbloom.core.domain.repository.settings.SettingsRepository
import com.joelkanyi.focusbloom.core.domain.repository.tasks.TasksRepository
import com.joelkanyi.focusbloom.core.utils.sessionType
import com.joelkanyi.focusbloom.core.utils.toMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskProgressScreenModel(
    settingsRepository: SettingsRepository,
    private val tasksRepository: TasksRepository
) : ScreenModel {
    val shortBreakColor = settingsRepository.shortBreakColor()
        .map { it }
        .stateIn(
            coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    val longBreakColor = settingsRepository.longBreakColor()
        .map { it }
        .stateIn(
            coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    val focusColor = settingsRepository.focusColor()
        .map { it }
        .stateIn(
            coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    val focusTime = settingsRepository.getSessionTime()
        .map {
            it?.toMillis() ?: (25).toMillis()
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
    val shortBreakTime = settingsRepository.getShortBreakTime()
        .map {
            it?.toMillis() ?: (5).toMillis()
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
    val longBreakTime = settingsRepository.getLongBreakTime()
        .map { it?.toMillis() ?: (15).toMillis() }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _task = MutableStateFlow<Task?>(null)
    val task = _task.asStateFlow()
    fun getTask(taskId: Int) {
        coroutineScope.launch {
            tasksRepository.getTask(taskId).collectLatest {
                _task.value = it
            }
        }
    }

    /**
     * This function updates the consumed focus time of the task
     * @param taskId the id of the task
     * @param consumedTime the consumed time of the focus
     */
    private fun updateConsumedFocusTime(taskId: Int, consumedTime: Long) {
        coroutineScope.launch {
            tasksRepository.updateConsumedFocusTime(taskId, consumedTime)
        }
    }

    /**
     * This function updates the consumed short break time of the task
     * @param taskId the id of the task
     * @param consumedTime the consumed time of the short break
     */
    private fun updateConsumedShortBreakTime(taskId: Int, consumedTime: Long) {
        coroutineScope.launch {
            tasksRepository.updateConsumedShortBreakTime(taskId, consumedTime)
        }
    }

    /**
     * This function updates the consumed long break time of the task
     * @param taskId the id of the task
     * @param consumedTime the consumed time of the long break
     */
    private fun updateConsumedLongBreakTime(taskId: Int, consumedTime: Long) {
        coroutineScope.launch {
            tasksRepository.updateConsumedLongBreakTime(taskId, consumedTime)
        }
    }

    /**
     * This function updates task as either in progress or not in progress
     * @param taskId the id of the task
     * @param inProgressTask the in progress task
     */
    private fun updateInProgressTask(taskId: Int, inProgressTask: Boolean) {
        coroutineScope.launch {
            tasksRepository.updateTaskInProgress(taskId, inProgressTask)
        }
    }

    fun updateActiveTask(taskId: Int, activeTask: Boolean) {
        coroutineScope.launch {
            tasksRepository.updateTaskActive(id = taskId, active = activeTask)
        }
    }

    /**
     * This function updates task as either completed or not completed
     * @param taskId the id of the task
     * @param completedTask the completed task
     */
    private fun updateCompletedTask(taskId: Int, completedTask: Boolean) {
        coroutineScope.launch {
            tasksRepository.updateTaskCompleted(taskId, completedTask)
        }
    }

    fun resetAllTasksToInactive() {
        coroutineScope.launch {
            tasksRepository.updateAllTasksActiveStatusToInactive()
        }
    }

    /**
     * This function updates the current cycle of the task
     * @param taskId the id of the task
     * @param currentCycle the current cycle of the task
     */
    private fun updateCurrentCycle(taskId: Int, currentCycle: Int) {
        coroutineScope.launch {
            tasksRepository.updateTaskCycleNumber(taskId, currentCycle)
        }
    }

    /**
     * This function updates the current session of the task (Focus, ShortBreak, LongBreak)
     * @param taskId the id of the task
     * @param currentSession the current session of the task
     */
    private fun updateCurrentSession(taskId: Int, currentSession: String) {
        coroutineScope.launch {
            tasksRepository.updateCurrentSessionName(taskId, currentSession)
        }
    }

    fun updateConsumedTime() {
        when (task.value?.current) {
            "Focus" -> updateConsumedFocusTime(
                task.value?.id ?: -1,
                Timer.tickingTime.value
            )

            "ShortBreak" -> updateConsumedShortBreakTime(
                task.value?.id ?: -1,
                Timer.tickingTime.value
            )

            "LongBreak" -> updateConsumedLongBreakTime(
                task.value?.id ?: -1,
                Timer.tickingTime.value
            )
        }
    }

    fun executeTasks() {
        coroutineScope.launch {
            if (task.value?.currentCycle?.equals(0) == true) {
                println("executeTasks: first cycle")
                updateCurrentCycle(task.value?.id ?: 0, 1)
                updateCurrentSession(task.value?.id ?: 0, "Focus")
                updateInProgressTask(task.value?.id ?: 0, true)
                Timer.setTickingTime(focusTime.value ?: 0L)
                Timer.start(
                    update = {
                        updateConsumedTime()
                    },
                    executeTasks = {
                        executeTasks()
                    }
                )
            } else {
                when (task.value?.current) {
                    "Focus" -> {
                        if (task.value?.currentCycle == task.value?.focusSessions) {
                            println("executeTasks: going for a long break after a focus session")
                            updateCurrentSession(task.value?.id ?: 0, "LongBreak")
                            updateInProgressTask(task.value?.id ?: 0, true)
                            Timer.setTickingTime(longBreakTime.value ?: 0L)
                            Timer.start(
                                update = {
                                    updateConsumedTime()
                                },
                                executeTasks = {
                                    executeTasks()
                                }
                            )
                        } else {
                            println("executeTasks: going for a short break after a focus session")
                            updateCurrentSession(task.value?.id ?: 0, "ShortBreak")
                            updateInProgressTask(task.value?.id ?: 0, true)
                            Timer.setTickingTime(shortBreakTime.value ?: 0L)
                            Timer.start(
                                update = {
                                    updateConsumedTime()
                                },
                                executeTasks = {
                                    executeTasks()
                                }
                            )
                        }
                    }

                    "ShortBreak" -> {
                        println("executeTasks: going for a focus session after a short break")
                        updateCurrentSession(task.value?.id ?: 0, "Focus")
                        updateCurrentCycle(
                            task.value?.id ?: 0,
                            task.value?.currentCycle?.plus(1) ?: (0 + 1)
                        )
                        updateInProgressTask(task.value?.id ?: 0, true)
                        Timer.setTickingTime(focusTime.value ?: 0L)
                        Timer.start(
                            update = {
                                updateConsumedTime()
                            },
                            executeTasks = {
                                executeTasks()
                            }
                        )
                    }

                    "LongBreak" -> {
                        println("executeTasks: completed all cycles")
                        val taskId = task.value?.id ?: 0
                        updateInProgressTask(taskId, false)
                        updateCompletedTask(taskId, true)
                        updateActiveTask(taskId, false)
                        Timer.stop()
                        Timer.reset()
                    }
                }
            }
        }
    }

    fun moveToNextSessionOfTheTask() {
        coroutineScope.launch {
            when (task.value?.current.sessionType()) {
                SessionType.Focus -> {
                    if (task.value?.currentCycle == task.value?.focusSessions) {
                        updateCurrentSession(task.value?.id ?: 0, "LongBreak")
                        updateInProgressTask(task.value?.id ?: 0, true)
                        Timer.setTickingTime(longBreakTime.value ?: 0L)
                        Timer.start(
                            update = {
                                updateConsumedTime()
                            },
                            executeTasks = {
                                executeTasks()
                            }
                        )
                    } else {
                        updateCurrentSession(task.value?.id ?: 0, "ShortBreak")
                        updateInProgressTask(task.value?.id ?: 0, true)
                        Timer.setTickingTime(shortBreakTime.value ?: 0L)
                        Timer.start(
                            update = {
                                updateConsumedTime()
                            },
                            executeTasks = {
                                executeTasks()
                            }
                        )
                    }
                }

                SessionType.LongBreak -> {
                    val taskId = task.value?.id ?: 0
                    updateInProgressTask(taskId, false)
                    updateCompletedTask(taskId, true)
                    updateActiveTask(taskId, false)
                    Timer.stop()
                    Timer.reset()
                }

                SessionType.ShortBreak -> {
                    updateCurrentSession(task.value?.id ?: 0, "Focus")
                    updateCurrentCycle(
                        task.value?.id ?: 0,
                        task.value?.currentCycle?.plus(1) ?: (0 + 1)
                    )
                    updateInProgressTask(task.value?.id ?: 0, true)
                    Timer.setTickingTime(focusTime.value ?: 0L)
                    Timer.start(
                        update = {
                            updateConsumedTime()
                        },
                        executeTasks = {
                            executeTasks()
                        }
                    )
                }
            }
        }
    }

    fun resetCurrentSessionOfTheTask() {
        coroutineScope.launch {
            when (task.value?.current.sessionType()) {
                SessionType.Focus -> {
                    updateCurrentSession(task.value?.id ?: 0, "Focus")
                    updateInProgressTask(task.value?.id ?: 0, true)
                    Timer.setTickingTime(focusTime.value ?: 0L)
                    Timer.start(
                        update = {
                            updateConsumedTime()
                        },
                        executeTasks = {
                            executeTasks()
                        }
                    )
                }

                SessionType.LongBreak -> {
                    val taskId = task.value?.id ?: 0
                    updateInProgressTask(taskId, false)
                    updateCompletedTask(taskId, true)
                    updateActiveTask(taskId, false)
                    Timer.stop()
                    Timer.reset()
                }

                SessionType.ShortBreak -> {
                    updateCurrentSession(task.value?.id ?: 0, "ShortBreak")
                    updateInProgressTask(task.value?.id ?: 0, true)
                    Timer.setTickingTime(shortBreakTime.value ?: 0L)
                    Timer.start(
                        update = {
                            updateConsumedTime()
                        },
                        executeTasks = {
                            executeTasks()
                        }
                    )
                }
            }
        }
    }
}
