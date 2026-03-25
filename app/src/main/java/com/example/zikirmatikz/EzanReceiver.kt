package com.example.zikirmatikz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class EzanReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "Namaz"
        showNotification(context, prayerName)
        // Sonraki günün vakitlerini planla
        PrayerScheduler.schedule(context)
    }

    private fun showNotification(context: Context, prayerName: String) {
        val channelId = "ezan_bildirimi"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ezan Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Namaz vakitlerinden 5 dakika önce hatırlatma"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Namaz Vakti Yaklaşıyor")
            .setContentText("$prayerName vakti 5 dakika sonra")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }
}
