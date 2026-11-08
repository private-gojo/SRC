package app.one.secondvpnlite.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.one.secondvpnlite.R
import app.one.secondvpnlite.logs.AppLogManager

class NotificationService : Service() {
    private var title: String? = null
    private var body: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.extras != null) {
            title = intent.getStringExtra("TITLE")
            body = intent.getStringExtra("BODY")
            val isConnected = intent.getBooleanExtra("IS_CONNECTED", false)
            if (isConnected) {
                ConnectedNotification()
            } else {
                StartingNotification()
            }
        }
        return START_STICKY
    }

    fun StartingNotification() {
        try {
            if (notifManagerStarting == null) {
                notifManagerStarting =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            }

            val bigText = NotificationCompat.BigTextStyle()
            bigText.setBigContentTitle(title)

            val builder = NotificationCompat.Builder(this, NOTIFY_STARTING.toString())
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }

            bigText.bigText(body)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
            builder.setSmallIcon(R.drawable.ic_notification)
            builder.setContentTitle(title)
            builder.setContentText(body)
            builder.setPriority(Notification.PRIORITY_LOW)
            builder.setStyle(bigText)
            builder.setOnlyAlertOnce(true)
            builder.setSound(null)
            builder.setOngoing(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = NOTIFY_STARTING.toString()
                val channel = NotificationChannel(
                    channelId,
                    title,
                    NotificationManager.IMPORTANCE_LOW
                )
                notifManagerStarting!!.createNotificationChannel(channel)
                channel.setSound(null, null)
                builder.setChannelId(channelId)
            }

            val notification = builder.build()
            notifManagerStarting!!.notify(NOTIFY_STARTING, notification)

            try {
                startForeground(NOTIFY_STARTING, notification)
            } catch (error: Exception) {
                AppLogManager.addLog("Notification error: $error")
                error.printStackTrace()
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    fun ConnectedNotification() {
        try {
            try {
                notifManagerStarting!!.cancelAll()
            } catch (error: Exception) {
                error.printStackTrace()
            }

            val name = "SecondVPN - Lite"
            val id = "SecondVPN Notification"
            val description = "SecondVPN Notification Service"
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }

            if (notifManager == null) {
                notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            }

            val builder: NotificationCompat.Builder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                var channel = notifManager!!.getNotificationChannel(id)
                if (channel == null) {
                    channel = NotificationChannel(id, name, importance)
                    channel.description = description
                    channel.enableVibration(true)
                    channel.lightColor = Color.GREEN
                    channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                    notifManager!!.createNotificationChannel(channel)
                }

                builder = NotificationCompat.Builder(this, id)
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentText(body)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(false)
                    .setContentIntent(pendingIntent)
                    .setTicker(title)
                    .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
            } else {
                builder = NotificationCompat.Builder(this)
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentText(body)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(title)
                    .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
                    .setPriority(Notification.PRIORITY_HIGH)
            }

            val notification = builder.build()
            notifManager!!.notify(NOTIFY_CONNECTED, notification)
            try {
                startForeground(NOTIFY_CONNECTED, notification)
            } catch (error: Exception) {
                AppLogManager.addLog("Notification error: $error")
                error.printStackTrace()
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopNotification()
        stopSelf()
        super.onDestroy()
    }

    companion object {
        const val NOTIFY_STARTING = 100260
        const val NOTIFY_CONNECTED = 100261

        @JvmField
        var notifManager: NotificationManager? = null

        @JvmField
        var notifManagerStarting: NotificationManager? = null

        @JvmStatic
        fun stopNotification() {
            try {
                notifManager!!.cancelAll()
                notifManagerStarting!!.cancelAll()
            } catch (error: Exception) {
                error.printStackTrace()
            }
        }
    }
}
