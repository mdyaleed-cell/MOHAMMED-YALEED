package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class BillReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "বিল পরিশোধ রিমাইন্ডার"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "আপনার মাসিক বিল পরিশোধের সময় হয়েছে।"
        val notifId = intent.getIntExtra(EXTRA_ID, 1001)
        BillReminderScheduler.showNotification(context, notifId, title, body)
    }

    companion object {
        const val EXTRA_TITLE = "extra_bill_title"
        const val EXTRA_BODY = "extra_bill_body"
        const val EXTRA_ID = "extra_bill_id"
    }
}

object BillReminderScheduler {
    private const val CHANNEL_ID = "hisab_khata_bill_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "বিল ও বাজেট রিমাইন্ডার",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "মাসিক বিল পরিশোধ এবং বাজেট সতর্কতার নোটিফিকেশন"
            }
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showNotification(context: Context, id: Int, title: String, body: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hisab)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    fun scheduleMonthlyBillReminder(
        context: Context,
        billId: String,
        title: String,
        amount: Double,
        dueDayOfMonth: Int
    ) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            putExtra(BillReminderReceiver.EXTRA_TITLE, "বিল রিমাইন্ডার: $title")
            putExtra(
                BillReminderReceiver.EXTRA_BODY,
                "প্রতি মাসের $dueDayOfMonth তারিখে ${Formatters.formatTaka(amount)} বিল পরিশোধ করতে হবে।"
            )
            putExtra(BillReminderReceiver.EXTRA_ID, billId.hashCode())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            billId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, dueDayOfMonth.coerceIn(1, getActualMaximum(java.util.Calendar.DAY_OF_MONTH)))
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.MONTH, 1)
            }
        }
        alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
    }
}
