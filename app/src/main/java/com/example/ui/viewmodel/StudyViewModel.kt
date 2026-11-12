package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.StudyDatabase
import com.example.data.model.ChapterEntity
import com.example.data.library.StudyLibraryStorageManager
import com.example.data.model.ExamEntity
import com.example.data.model.NoteAttachment
import com.example.data.model.NoteEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TimetableSlotEntity
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class SubjectProgress(
    val subject: SubjectEntity,
    val totalChapters: Int,
    val completedChapters: Int,
    val inProgressChapters: Int,
    val revisedChapters: Int,
    val completionPercent: Float
)

data class ExamCountdown(
    val exam: ExamEntity,
    val daysRemaining: Long,
    val hoursRemaining: Long,
    val isPast: Boolean
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = com.example.di.DatabaseProvider.provideStudyRepository(application)


    // Raw Streams from Room
    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chapters: StateFlow<List<ChapterEntity>> = repository.allChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timetableSlots: StateFlow<List<TimetableSlotEntity>> = repository.allTimetableSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                val samplePdf = StudyLibraryStorageManager.createSamplePdfIfMissing(application)
                if (samplePdf != null) {
                    val list = notes.first { it.isNotEmpty() }
                    val physicsNote = list.find { it.title.contains("Mechanics", ignoreCase = true) }
                    if (physicsNote != null && physicsNote.getAttachments().isEmpty()) {
                        val updated = physicsNote.copy(
                            chapter = "Laws of Motion & Mechanics",
                            category = "Formulas & Cheatsheet",
                            attachmentsJson = NoteAttachment.toJsonString(listOf(samplePdf))
                        )
                        updateNote(updated)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // UI Navigation & Preferences State
    private val _currentTab = MutableStateFlow(0) // 0: Dashboard, 1: Tasks, 2: Subjects, 3: Timetable, 4: Notes, 5: Analytics
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null = system, true = dark, false = light
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    // Tasks Filtering
    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    private val _taskFilterStatus = MutableStateFlow("ALL") // ALL, PENDING, COMPLETED
    val taskFilterStatus: StateFlow<String> = _taskFilterStatus.asStateFlow()

    private val _taskFilterSubjectId = MutableStateFlow<Long?>(null)
    val taskFilterSubjectId: StateFlow<Long?> = _taskFilterSubjectId.asStateFlow()

    // Subjects & Chapters Screen State
    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    val selectedSubjectId: StateFlow<Long?> = _selectedSubjectId.asStateFlow()

    // Timetable Screen State
    private val _selectedDayOfWeek = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let {
        // Convert Java Calendar (Sun=1, Mon=2..Sat=7) to StudyOS standard (Mon=1..Sun=7)
        if (it == Calendar.SUNDAY) 7 else it - 1
    })
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    // Digital Study Library & Notes Filtering
    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = _noteSearchQuery.asStateFlow()

    private val _noteFilterSubjectId = MutableStateFlow<Long?>(null)
    val noteFilterSubjectId: StateFlow<Long?> = _noteFilterSubjectId.asStateFlow()

    private val _noteFilterCategory = MutableStateFlow<String?>(null)
    val noteFilterCategory: StateFlow<String?> = _noteFilterCategory.asStateFlow()

    private val _noteFilterFileType = MutableStateFlow("ALL") // "ALL", "PDF", "IMAGE", "DOC"
    val noteFilterFileType: StateFlow<String> = _noteFilterFileType.asStateFlow()

    private val _libraryViewMode = MutableStateFlow("NOTES") // "NOTES", "FILES"
    val libraryViewMode: StateFlow<String> = _libraryViewMode.asStateFlow()

    // Computed Progress by Subject
    val subjectsProgress: StateFlow<List<SubjectProgress>> = combine(subjects, chapters) { subjList, chapList ->
        subjList.map { subj ->
            val subChapters = chapList.filter { it.subjectId == subj.id }
            val completed = subChapters.count { it.status == "COMPLETED" }
            val inProgress = subChapters.count { it.status == "IN_PROGRESS" }
            val revised = subChapters.count { it.isRevisionDone }
            val total = subChapters.size
            val percent = if (total > 0) completed.toFloat() / total else 0f
            SubjectProgress(
                subject = subj,
                totalChapters = total,
                completedChapters = completed,
                inProgressChapters = inProgress,
                revisedChapters = revised,
                completionPercent = percent
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall Syllabus Completion
    val overallSyllabusProgress: StateFlow<Float> = chapters.combine(subjectsProgress) { chapList, _ ->
        if (chapList.isEmpty()) 0f
        else chapList.count { it.status == "COMPLETED" }.toFloat() / chapList.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Today's Tasks
    val todayTasks: StateFlow<List<TaskEntity>> = tasks.combine(MutableStateFlow(Unit)) { allTasksList, _ ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis

        // Tasks scheduled for today or pending from before
        allTasksList.filter { task ->
            (task.dueDateMillis in startOfDay until endOfDay) ||
            (!task.isCompleted && task.dueDateMillis < endOfDay)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exam Countdowns
    val examCountdowns: StateFlow<List<ExamCountdown>> = exams.combine(MutableStateFlow(Unit)) { examList, _ ->
        val now = System.currentTimeMillis()
        examList.map { exam ->
            val diff = exam.examDateMillis - now
            val isPast = diff < 0
            val positiveDiff = if (isPast) 0L else diff
            val days = positiveDiff / (1000 * 60 * 60 * 24)
            val hours = (positiveDiff / (1000 * 60 * 60)) % 24
            ExamCountdown(
                exam = exam,
                daysRemaining = days,
                hoursRemaining = hours,
                isPast = isPast
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Setters for Navigation & Filter State
    fun setTab(tab: Int) {
        _currentTab.value = tab
    }

    fun toggleDarkMode() {
        _isDarkMode.value = when (_isDarkMode.value) {
            null -> true
            true -> false
            false -> null
        }
    }

    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    fun setTaskFilterStatus(status: String) {
        _taskFilterStatus.value = status
    }

    fun setTaskFilterSubjectId(subjectId: Long?) {
        _taskFilterSubjectId.value = subjectId
    }

    fun setSelectedSubjectId(subjectId: Long?) {
        _selectedSubjectId.value = subjectId
    }

    fun setSelectedDayOfWeek(day: Int) {
        _selectedDayOfWeek.value = day
    }

    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    fun setNoteFilterSubjectId(subjectId: Long?) {
        _noteFilterSubjectId.value = subjectId
    }

    fun setNoteFilterCategory(category: String?) {
        _noteFilterCategory.value = category
    }

    fun setNoteFilterFileType(fileType: String) {
        _noteFilterFileType.value = fileType
    }

    fun setLibraryViewMode(mode: String) {
        _libraryViewMode.value = mode
    }

    // CRUD: Tasks
    fun insertTask(
        title: String,
        description: String,
        subjectId: Long?,
        dueDateMillis: Long,
        priority: String,
        estimatedMinutes: Int
    ) = viewModelScope.launch {
        repository.insertTask(
            TaskEntity(
                title = title.trim(),
                description = description.trim(),
                subjectId = subjectId,
                dueDateMillis = dueDateMillis,
                priority = priority,
                isCompleted = false,
                estimatedMinutes = estimatedMinutes
            )
        )
    }

    fun updateTask(task: TaskEntity) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun toggleTaskCompleted(task: TaskEntity) = viewModelScope.launch {
        val newStatus = !task.isCompleted
        val completedAt = if (newStatus) System.currentTimeMillis() else null
        repository.toggleTaskCompleted(task.id, newStatus, completedAt)
    }

    fun deleteTask(task: TaskEntity) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun clearCompletedTasks() = viewModelScope.launch {
        repository.deleteCompletedTasks()
    }

    // CRUD: Subjects
    fun insertSubject(name: String, colorHex: String, iconKey: String, targetHours: Int) = viewModelScope.launch {
        repository.insertSubject(
            SubjectEntity(
                name = name.trim(),
                colorHex = colorHex,
                iconKey = iconKey,
                targetHoursPerWeek = targetHours
            )
        )
    }

    fun updateSubject(subject: SubjectEntity) = viewModelScope.launch {
        repository.updateSubject(subject)
    }

    fun deleteSubject(subject: SubjectEntity) = viewModelScope.launch {
        if (_selectedSubjectId.value == subject.id) {
            _selectedSubjectId.value = null
        }
        repository.deleteSubject(subject)
    }

    // CRUD: Chapters
    fun insertChapter(subjectId: Long, chapterNumber: Int, title: String, notes: String = "") = viewModelScope.launch {
        repository.insertChapter(
            ChapterEntity(
                subjectId = subjectId,
                chapterNumber = chapterNumber,
                title = title.trim(),
                status = "NOT_STARTED",
                isRevisionDone = false,
                notes = notes
            )
        )
    }

    fun updateChapter(chapter: ChapterEntity) = viewModelScope.launch {
        repository.updateChapter(chapter)
    }

    fun updateChapterStatus(chapter: ChapterEntity, status: String) = viewModelScope.launch {
        val completedAt = if (status == "COMPLETED") System.currentTimeMillis() else null
        repository.updateChapterStatus(chapter.id, status, completedAt)
    }

    fun toggleChapterRevision(chapter: ChapterEntity) = viewModelScope.launch {
        repository.updateChapterRevision(chapter.id, !chapter.isRevisionDone)
    }

    fun deleteChapter(chapter: ChapterEntity) = viewModelScope.launch {
        repository.deleteChapter(chapter)
    }

    // CRUD: Timetable
    fun insertSlot(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        subjectId: Long?,
        topic: String,
        slotType: String,
        roomOrLink: String = ""
    ) = viewModelScope.launch {
        repository.insertSlot(
            TimetableSlotEntity(
                dayOfWeek = dayOfWeek,
                startTime = startTime.trim(),
                endTime = endTime.trim(),
                subjectId = subjectId,
                topic = topic.trim(),
                slotType = slotType,
                roomOrLink = roomOrLink.trim()
            )
        )
    }

    fun updateSlot(slot: TimetableSlotEntity) = viewModelScope.launch {
        repository.updateSlot(slot)
    }

    fun deleteSlot(slot: TimetableSlotEntity) = viewModelScope.launch {
        repository.deleteSlot(slot)
    }

    // CRUD: Notes & Digital Study Library
    fun insertNote(
        title: String,
        content: String,
        subjectId: Long?,
        isPinned: Boolean,
        chapter: String = "",
        category: String = "Lecture Notes",
        attachments: List<NoteAttachment> = emptyList()
    ) = viewModelScope.launch {
        repository.insertNote(
            NoteEntity(
                title = title.trim(),
                content = content.trim(),
                subjectId = subjectId,
                isPinned = isPinned,
                createdAtMillis = System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis(),
                chapter = chapter.trim(),
                category = category.trim().ifBlank { "Lecture Notes" },
                attachmentsJson = NoteAttachment.toJsonString(attachments)
            )
        )
    }

    fun updateNote(note: NoteEntity) = viewModelScope.launch {
        repository.updateNote(note.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    fun deleteNote(note: NoteEntity) = viewModelScope.launch {
        val attachments = note.getAttachments()
        attachments.forEach { att ->
            try {
                StudyLibraryStorageManager.deleteAttachment(getApplication(), att)
            } catch (_: Exception) {}
        }
        repository.deleteNote(note)
    }

    fun toggleNotePin(note: NoteEntity) = viewModelScope.launch {
        repository.toggleNotePin(note.id, !note.isPinned)
    }

    fun addAttachmentToNote(note: NoteEntity, attachment: NoteAttachment) = viewModelScope.launch {
        val currentAttachments = note.getAttachments().toMutableList()
        currentAttachments.add(attachment)
        updateNote(note.withAttachments(currentAttachments))
    }

    fun removeAttachmentFromNote(note: NoteEntity, attachmentId: String) = viewModelScope.launch {
        val currentAttachments = note.getAttachments().toMutableList()
        val toRemove = currentAttachments.find { it.id == attachmentId }
        if (toRemove != null) {
            try {
                StudyLibraryStorageManager.deleteAttachment(getApplication(), toRemove)
            } catch (_: Exception) {}
            currentAttachments.remove(toRemove)
            updateNote(note.withAttachments(currentAttachments))
        }
    }

    fun renameAttachmentInNote(note: NoteEntity, attachmentId: String, newName: String) = viewModelScope.launch {
        val currentAttachments = note.getAttachments().map { att ->
            if (att.id == attachmentId) {
                StudyLibraryStorageManager.renameAttachment(att, newName)
            } else {
                att
            }
        }
        updateNote(note.withAttachments(currentAttachments))
    }

    // CRUD: Exams
    fun insertExam(title: String, subjectId: Long?, examDateMillis: Long, targetScore: String, notes: String) = viewModelScope.launch {
        repository.insertExam(
            ExamEntity(
                title = title.trim(),
                subjectId = subjectId,
                examDateMillis = examDateMillis,
                targetScore = targetScore.trim(),
                notes = notes.trim()
            )
        )
    }

    fun updateExam(exam: ExamEntity) = viewModelScope.launch {
        repository.updateExam(exam)
    }

    fun deleteExam(exam: ExamEntity) = viewModelScope.launch {
        repository.deleteExam(exam)
    }
}
