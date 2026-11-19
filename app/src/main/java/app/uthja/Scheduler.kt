package app.uthja

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.Calendar

object Scheduler {
    const val EXTRA_ID = "alarm_id"
    const val TEST_ID = 0  // real alarms start at 1

    fun nextTrigger(alarm: Alarm, now: Calendar = Calendar.getInstance()): Long {
        val c = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!c.after(now)) c.add(Calendar.DAY_OF_YEAR, 1)
        if (alarm.days.isNotEmpty()) {
            while (c.get(Calendar.DAY_OF_WEEK) !in alarm.days) c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return c.timeInMillis
    }

    private fun pending(ctx: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, id,
            Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun scheduleAt(ctx: Context, id: Int, timeMillis: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, id)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi)
            return
        }
        val show = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        // setAlarmClock = exact, fires even in Doze, and shows the alarm icon in the status bar.
        am.setAlarmClock(AlarmManager.AlarmClockInfo(timeMillis, show), pi)
    }

    fun schedule(ctx: Context, alarm: Alarm) {
        if (alarm.enabled) scheduleAt(ctx, alarm.id, nextTrigger(alarm)) else cancel(ctx, alarm.id)
    }

    fun cancel(ctx: Context, id: Int) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, id))
    }

    fun rescheduleAll(ctx: Context) = AlarmStore.all(ctx).forEach { schedule(ctx, it) }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(Scheduler.EXTRA_ID, -1)
        val alarm = AlarmStore.get(ctx, id)
        if (alarm == null && id != Scheduler.TEST_ID) return  // deleted alarm
        if (alarm != null) {
            // Line up the next ring right away (or switch off a one-time alarm).
            if (alarm.days.isEmpty()) AlarmStore.upsert(ctx, alarm.copy(enabled = false))
            else Scheduler.schedule(ctx, alarm)
        }
        ContextCompat.startForegroundService(
            ctx, Intent(ctx, AlarmService::class.java).putExtra(Scheduler.EXTRA_ID, id)
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = Scheduler.rescheduleAll(ctx)
}
