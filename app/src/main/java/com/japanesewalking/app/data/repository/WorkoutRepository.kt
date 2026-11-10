package com.japanesewalking.app.data.repository

import com.japanesewalking.app.data.db.WorkoutDao
import com.japanesewalking.app.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    val allSessions: Flow<List<WorkoutSession>> = workoutDao.getAllSessions()
    val latestSession: Flow<WorkoutSession?> = workoutDao.getLatestSession()
    val totalSessionsCount: Flow<Int> = workoutDao.getTotalSessionsCount()
    val totalDurationSeconds: Flow<Long?> = workoutDao.getTotalDurationSeconds()
    val totalDistanceMeters: Flow<Double?> = workoutDao.getTotalDistanceMeters()

    suspend fun saveSession(session: WorkoutSession): Long {
        return workoutDao.insertSession(session)
    }

    suspend fun getSessionById(id: Long): WorkoutSession? {
        return workoutDao.getSessionById(id)
    }

    suspend fun deleteSession(id: Long) {
        workoutDao.deleteSession(id)
    }
}
