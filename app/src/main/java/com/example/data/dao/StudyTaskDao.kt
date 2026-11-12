package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StudyTask
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object interface for the StudyTask entity.
 */
@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateMillis ASC, id DESC")
    fun getAllTasks(): Flow<List<StudyTask>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): StudyTask?

    @Query("SELECT * FROM tasks WHERE dueDateMillis >= :startOfDay AND dueDateMillis <= :endOfDay ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTask>): List<Long>

    @Update
    suspend fun updateTask(task: StudyTask)

    @Delete
    suspend fun deleteTask(task: StudyTask)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun toggleTaskCompleted(id: Long, isCompleted: Boolean, completedAt: Long?)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()
}
