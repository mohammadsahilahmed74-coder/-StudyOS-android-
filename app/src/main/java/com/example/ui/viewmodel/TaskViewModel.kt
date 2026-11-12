package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.StudyTask
import com.example.data.repository.StudyTaskRepository
import com.example.data.repository.StudyTaskRepositoryInterface
import com.example.di.DatabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * TaskViewModel manages and exposes Flow-based task lists to the UI
 * and coordinates all Add, Update, Toggle, and Delete operations
 * using the StudyTaskRepository.
 */
class TaskViewModel(
    application: Application,
    private val taskRepository: StudyTaskRepositoryInterface = DatabaseProvider.provideStudyTaskRepository(application)
) : AndroidViewModel(application) {

    // Raw stream of all tasks from repository
    val allTasks: StateFlow<List<StudyTask>> = taskRepository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter and search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow("ALL") // ALL, PENDING, COMPLETED
    val filterStatus: StateFlow<String> = _filterStatus.asStateFlow()

    private val _filterSubjectId = MutableStateFlow<Long?>(null)
    val filterSubjectId: StateFlow<Long?> = _filterSubjectId.asStateFlow()

    // Filtered tasks stream for the UI
    val filteredTasks: StateFlow<List<StudyTask>> = combine(
        allTasks,
        _searchQuery,
        _filterStatus,
        _filterSubjectId
    ) { tasks, query, status, subjectId ->
        tasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "PENDING" -> !task.isCompleted
                "COMPLETED" -> task.isCompleted
                else -> true
            }

            val matchesSubject = subjectId == null || task.subjectId == subjectId

            matchesQuery && matchesStatus && matchesSubject
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Today's tasks stream for dashboard
    val todayTasks: StateFlow<List<StudyTask>> = allTasks.combine(MutableStateFlow(Unit)) { tasksList, _ ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis

        tasksList.filter { task ->
            (task.dueDate in startOfDay until endOfDay) ||
                (!task.isCompleted && task.dueDate < endOfDay)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filter mutators
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun setFilterSubjectId(subjectId: Long?) {
        _filterSubjectId.value = subjectId
    }

    /**
     * Add a new study task.
     */
    fun addTask(
        title: String,
        description: String = "",
        subjectId: Long? = null,
        dueDate: Long = System.currentTimeMillis(),
        priority: String = "MEDIUM",
        estimatedMinutes: Int = 45
    ) = viewModelScope.launch {
        taskRepository.insertTask(
            StudyTask(
                title = title.trim(),
                description = description.trim(),
                dueDate = dueDate,
                isCompleted = false,
                subjectId = subjectId,
                priority = priority,
                estimatedMinutes = estimatedMinutes
            )
        )
    }

    /**
     * Update an existing study task.
     */
    fun updateTask(task: StudyTask) = viewModelScope.launch {
        taskRepository.updateTask(task)
    }

    /**
     * Toggle a study task's completion status.
     */
    fun toggleTaskComplete(task: StudyTask) = viewModelScope.launch {
        val newStatus = !task.isCompleted
        val completedAt = if (newStatus) System.currentTimeMillis() else null
        taskRepository.toggleTaskCompleted(task.id, newStatus, completedAt)
    }

    /**
     * Delete a study task.
     */
    fun deleteTask(task: StudyTask) = viewModelScope.launch {
        taskRepository.deleteTask(task)
    }

    /**
     * Delete all completed tasks.
     */
    fun clearCompletedTasks() = viewModelScope.launch {
        taskRepository.deleteCompletedTasks()
    }

    /**
     * Factory for creating TaskViewModel instances with a custom repository (e.g. for testing).
     */
    companion object {
        fun provideFactory(
            application: Application,
            repository: StudyTaskRepositoryInterface
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskViewModel(application, repository) as T
            }
        }
    }
}
