package com.japanesewalking.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.japanesewalking.app.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC LIMIT 1")
    fun getLatestSession(): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSession?

    @Query("SELECT COUNT(*) FROM workout_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT SUM(durationSeconds) FROM workout_sessions")
    fun getTotalDurationSeconds(): Flow<Long?>

    @Query("SELECT SUM(distanceMeters) FROM workout_sessions")
    fun getTotalDistanceMeters(): Flow<Double?>

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}
