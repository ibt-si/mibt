package org.interferolog.fibt.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.interferolog.fibt.FibtApplication
import org.interferolog.fibt.MainActivity
import org.interferolog.fibt.content.POINT_C
import org.interferolog.fibt.data.FibtDatabase
import java.time.ZonedDateTime

class PointReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                show(context)
                if (minute >= 0) ReminderScheduler.scheduleMinute(context, minute)
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
            val notification = NotificationCompat.Builder(context, FibtApplication.CHANNEL_POINT_C)
                .setSmallIcon(org.interferolog.fibt.R.drawable.ic_notification)
                .setContentTitle("Точка C")
                .setContentText(POINT_C)
                .setStyle(NotificationCompat.BigTextStyle().bigText(POINT_C))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
            NotificationManagerCompat.from(context).notify(MINUTE_NOTIFICATION, notification)
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
        val plan = kotlinx.coroutines.runBlocking { FibtDatabase.get(context).journal().reminder() }
        if (plan?.enabled == false) return
        val minutes = plan?.let { listOf(it.minute1, it.minute2, it.minute3) }
            ?: listOf(9 * 60, 14 * 60, 20 * 60)
        minutes.distinct().forEach { scheduleMinute(context, it) }
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
