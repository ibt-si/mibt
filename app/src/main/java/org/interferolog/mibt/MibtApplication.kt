package org.interferolog.mibt

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.interferolog.mibt.reminder.ReminderScheduler

class MibtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_POINT_C, "Точка C", NotificationManager.IMPORTANCE_LOW)
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.description = "Три бесшумных напоминания в день"
        manager.createNotificationChannel(channel)
        ReminderScheduler.reschedule(this)
    }

    companion object {
        const val CHANNEL_POINT_C = "point-c"
    }
}
