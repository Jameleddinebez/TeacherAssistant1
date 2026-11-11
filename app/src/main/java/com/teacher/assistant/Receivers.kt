package com.teacher.assistant

import android.app.*
import android.content.*
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("wake", "منبه العمل", NotificationManager.IMPORTANCE_HIGH))
        val full = PendingIntent.getActivity(c, 1, Intent(c, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, "wake")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان وقت الاستيقاظ").setContentText("لديك حصة اليوم")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(full, true).setContentIntent(full).setAutoCancel(true).build()
        nm.notify(1, n)
        // أعد جدولة الأيام القادمة
        val p = goAsync()
        Thread { try { Scheduler.scheduleAll(c) } finally { p.finish() } }.start()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val p = goAsync()
        Thread { try { Scheduler.scheduleAll(c) } finally { p.finish() } }.start()
    }
}
