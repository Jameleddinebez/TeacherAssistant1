package com.teacher.assistant

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.time.*
import java.time.format.TextStyle
import java.util.Locale

val AR = Locale("ar")

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val ctx = applicationContext
        CoroutineScope(Dispatchers.IO).launch { Scheduler.scheduleAll(ctx) }
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                    Surface(Modifier.fillMaxSize()) { Home() }
                }
            }
        }
    }
}

@Composable fun Home() {
    val c = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val dao = remember { AppDb.get(c).dao() }
    val lessons by dao.lessonsFlow().collectAsState(emptyList())
    val holidays by dao.holidaysFlow().collectAsState(emptyList())
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var on by remember { mutableStateOf(Scheduler.enabled(c)) }
    var off by remember { mutableStateOf(Scheduler.offset(c)) }
    var next by remember { mutableStateOf<LocalDateTime?>(null) }
    val scope = rememberCoroutineScope()
    fun refresh() { scope.launch(Dispatchers.IO) { Scheduler.scheduleAll(c); next = Scheduler.nextWake(c) } }
    LaunchedEffect(Unit) { refresh(); while (true) { now = LocalDateTime.now(); delay(1000) } }
    LaunchedEffect(lessons, holidays) { if (lessons.isNotEmpty()) refresh() }

    val today = now.toLocalDate()
    val status = when {
        today.dayOfWeek in Scheduler.weekend -> "راحة (نهاية الأسبوع)"
        Scheduler.isHoliday(today, holidays) -> "عطلة"
        lessons.none { it.day == today.dayOfWeek.value } -> "راحة (لا حصص)"
        else -> "يوم عمل"
    }
    val todays = if (status == "يوم عمل") lessons.filter { it.day == today.dayOfWeek.value }.sortedBy { it.start } else emptyList()
    val nextLesson = todays.firstOrNull { today.atTime(LocalTime.parse(it.start)).isAfter(now) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("%s %s".format(today.dayOfWeek.getDisplayName(TextStyle.FULL, AR), today), style = MaterialTheme.typography.titleLarge)
            Text("%02d:%02d:%02d".format(now.hour, now.minute, now.second), style = MaterialTheme.typography.displayMedium)
            Text("حالة اليوم: $status")
        }
        item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text("المنبه القادم: " + (next?.let { "${it.toLocalDate()} ${it.toLocalTime().withSecond(0)}" } ?: "لا يوجد"))
                if (nextLesson != null) {
                    val m = Duration.between(now, today.atTime(LocalTime.parse(nextLesson.start))).toMinutes()
                    Text("الحصة القادمة: ${nextLesson.className} - الحجرة ${nextLesson.room} بعد ${m / 60} س و${m % 60} د")
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("تفعيل منبهات العمل"); Switch(on, { on = it; Scheduler.setEnabled(c, it); refresh() })
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    OutlinedButton({ if (off > 60) { off -= 15; Scheduler.setOffset(c, off); refresh() } }) { Text("-") }
                    Text("قبل أول حصة بـ $off دقيقة")
                    OutlinedButton({ off += 15; Scheduler.setOffset(c, off); refresh() }) { Text("+") }
                }
            } }
        }
        item { Text("حصص اليوم", style = MaterialTheme.typography.titleMedium) }
        if (todays.isEmpty()) item { Text("لا حصص اليوم") }
        items(todays) { Text("${it.start}-${it.end}  |  ${it.className}  |  الحجرة ${it.room}") }
        item { Text("الجدول الأسبوعي", style = MaterialTheme.typography.titleMedium) }
        for (d in listOf(7, 1, 2, 3, 4)) {
            val day = DayOfWeek.of(d)
            item { Text(day.getDisplayName(TextStyle.FULL, AR), style = MaterialTheme.typography.titleSmall) }
            val dl = lessons.filter { it.day == d }.sortedBy { it.start }
            if (dl.isEmpty()) item { Text("—") }
            items(dl) { Text("${it.start}-${it.end}  ${it.className}  (${it.room})") }
        }
        item { Text("العطل", style = MaterialTheme.typography.titleMedium) }
        items(holidays) { Text("${it.name}: ${it.startDate} → ${it.endDate}") }
    }
}
