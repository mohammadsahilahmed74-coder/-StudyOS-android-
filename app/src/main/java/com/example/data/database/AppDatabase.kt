package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ChapterDao
import com.example.data.dao.ExamDao
import com.example.data.dao.NoteDao
import com.example.data.dao.StudyTaskDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TimetableDao
import com.example.data.model.ChapterEntity
import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.StudyTask
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableSlotEntity
import kotlinx.coroutines.CoroutineScope

/**
 * Main AppDatabase class defining Room database configurations and DAOs.
 */
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StudyTask::class,
        SubjectEntity::class,
        ChapterEntity::class,
        TimetableSlotEntity::class,
        NoteEntity::class,
        ExamEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun taskDao(): TaskDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun timetableDao(): TimetableDao
    abstract fun noteDao(): NoteDao
    abstract fun examDao(): ExamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE notes ADD COLUMN chapter TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE notes ADD COLUMN category TEXT NOT NULL DEFAULT 'Lecture Notes'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE notes ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT '[]'")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_os_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
