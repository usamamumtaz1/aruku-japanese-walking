package com.japanesewalking.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.japanesewalking.app.MainActivity
import com.japanesewalking.app.R
import com.japanesewalking.app.data.model.IntervalType
import com.japanesewalking.app.data.model.LiveWorkoutState
import com.japanesewalking.app.data.model.SessionStatus
import com.japanesewalking.app.data.model.WorkoutGoal
import com.japanesewalking.app.service.WorkoutForegroundService
import java.util.Locale

class WorkoutAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId, lastKnownState)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_WIDGET_START -> {
                // Quick start standard 30-min Japanese walk (3 min fast / 3 min moderate, 5 sets)
                WorkoutForegroundService.startWorkout(
                    context = context,
                    fastSeconds = 180,
                    normalSeconds = 180,
                    goal = WorkoutGoal()
                )
                // Open app to live workout screen
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(appIntent)
            }

            ACTION_WIDGET_PAUSE_RESUME -> {
                if (lastKnownState?.status == SessionStatus.RUNNING) {
                    WorkoutForegroundService.pauseWorkout(context)
                } else if (lastKnownState?.status == SessionStatus.PAUSED) {
                    WorkoutForegroundService.resumeWorkout(context)
                }
            }

            ACTION_WIDGET_STOP -> {
                WorkoutForegroundService.stopWorkout(context)
                lastKnownState = null
                pushUpdateToAll(context, null)
            }

            ACTION_UPDATE_WIDGET -> {
                pushUpdateToAll(context, lastKnownState)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_START = "com.japanesewalking.app.WIDGET_START"
        const val ACTION_WIDGET_PAUSE_RESUME = "com.japanesewalking.app.WIDGET_PAUSE_RESUME"
        const val ACTION_WIDGET_STOP = "com.japanesewalking.app.WIDGET_STOP"
        const val ACTION_UPDATE_WIDGET = "com.japanesewalking.app.ACTION_UPDATE_WIDGET"

        @Volatile
        var lastKnownState: LiveWorkoutState? = null

        fun notifyStateChanged(context: Context, state: LiveWorkoutState) {
            lastKnownState = state
            pushUpdateToAll(context, state)
        }

        fun pushUpdateToAll(context: Context, state: LiveWorkoutState?) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, WorkoutAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            for (id in appWidgetIds) {
                updateWidgetView(context, appWidgetManager, id, state)
            }
        }

        private fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            state: LiveWorkoutState?
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_workout_2x2)

            // Click root or header to launch MainActivity
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

            val isActive = state != null && (state.status == SessionStatus.RUNNING || state.status == SessionStatus.PAUSED)

            if (isActive && state != null) {
                views.setViewVisibility(R.id.view_idle, View.GONE)
                views.setViewVisibility(R.id.view_active, View.VISIBLE)

                val intervalName = if (state.currentInterval == IntervalType.FAST) {
                    "⚡ FAST WALK"
                } else {
                    "🍃 MODERATE WALK"
                }
                val intervalColor = if (state.currentInterval == IntervalType.FAST) {
                    0xFFFF7043.toInt() // Vermilion / Orange
                } else {
                    0xFF8FF7BD.toInt() // Mint Green
                }
                views.setTextViewText(R.id.tv_active_interval, intervalName)
                views.setTextColor(R.id.tv_active_interval, intervalColor)

                val remainingSec = state.intervalRemainingSeconds
                val timerText = String.format(Locale.getDefault(), "%02d:%02d", remainingSec / 60, remainingSec % 60)
                views.setTextViewText(R.id.tv_active_timer, timerText)

                val km = state.distanceMeters / 1000.0
                val detailText = "Set ${state.completedSets + 1} • ${String.format(Locale.getDefault(), "%.1f km", km)}"
                views.setTextViewText(R.id.tv_active_details, detailText)

                val isPaused = state.status == SessionStatus.PAUSED
                val pauseLabel = if (isPaused) "Resume" else "Pause"
                val pauseIcon = if (isPaused) R.drawable.ic_play_white else R.drawable.ic_pause_white

                views.setTextViewText(R.id.tv_widget_pause_label, pauseLabel)
                views.setImageViewResource(R.id.img_widget_pause_icon, pauseIcon)

                // Pause / Resume PendingIntent
                val pauseIntent = Intent(context, WorkoutAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_PAUSE_RESUME
                }
                val pausePendingIntent = PendingIntent.getBroadcast(
                    context,
                    101,
                    pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_pause, pausePendingIntent)

                // Stop PendingIntent
                val stopIntent = Intent(context, WorkoutAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_STOP
                }
                val stopPendingIntent = PendingIntent.getBroadcast(
                    context,
                    102,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_stop, stopPendingIntent)

            } else {
                views.setViewVisibility(R.id.view_idle, View.VISIBLE)
                views.setViewVisibility(R.id.view_active, View.GONE)

                // Start Walk PendingIntent
                val startIntent = Intent(context, WorkoutAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_START
                }
                val startPendingIntent = PendingIntent.getBroadcast(
                    context,
                    100,
                    startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_widget_start_walk, startPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
