// Copyright (C) 2026 Vadim N. Cherashev
// SPDX-License-Identifier: GPL-3.0-or-later

package org.interferolog.mibt.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.interferolog.mibt.MibtApplication
import org.interferolog.mibt.MainActivity
import org.interferolog.mibt.content.POINT_C
import org.interferolog.mibt.data.MibtDatabase
import java.time.ZonedDateTime

/** POST_NOTIFICATIONS is only a runtime permission from Android 13 onwards. */
fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

class PointReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // A plan disabled after the alarm was scheduled must not notify,
                // and its alarm is left to expire instead of rescheduling itself.
                if (MibtDatabase.get(context).journal().reminder()?.enabled == true) {
                    show(context)
                    if (minute >= 0) ReminderScheduler.scheduleMinute(context, minute)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_MINUTE = "minute"

        fun show(context: Context) {
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).putExtra("open", "pointc")
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, MibtApplication.CHANNEL_POINT_C)
                .setSmallIcon(org.interferolog.mibt.R.drawable.ic_notification)
                .setContentTitle("Точка C")
                .setContentText(POINT_C)
                .setStyle(NotificationCompat.BigTextStyle().bigText(POINT_C))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            if (!hasNotificationPermission(context)) return
            try {
                NotificationManagerCompat.from(context).notify(MINUTE_NOTIFICATION, notification)
            } catch (_: SecurityException) {
                // The permission may have been revoked in Settings after the check.
            }
        }

        private const val MINUTE_NOTIFICATION = 42
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

object ReminderScheduler {
    fun reschedule(context: Context) {
        // Reminders are off until the user turns them on and saves a plan.
        val plan = kotlinx.coroutines.runBlocking { MibtDatabase.get(context).journal().reminder() }
        if (plan?.enabled != true) return
        listOf(plan.minute1, plan.minute2, plan.minute3).distinct().forEach {
            scheduleMinute(context, it)
        }
    }

    fun scheduleMinute(context: Context, minuteOfDay: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val trigger = next(minuteOfDay)
        val pending = pending(context, minuteOfDay)
        alarm.setWindow(AlarmManager.RTC, trigger, WINDOW, pending)
    }

    private fun next(minuteOfDay: Int): Long {
        val now = ZonedDateTime.now()
        var trigger = now.toLocalDate().atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(now.zone)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        return trigger.toInstant().toEpochMilli()
    }

    private fun pending(context: Context, minuteOfDay: Int): PendingIntent {
        val intent = Intent(context, PointReminderReceiver::class.java).putExtra(PointReminderReceiver.EXTRA_MINUTE, minuteOfDay)
        return PendingIntent.getBroadcast(
            context,
            minuteOfDay,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val WINDOW = 15 * 60 * 1000L
}
