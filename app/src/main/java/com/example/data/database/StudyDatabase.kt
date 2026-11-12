package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        TaskEntity::class,
        TimetableSlotEntity::class,
        NoteEntity::class,
        ExamEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun taskDao(): TaskDao
    abstract fun timetableDao(): TimetableDao
    abstract fun noteDao(): NoteDao
    abstract fun examDao(): ExamDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_os_db"
                )
                    .addCallback(StudyDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class StudyDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: StudyDatabase) {
            val subjectDao = database.subjectDao()
            val chapterDao = database.chapterDao()
            val taskDao = database.taskDao()
            val timetableDao = database.timetableDao()
            val noteDao = database.noteDao()
            val examDao = database.examDao()

            // 1. Create Core 5 Subjects
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
                    TaskEntity(
                        title = "Solve 15 Numericals on Laws of Motion",
                        description = "Focus on pulleys, friction blocks, and free body diagrams",
                        subjectId = physicsId,
                        dueDateMillis = now,
                        priority = "HIGH",
                        isCompleted = false,
                        estimatedMinutes = 60
                    ),
                    TaskEntity(
                        title = "Revise Chemical Bonding & Hybridization",
                        description = "Draw VSEPR shapes and note bond angles for sp, sp2, sp3",
                        subjectId = chemistryId,
                        dueDateMillis = now,
                        priority = "MEDIUM",
                        isCompleted = false,
                        estimatedMinutes = 45
                    ),
                    TaskEntity(
                        title = "Practice Biology Diagram: Cell Membrane",
                        description = "Fluid Mosaic Model diagram with fluid lipid bilayer labels",
                        subjectId = biologyId,
                        dueDateMillis = now,
                        priority = "MEDIUM",
                        isCompleted = true,
                        estimatedMinutes = 30,
                        completedAtMillis = now - 3600000
                    ),
                    TaskEntity(
                        title = "Draft English Analytical Paragraph",
                        description = "Write 120-word response analyzing pie chart data",
                        subjectId = englishId,
                        dueDateMillis = now + 86400000,
                        priority = "LOW",
                        isCompleted = false,
                        estimatedMinutes = 35
                    )
                )
            )

            // 4. Timetable slots for Monday to Friday
            val sampleSlots = listOf(
                TimetableSlotEntity(dayOfWeek = 1, startTime = "08:30", endTime = "10:00", subjectId = physicsId, topic = "Mechanics & Problem Solving", slotType = "SELF_STUDY"),
                TimetableSlotEntity(dayOfWeek = 1, startTime = "10:30", endTime = "12:00", subjectId = chemistryId, topic = "Atomic Models & Quantum Numbers", slotType = "LECTURE"),
                TimetableSlotEntity(dayOfWeek = 1, startTime = "14:00", endTime = "15:30", subjectId = biologyId, topic = "Plant Classification Taxonomy", slotType = "REVISION"),
                TimetableSlotEntity(dayOfWeek = 1, startTime = "16:30", endTime = "17:30", subjectId = englishId, topic = "Literature Reading & Vocabulary", slotType = "PRACTICE"),

                TimetableSlotEntity(dayOfWeek = 2, startTime = "09:00", endTime = "10:30", subjectId = chemistryId, topic = "Organic Reaction Mechanisms", slotType = "SELF_STUDY"),
                TimetableSlotEntity(dayOfWeek = 2, startTime = "11:00", endTime = "12:30", subjectId = physicsId, topic = "Kinematics Equations & Graphs", slotType = "PRACTICE"),
                TimetableSlotEntity(dayOfWeek = 2, startTime = "14:30", endTime = "16:00", subjectId = hindiId, topic = "Kavita Arth evam Bhaav", slotType = "LECTURE"),

                TimetableSlotEntity(dayOfWeek = 3, startTime = "08:30", endTime = "10:00", subjectId = biologyId, topic = "Cellular Respiration & Krebs Cycle", slotType = "SELF_STUDY"),
                TimetableSlotEntity(dayOfWeek = 3, startTime = "10:30", endTime = "12:00", subjectId = physicsId, topic = "Friction & Circular Motion", slotType = "LECTURE"),
                TimetableSlotEntity(dayOfWeek = 3, startTime = "15:00", endTime = "16:30", subjectId = chemistryId, topic = "Periodic Trends Quiz Preparation", slotType = "REVISION"),

                TimetableSlotEntity(dayOfWeek = 4, startTime = "09:00", endTime = "10:30", subjectId = physicsId, topic = "Work Energy Theorem Numericals", slotType = "PRACTICE"),
                TimetableSlotEntity(dayOfWeek = 4, startTime = "11:00", endTime = "12:30", subjectId = biologyId, topic = "Living Organisms Anatomy", slotType = "LECTURE"),
                TimetableSlotEntity(dayOfWeek = 4, startTime = "14:00", endTime = "15:30", subjectId = hindiId, topic = "Patra Lekhan evam Vyakaran", slotType = "SELF_STUDY"),

                TimetableSlotEntity(dayOfWeek = 5, startTime = "08:30", endTime = "10:00", subjectId = chemistryId, topic = "Equilibrium Constant Derivations", slotType = "SELF_STUDY"),
                TimetableSlotEntity(dayOfWeek = 5, startTime = "10:30", endTime = "12:00", subjectId = englishId, topic = "Grammar: Reported Speech & Active/Passive", slotType = "PRACTICE"),
                TimetableSlotEntity(dayOfWeek = 5, startTime = "14:00", endTime = "16:00", subjectId = physicsId, topic = "Weekly Mock Test & Evaluation", slotType = "REVISION")
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
                    ),
                    NoteEntity(
                        title = "Chemistry: Oxidation Rules & Tips",
                        content = "• Free element oxidation state is always 0\n• Fluorine is always -1 in compounds\n• Oxygen is usually -2 (except in peroxides where it is -1, and OF2 where it is +2)\n• Group 1 alkali metals are always +1, Group 2 alkaline earths are always +2",
                        subjectId = chemistryId,
                        isPinned = false
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
                    ),
                    ExamEntity(
                        title = "Biology Unit Test",
                        subjectId = biologyId,
                        examDateMillis = now + (7 * oneDay),
                        targetScore = "100%",
                        notes = "Chapters 1, 2, and 3 taxonomy diagrams."
                    )
                )
            )
        }
    }
}
