package com.example.data.repository

import com.example.data.dao.ChapterDao
import com.example.data.dao.ExamDao
import com.example.data.dao.NoteDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TimetableDao
import com.example.data.model.ChapterEntity
import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow

class StudyRepository(
    private val subjectDao: SubjectDao,
    private val chapterDao: ChapterDao,
    private val taskDao: TaskDao,
    private val timetableDao: TimetableDao,
    private val noteDao: NoteDao,
    private val examDao: ExamDao
) {
    // Subjects
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.deleteSubject(subject)

    // Chapters
    val allChapters: Flow<List<ChapterEntity>> = chapterDao.getAllChapters()
    fun getChaptersForSubject(subjectId: Long): Flow<List<ChapterEntity>> = chapterDao.getChaptersForSubject(subjectId)
    suspend fun insertChapter(chapter: ChapterEntity): Long = chapterDao.insertChapter(chapter)
    suspend fun updateChapter(chapter: ChapterEntity) = chapterDao.updateChapter(chapter)
    suspend fun deleteChapter(chapter: ChapterEntity) = chapterDao.deleteChapter(chapter)
    suspend fun updateChapterStatus(id: Long, status: String, completedAt: Long?) =
        chapterDao.updateChapterStatus(id, status, completedAt)
    suspend fun updateChapterRevision(id: Long, isRevisionDone: Boolean) =
        chapterDao.updateChapterRevision(id, isRevisionDone)

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>> =
        taskDao.getTasksForDateRange(startOfDay, endOfDay)
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    suspend fun toggleTaskCompleted(id: Long, isCompleted: Boolean, completedAt: Long?) =
        taskDao.toggleTaskCompleted(id, isCompleted, completedAt)
    suspend fun deleteCompletedTasks() = taskDao.deleteCompletedTasks()

    // Timetable
    val allTimetableSlots: Flow<List<TimetableSlotEntity>> = timetableDao.getAllSlots()
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlotEntity>> = timetableDao.getSlotsForDay(dayOfWeek)
    suspend fun insertSlot(slot: TimetableSlotEntity): Long = timetableDao.insertSlot(slot)
    suspend fun updateSlot(slot: TimetableSlotEntity) = timetableDao.updateSlot(slot)
    suspend fun deleteSlot(slot: TimetableSlotEntity) = timetableDao.deleteSlot(slot)

    // Notes
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    fun getNotesForSubject(subjectId: Long): Flow<List<NoteEntity>> = noteDao.getNotesForSubject(subjectId)
    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)
    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)
    suspend fun toggleNotePin(id: Long, isPinned: Boolean) = noteDao.togglePin(id, isPinned)

    // Exams
    val allExams: Flow<List<ExamEntity>> = examDao.getAllExams()
    suspend fun insertExam(exam: ExamEntity): Long = examDao.insertExam(exam)
    suspend fun updateExam(exam: ExamEntity) = examDao.updateExam(exam)
    suspend fun deleteExam(exam: ExamEntity) = examDao.deleteExam(exam)
}
