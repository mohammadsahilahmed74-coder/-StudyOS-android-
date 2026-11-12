package com.example.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ChapterDao
import com.example.data.dao.ExamDao
import com.example.data.dao.NoteDao
import com.example.data.dao.StudyTaskDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TimetableDao
import com.example.data.database.AppDatabase
import com.example.data.model.ChapterEntity
import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.StudyTask
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableSlotEntity
import com.example.data.repository.StudyRepository
import com.example.data.repository.StudyTaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Singleton dependency provider object for AppDatabase, DAOs, and Repositories.
 */
object DatabaseProvider {

    @Volatile
    private var databaseInstance: AppDatabase? = null

    @Volatile
    private var studyTaskRepositoryInstance: StudyTaskRepository? = null

    @Volatile
    private var studyRepositoryInstance: StudyRepository? = null

    /**
     * Provides the singleton AppDatabase instance.
     */
    fun provideDatabase(context: Context): AppDatabase {
        return databaseInstance ?: synchronized(this) {
            databaseInstance ?: buildDatabase(context.applicationContext).also {
                databaseInstance = it
            }
        }
    }

    /**
     * Builds the AppDatabase Room instance with callback for default dataset prepopulation.
     */
    private fun buildDatabase(appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "study_os_db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(provideDatabase(appContext))
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides the singleton StudyTaskDao instance.
     */
    fun provideStudyTaskDao(context: Context): StudyTaskDao {
        return provideDatabase(context).studyTaskDao()
    }

    /**
     * Provides the singleton TaskDao instance.
     */
    fun provideTaskDao(context: Context): TaskDao {
        return provideDatabase(context).taskDao()
    }

    /**
     * Provides the singleton SubjectDao instance.
     */
    fun provideSubjectDao(context: Context): SubjectDao {
        return provideDatabase(context).subjectDao()
    }

    /**
     * Provides the singleton ChapterDao instance.
     */
    fun provideChapterDao(context: Context): ChapterDao {
        return provideDatabase(context).chapterDao()
    }

    /**
     * Provides the singleton TimetableDao instance.
     */
    fun provideTimetableDao(context: Context): TimetableDao {
        return provideDatabase(context).timetableDao()
    }

    /**
     * Provides the singleton NoteDao instance.
     */
    fun provideNoteDao(context: Context): NoteDao {
        return provideDatabase(context).noteDao()
    }

    /**
     * Provides the singleton ExamDao instance.
     */
    fun provideExamDao(context: Context): ExamDao {
        return provideDatabase(context).examDao()
    }

    /**
     * Provides the singleton StudyTaskRepository instance.
     */
    fun provideStudyTaskRepository(context: Context): StudyTaskRepository {
        return studyTaskRepositoryInstance ?: synchronized(this) {
            studyTaskRepositoryInstance ?: StudyTaskRepository(
                studyTaskDao = provideStudyTaskDao(context)
            ).also { studyTaskRepositoryInstance = it }
        }
    }

    /**
     * Provides the aggregate StudyRepository instance for multi-entity workflows.
     */
    fun provideStudyRepository(context: Context): StudyRepository {
        return studyRepositoryInstance ?: synchronized(this) {
            val db = provideDatabase(context)
            studyRepositoryInstance ?: StudyRepository(
                subjectDao = db.subjectDao(),
                chapterDao = db.chapterDao(),
                taskDao = db.taskDao(),
                timetableDao = db.timetableDao(),
                noteDao = db.noteDao(),
                examDao = db.examDao()
            ).also { studyRepositoryInstance = it }
        }
    }

    private suspend fun populateInitialData(database: AppDatabase) {
        val subjectDao = database.subjectDao()
        val chapterDao = database.chapterDao()
        val taskDao = database.studyTaskDao()
        val timetableDao = database.timetableDao()
        val noteDao = database.noteDao()
        val examDao = database.examDao()

        // 1. Initial 5 core subjects
        val physicsId = subjectDao.insertSubject(
            SubjectEntity(name = "Physics", colorHex = "#2563EB", iconKey = "atom", targetHoursPerWeek = 8)
        )
        val chemistryId = subjectDao.insertSubject(
            SubjectEntity(name = "Chemistry", colorHex = "#059669", iconKey = "flask", targetHoursPerWeek = 8)
        )
        val biologyId = subjectDao.insertSubject(
            SubjectEntity(name = "Biology", colorHex = "#7C3AED", iconKey = "dna", targetHoursPerWeek = 8)
        )
        val hindiId = subjectDao.insertSubject(
            SubjectEntity(name = "Hindi", colorHex = "#D97706", iconKey = "book", targetHoursPerWeek = 5)
        )
        val englishId = subjectDao.insertSubject(
            SubjectEntity(name = "English", colorHex = "#DB2777", iconKey = "translate", targetHoursPerWeek = 5)
        )

        // 2. Chapters for Physics
        chapterDao.insertChapters(
            listOf(
                ChapterEntity(subjectId = physicsId, chapterNumber = 1, title = "Units & Measurements", status = "COMPLETED", isRevisionDone = true),
                ChapterEntity(subjectId = physicsId, chapterNumber = 2, title = "Motion in a Straight Line", status = "COMPLETED", isRevisionDone = false),
                ChapterEntity(subjectId = physicsId, chapterNumber = 3, title = "Motion in a Plane", status = "IN_PROGRESS", isRevisionDone = false),
                ChapterEntity(subjectId = physicsId, chapterNumber = 4, title = "Laws of Motion & Friction", status = "IN_PROGRESS", isRevisionDone = false),
                ChapterEntity(subjectId = physicsId, chapterNumber = 5, title = "Work, Energy & Power", status = "NOT_STARTED", isRevisionDone = false),
                ChapterEntity(subjectId = physicsId, chapterNumber = 6, title = "System of Particles & Rotational Motion", status = "NOT_STARTED", isRevisionDone = false),
                ChapterEntity(subjectId = physicsId, chapterNumber = 7, title = "Gravitation", status = "NOT_STARTED", isRevisionDone = false)
            )
        )

        // Chapters for Chemistry
        chapterDao.insertChapters(
            listOf(
                ChapterEntity(subjectId = chemistryId, chapterNumber = 1, title = "Some Basic Concepts of Chemistry", status = "COMPLETED", isRevisionDone = true),
                ChapterEntity(subjectId = chemistryId, chapterNumber = 2, title = "Structure of Atom", status = "COMPLETED", isRevisionDone = true),
                ChapterEntity(subjectId = chemistryId, chapterNumber = 3, title = "Classification of Elements & Periodicity", status = "IN_PROGRESS", isRevisionDone = false),
                ChapterEntity(subjectId = chemistryId, chapterNumber = 4, title = "Chemical Bonding & Molecular Structure", status = "NOT_STARTED", isRevisionDone = false),
                ChapterEntity(subjectId = chemistryId, chapterNumber = 5, title = "Thermodynamics & Equilibrium", status = "NOT_STARTED", isRevisionDone = false),
                ChapterEntity(subjectId = chemistryId, chapterNumber = 6, title = "Organic Chemistry - Basic Principles", status = "NOT_STARTED", isRevisionDone = false)
            )
        )

        // Chapters for Biology
        chapterDao.insertChapters(
            listOf(
                ChapterEntity(subjectId = biologyId, chapterNumber = 1, title = "The Living World", status = "COMPLETED", isRevisionDone = true),
                ChapterEntity(subjectId = biologyId, chapterNumber = 2, title = "Biological Classification", status = "COMPLETED", isRevisionDone = false),
                ChapterEntity(subjectId = biologyId, chapterNumber = 3, title = "Plant Kingdom", status = "IN_PROGRESS", isRevisionDone = false),
                ChapterEntity(subjectId = biologyId, chapterNumber = 4, title = "Animal Kingdom", status = "NOT_STARTED", isRevisionDone = false),
                ChapterEntity(subjectId = biologyId, chapterNumber = 5, title = "Cell: The Unit of Life", status = "NOT_STARTED", isRevisionDone = false),
                ChapterEntity(subjectId = biologyId, chapterNumber = 6, title = "Human Physiology", status = "NOT_STARTED", isRevisionDone = false)
            )
        )

        // Chapters for Hindi
        chapterDao.insertChapters(
            listOf(
                ChapterEntity(subjectId = hindiId, chapterNumber = 1, title = "सूरदास के पद (Surdas ke Pad)", status = "COMPLETED", isRevisionDone = true),
                ChapterEntity(subjectId = hindiId, chapterNumber = 2, title = "नेताजी का चश्मा (Netaji ka Chashma)", status = "COMPLETED", isRevisionDone = true),
                ChapterEntity(subjectId = hindiId, chapterNumber = 3, title = "बालगोबिन भगत (Bal Gobin Bhagat)", status = "IN_PROGRESS", isRevisionDone = false),
                ChapterEntity(subjectId = hindiId, chapterNumber = 4, title = "यह दंतुरित मुस्कान (Yah Danturit Muskan)", status = "NOT_STARTED", isRevisionDone = false),
                ChapterEntity(subjectId = hindiId, chapterNumber = 5, title = "व्याकरण: रस एवं वाक्य भेद (Grammar)", status = "NOT_STARTED", isRevisionDone = false)
            )
        )

        // Chapters for English
        chapterDao.insertChapters(
            listOf(
                ChapterEntity(subjectId = englishId, chapterNumber = 1, title = "A Letter to God", status = "COMPLETED", isRevisionDone = true),
                ChapterEntity(subjectId = englishId, chapterNumber = 2, title = "Nelson Mandela: Long Walk to Freedom", status = "COMPLETED", isRevisionDone = false),
                ChapterEntity(subjectId = englishId, chapterNumber = 3, title = "Two Stories About Flying", status = "IN_PROGRESS", isRevisionDone = false),
                ChapterEntity(subjectId = englishId, chapterNumber = 4, title = "From the Diary of Anne Frank", status = "NOT_STARTED", isRevisionDone = false),
                ChapterEntity(subjectId = englishId, chapterNumber = 5, title = "Formal Letter & Analytical Paragraph", status = "NOT_STARTED", isRevisionDone = false)
            )
        )

        // 3. Initial Study Tasks
        val now = System.currentTimeMillis()
        taskDao.insertTasks(
            listOf(
                StudyTask(
                    title = "Solve 15 Numericals on Laws of Motion",
                    description = "Focus on pulleys, friction blocks, and free body diagrams",
                    subjectId = physicsId,
                    dueDate = now,
                    priority = "HIGH",
                    isCompleted = false,
                    estimatedMinutes = 60
                ),
                StudyTask(
                    title = "Revise Chemical Bonding & Hybridization",
                    description = "Draw VSEPR shapes and note bond angles for sp, sp2, sp3",
                    subjectId = chemistryId,
                    dueDate = now,
                    priority = "MEDIUM",
                    isCompleted = false,
                    estimatedMinutes = 45
                ),
                StudyTask(
                    title = "Practice Biology Diagram: Cell Membrane",
                    description = "Fluid Mosaic Model diagram with fluid lipid bilayer labels",
                    subjectId = biologyId,
                    dueDate = now,
                    priority = "MEDIUM",
                    isCompleted = true,
                    estimatedMinutes = 30,
                    completedAtMillis = now - 3600000
                ),
                StudyTask(
                    title = "Draft English Analytical Paragraph",
                    description = "Write 120-word response analyzing pie chart data",
                    subjectId = englishId,
                    dueDate = now + 86400000,
                    priority = "LOW",
                    isCompleted = false,
                    estimatedMinutes = 35
                )
            )
        )

        // 4. Timetable slots
        val sampleSlots = listOf(
            TimetableSlotEntity(dayOfWeek = 1, startTime = "08:30", endTime = "10:00", subjectId = physicsId, topic = "Mechanics & Problem Solving", slotType = "SELF_STUDY"),
            TimetableSlotEntity(dayOfWeek = 1, startTime = "10:30", endTime = "12:00", subjectId = chemistryId, topic = "Atomic Models & Quantum Numbers", slotType = "LECTURE"),
            TimetableSlotEntity(dayOfWeek = 1, startTime = "14:00", endTime = "15:30", subjectId = biologyId, topic = "Plant Classification Taxonomy", slotType = "REVISION"),
            TimetableSlotEntity(dayOfWeek = 1, startTime = "16:30", endTime = "17:30", subjectId = englishId, topic = "Literature Reading & Vocabulary", slotType = "PRACTICE"),
            TimetableSlotEntity(dayOfWeek = 2, startTime = "09:00", endTime = "10:30", subjectId = chemistryId, topic = "Organic Reaction Mechanisms", slotType = "SELF_STUDY"),
            TimetableSlotEntity(dayOfWeek = 2, startTime = "11:00", endTime = "12:30", subjectId = physicsId, topic = "Kinematics Equations & Graphs", slotType = "PRACTICE"),
            TimetableSlotEntity(dayOfWeek = 2, startTime = "14:30", endTime = "16:00", subjectId = hindiId, topic = "Kavita Arth evam Bhaav", slotType = "LECTURE")
        )
        timetableDao.insertSlots(sampleSlots)

        // 5. Initial Notes
        noteDao.insertNotes(
            listOf(
                NoteEntity(
                    title = "Welcome to StudyOS! 🎓",
                    content = "StudyOS is your personal high-performance academic operating system.\n\n✨ Key Features:\n• Dashboard: Today's tasks, live exam countdown, and quick timetable\n• Task Manager: Set priorities, due dates, and check off completed work\n• Subjects & Chapters: Full syllabus progress tracker for Physics, Chemistry, Biology, Hindi, English, and custom subjects\n• Timetable: Flexible daily & weekly schedule planner\n• Notes: Instant revision notebook with quick search and pinning\n• Analytics: Live countdowns and progress analytics\n\nAll data is stored directly and persistently on your device!",
                    subjectId = null,
                    isPinned = true
                ),
                NoteEntity(
                    title = "Physics: Important Mechanics Formulas",
                    content = "1. v = u + at\n2. s = ut + 0.5 * a * t^2\n3. v^2 = u^2 + 2as\n4. F = dp/dt = m*a\n5. Friction f_s <= μ_s * N\n6. Work W = F · d = F * d * cos(θ)\n7. Kinetic Energy KE = 0.5 * m * v^2",
                    subjectId = physicsId,
                    isPinned = true
                )
            )
        )

        // 6. Upcoming Exams Countdown
        val oneDay = 86400000L
        examDao.insertExams(
            listOf(
                ExamEntity(
                    title = "Final Board Examinations",
                    subjectId = null,
                    examDateMillis = now + (42 * oneDay),
                    targetScore = "98%",
                    notes = "Target: Top percentile. Daily 4h revision schedule."
                ),
                ExamEntity(
                    title = "Physics & Chemistry Mid-Terms",
                    subjectId = physicsId,
                    examDateMillis = now + (15 * oneDay),
                    targetScore = "95%+",
                    notes = "Focus on derivations and numerical problems."
                )
            )
        )
    }
}
