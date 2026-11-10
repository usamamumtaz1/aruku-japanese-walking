package com.japanesewalking.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.japanesewalking.app.data.db.WorkoutDatabase
import com.japanesewalking.app.data.repository.WorkoutRepository

class JapaneseWalkingApp : Application() {

    lateinit var database: WorkoutDatabase
        private set

    lateinit var repository: WorkoutRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = WorkoutDatabase.getDatabase(this)
        repository = WorkoutRepository(database.workoutDao())

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_desc)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "japanese_walking_workout_v2"
        const val NOTIFICATION_ID = 1001

        lateinit var instance: JapaneseWalkingApp
            private set
    }
}
