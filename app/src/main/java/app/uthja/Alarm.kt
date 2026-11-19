package app.uthja

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** days uses java.util.Calendar numbering: 1 = Sunday ... 7 = Saturday. Empty = rings once. */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Set<Int> = emptySet(),
    val enabled: Boolean = true,
) {
    fun timeText(): String =
        LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("h:mm a"))

    fun daysText(): String {
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        if (days == setOf(2, 3, 4, 5, 6)) return "Weekdays"
        val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return (1..7).filter { it in days }.joinToString(" ") { names[it - 1] }
    }
}

object AlarmStore {
    private const val PREFS = "uthja"
    private const val KEY = "alarms"

    fun all(ctx: Context): List<Alarm> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val d = o.optJSONArray("days") ?: JSONArray()
            Alarm(
                id = o.getInt("id"),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                days = (0 until d.length()).map { d.getInt(it) }.toSet(),
                enabled = o.optBoolean("enabled", true),
            )
        }.sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    private fun saveAll(ctx: Context, list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("days", JSONArray(a.days.toList()))
                put("enabled", a.enabled)
            })
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun get(ctx: Context, id: Int): Alarm? = all(ctx).find { it.id == id }
    fun upsert(ctx: Context, alarm: Alarm) = saveAll(ctx, all(ctx).filter { it.id != alarm.id } + alarm)
    fun delete(ctx: Context, id: Int) = saveAll(ctx, all(ctx).filter { it.id != id })
    fun newId(ctx: Context): Int = (all(ctx).maxOfOrNull { it.id } ?: 0) + 1
}
