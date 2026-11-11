package com.teacher.assistant

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val day: Int,            // java.time.DayOfWeek: 1=الاثنين ... 7=الأحد
    val start: String,       // "08:00"
    val end: String,
    val className: String,   // مثال: 1م2 = السنة الأولى متوسط، القسم 2
    val room: String
)

@Entity data class Holiday(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: String,   // أول يوم راحة (ISO)
    val endDate: String,     // آخر يوم راحة (ISO)
    val name: String
)

@Dao interface AppDao {
    @Query("SELECT * FROM Lesson ORDER BY day, start") fun lessonsFlow(): Flow<List<Lesson>>
    @Query("SELECT * FROM Lesson") fun lessons(): List<Lesson>
    @Query("SELECT * FROM Holiday") fun holidays(): List<Holiday>
    @Query("SELECT COUNT(*) FROM Lesson") fun lessonCount(): Int
    @Query("SELECT COUNT(*) FROM Holiday") fun holidayCount(): Int
    @Insert fun insertLessons(l: List<Lesson>)
    @Insert fun insertHolidays(h: List<Holiday>)
    @Insert fun insertHoliday(h: Holiday)
    @Delete fun deleteHoliday(h: Holiday)
    @Query("SELECT * FROM Holiday ORDER BY startDate") fun holidaysFlow(): Flow<List<Holiday>>
}

@Database(entities = [Lesson::class, Holiday::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "teacher.db")
                .build().also { inst = it; seed(it.dao()) }
        }
        // بيانات أولية من جدول الحصص والرزنامة الرسمية 2026-2027 (قابلة للتعديل لاحقاً)
        private fun seed(d: AppDao) {
            if (d.lessonCount() == 0) {
                fun l(day: Int, s: String, e: String, c: String, r: String) = Lesson(day = day, start = s, end = e, className = c, room = r)
                d.insertLessons(listOf(
                    l(7,"08:00","09:00","1م2","16"), l(7,"09:00","10:00","2م4","2"), l(7,"10:00","11:00","3م5","12"),
                    l(7,"13:30","14:30","3م4","3"), l(7,"14:30","15:30","1م1","13"),
                    l(1,"08:00","09:00","2م3","18"), l(1,"09:00","10:00","3م3","10"), l(1,"10:00","11:00","3م1","5"),
                    l(1,"11:00","12:00","1م2","14"), l(1,"13:30","14:30","4م1","1"), l(1,"14:30","15:30","2م5","8"),
                    l(3,"08:00","09:00","3م4","11"), l(3,"09:00","10:00","4م4","4"),
                    l(4,"08:00","09:00","2م6","9"), l(4,"09:00","10:00","1م4","19"), l(4,"10:00","11:00","2م2","17"),
                    l(4,"11:00","12:00","2م4","7"), l(4,"13:30","14:30","3م1","15"), l(4,"14:30","15:30","2م3","6")
                ))
            }
            if (d.holidayCount() == 0) {
                d.insertHolidays(listOf(
                    Holiday(startDate = "2026-10-28", endDate = "2026-11-01", name = "عطلة الخريف"),
                    Holiday(startDate = "2026-12-18", endDate = "2027-01-02", name = "عطلة الشتاء"),
                    Holiday(startDate = "2027-03-19", endDate = "2027-04-03", name = "عطلة الربيع"),
                    Holiday(startDate = "2027-07-09", endDate = "2027-09-30", name = "العطلة السنوية")
                ))
            }
        }
    }
}
