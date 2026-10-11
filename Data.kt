package com.ecetprep.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Subject(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)
@Entity data class Topic(@PrimaryKey(autoGenerate = true) val id: Long = 0, val subjectId: Long,
                         val name: String, val completed: Boolean = false)

@Dao interface PrepDao {
    @Query("SELECT * FROM Subject ORDER BY id") fun subjects(): Flow<List<Subject>>
    @Query("SELECT * FROM Topic ORDER BY id") fun topics(): Flow<List<Topic>>
    @Query("SELECT COUNT(*) FROM Subject") suspend fun subjectCount(): Int
    @Insert suspend fun insertSubject(s: Subject): Long
    @Insert suspend fun insertTopic(t: Topic): Long
    @Update suspend fun update(t: Topic)
    @Delete suspend fun delete(t: Topic)
}

@Database(entities = [Subject::class, Topic::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): PrepDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "prep.db").build().also { inst = it }
        }
    }
}
