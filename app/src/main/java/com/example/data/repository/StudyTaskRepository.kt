package com.example.data.repository

import com.example.data.dao.StudyTaskDao
import com.example.data.model.StudyTask
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data access operations for StudyTask.
 */
interface StudyTaskRepositoryInterface {
    val allTasks: Flow<List<StudyTask>>
    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<StudyTask>>
    suspend fun getTaskById(id: Long): StudyTask?
    suspend fun insertTask(task: StudyTask): Long
    suspend fun insertTasks(tasks: List<StudyTask>): List<Long>
    suspend fun updateTask(task: StudyTask)
    suspend fun deleteTask(task: StudyTask)
    suspend fun toggleTaskCompleted(id: Long, isCompleted: Boolean, completedAt: Long?)
    suspend fun deleteCompletedTasks()
}

/**
 * StudyTaskRepository abstracts data operations between the ViewModel and the Room DAO,
 * adhering to Clean Architecture principles.
 */
class StudyTaskRepository(
    private val studyTaskDao: StudyTaskDao
) : StudyTaskRepositoryInterface {

    /**
     * Flow emitting all study tasks ordered by completion and due date.
     */
    override val allTasks: Flow<List<StudyTask>> = studyTaskDao.getAllTasks()

    /**
     * Flow emitting study tasks falling within a specified timestamp range.
     */
    override fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<StudyTask>> =
        studyTaskDao.getTasksForDateRange(startOfDay, endOfDay)

    /**
     * Retrieve a single task by ID.
     */
    override suspend fun getTaskById(id: Long): StudyTask? =
        studyTaskDao.getTaskById(id)

    /**
     * Insert a new study task.
     */
    override suspend fun insertTask(task: StudyTask): Long =
        studyTaskDao.insertTask(task)

    /**
     * Insert multiple study tasks.
     */
    override suspend fun insertTasks(tasks: List<StudyTask>): List<Long> =
        studyTaskDao.insertTasks(tasks)

    /**
     * Update an existing study task.
     */
    override suspend fun updateTask(task: StudyTask) =
        studyTaskDao.updateTask(task)

    /**
     * Delete a study task.
     */
    override suspend fun deleteTask(task: StudyTask) =
        studyTaskDao.deleteTask(task)

    /**
     * Toggle completion status of a study task.
     */
    override suspend fun toggleTaskCompleted(id: Long, isCompleted: Boolean, completedAt: Long?) =
        studyTaskDao.toggleTaskCompleted(id, isCompleted, completedAt)

    /**
     * Delete all completed tasks from the database.
     */
    override suspend fun deleteCompletedTasks() =
        studyTaskDao.deleteCompletedTasks()
}
