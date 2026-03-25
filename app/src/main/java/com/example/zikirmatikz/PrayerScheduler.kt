package com.example.zikirmatikz

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object PrayerScheduler {

    private val prayerNames = listOf("Sabah", "Öğle", "İkindi", "Akşam", "Yatsı")

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences("zikirmatik", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("bildirim_aktif", false)) return

        val lat = prefs.getFloat("lat", 0f).toDouble()
        val lng = prefs.getFloat("lng", 0f).toDouble()
        if (lat == 0.0 && lng == 0.0) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        scheduleForDay(context, alarmManager, lat, lng, Calendar.getInstance())
        scheduleForDay(context, alarmManager, lat, lng, Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        })
    }

    private fun scheduleForDay(
        context: Context,
        alarmManager: AlarmManager,
        lat: Double,
        lng: Double,
        cal: Calendar
    ) {
        val times = PrayerTimeCalculator.calculate(lat, lng, cal)
        val prayerMillis = listOf(times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha)

        val now = System.currentTimeMillis()
        val dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)

        prayerMillis.forEachIndexed { index, timeMillis ->
            val notifTime = timeMillis - 5 * 60 * 1000L
            if (notifTime > now) {
                val intent = Intent(context, EzanReceiver::class.java).apply {
                    putExtra("prayer_name", prayerNames[index])
                }
                val requestCode = dayKey * 10 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, notifTime, pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, notifTime, pendingIntent)
                }
            }
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (dayOffset in 0..1) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
            val dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
            for (i in 0..4) {
                val intent = Intent(context, EzanReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    dayKey * 10 + i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent?.let { alarmManager.cancel(it) }
            }
        }
    }
}
