package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChapterEntity
import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY id ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>): List<Long>

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters ORDER BY chapterNumber ASC")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY chapterNumber ASC")
    fun getChaptersForSubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>): List<Long>

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("UPDATE chapters SET status = :status, completedAtMillis = :completedAt WHERE id = :id")
    suspend fun updateChapterStatus(id: Long, status: String, completedAt: Long?)

    @Query("UPDATE chapters SET isRevisionDone = :isRevisionDone WHERE id = :id")
    suspend fun updateChapterRevision(id: Long, isRevisionDone: Boolean)
}

@Dao
interface TaskDao : StudyTaskDao


@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlotEntity>): List<Long>

    @Update
    suspend fun updateSlot(slot: TimetableSlotEntity)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlotEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY isPinned DESC, updatedAtMillis DESC")
    fun getNotesForSubject(subjectId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>): List<Long>

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun togglePin(id: Long, isPinned: Boolean)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examDateMillis ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>): List<Long>

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)
}
