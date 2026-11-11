package com.teacher.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.*

object Scheduler {
    const val DAYS_AHEAD = 21
    val weekend = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

    fun offset(c: Context) = c.getSharedPreferences("p", 0).getInt("offset", 60)
    fun setOffset(c: Context, m: Int) = c.getSharedPreferences("p", 0).edit().putInt("offset", m).apply()
    fun enabled(c: Context) = c.getSharedPreferences("p", 0).getBoolean("on", true)
    fun setEnabled(c: Context, b: Boolean) = c.getSharedPreferences("p", 0).edit().putBoolean("on", b).apply()

    fun isHoliday(d: LocalDate, hs: List<Holiday>) =
        hs.any { !d.isBefore(LocalDate.parse(it.startDate)) && !d.isAfter(LocalDate.parse(it.endDate)) }

    fun wakeTime(d: LocalDate, ls: List<Lesson>, hs: List<Holiday>, off: Int): LocalDateTime? {
        if (d.dayOfWeek in weekend || isHoliday(d, hs)) return null
        val first = ls.filter { it.day == d.dayOfWeek.value }.minByOrNull { it.start } ?: return null
        return d.atTime(LocalTime.parse(first.start)).minusMinutes(off.toLong())
    }

    private fun pi(c: Context, d: LocalDate, flag: Int): PendingIntent? {
        val i = Intent(c, AlarmReceiver::class.java).putExtra("date", d.toString())
        return PendingIntent.getBroadcast(c, d.toEpochDay().toInt(), i, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    /** يُستدعى من الواجهة ومن إعادة التشغيل ومن كل منبه: يعيد بناء منبهات الأيام القادمة. */
    fun scheduleAll(c: Context) {
        val db = AppDb.get(c).dao()
        val ls = db.lessons(); val hs = db.holidays()
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now(); val off = offset(c)
        for (n in 0..DAYS_AHEAD) {
            val d = LocalDate.now().plusDays(n.toLong())
            val t = if (enabled(c)) wakeTime(d, ls, hs, off) else null
            if (t != null && t.isAfter(now)) {
                val ms = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
                am.setAlarmClock(AlarmManager.AlarmClockInfo(ms, show), pi(c, d, PendingIntent.FLAG_UPDATE_CURRENT)!!)
            } else {
                pi(c, d, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it); it.cancel() }
            }
        }
    }

    fun nextWake(c: Context): LocalDateTime? {
        if (!enabled(c)) return null
        val db = AppDb.get(c).dao(); val ls = db.lessons(); val hs = db.holidays()
        val now = LocalDateTime.now()
        return (0..DAYS_AHEAD).mapNotNull { wakeTime(LocalDate.now().plusDays(it.toLong()), ls, hs, offset(c)) }
            .firstOrNull { it.isAfter(now) }
    }
}
