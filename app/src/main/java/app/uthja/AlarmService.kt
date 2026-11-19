package app.uthja

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Rings at full volume until RingActivity says every challenge is solved. */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    // Turned the volume down? Nice try — it goes back up every 1.5 s.
    private val keepLoud = object : Runnable {
        override fun run() {
            maxVolume()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(Scheduler.EXTRA_ID, currentId) ?: currentId
        currentId = id
        isRinging = true

        val notification = buildNotification(id)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        if (player == null) startRinging()
        try { startActivity(ringIntent(this, id)) } catch (_: Exception) { /* full-screen intent covers it */ }

        // If the system kills us, come back and keep ringing.
        return START_STICKY
    }

    private fun buildNotification(id: Int): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL, "Ringing alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)  // we play the sound ourselves
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)

        val pi = PendingIntent.getActivity(
            this, 1, ringIntent(this, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Uth Ja! ⏰")
            .setContentText("Solve the challenges to stop the alarm")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
    }

    private fun startRinging() {
        maxVolume()
        handler.post(keepLoud)

        val uris = listOf(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        for (uri in uris) {
            if (uri == null) continue
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmService, uri)
                    isLooping = true
                    setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                    prepare()
                    start()
                }
                break
            } catch (_: Exception) {
                player?.release()
                player = null
            }
        }

        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400), 0))
    }

    private fun maxVolume() {
        try {
            val am = getSystemService(AudioManager::class.java)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        } catch (_: Exception) { /* Do Not Disturb can block this */ }
    }

    override fun onDestroy() {
        handler.removeCallbacks(keepLoud)
        player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        player = null
        vibrator?.cancel()
        isRinging = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "ringing"
        private const val NOTIF_ID = 42

        @Volatile var isRinging = false
        @Volatile var currentId = -1

        fun ringIntent(ctx: Context, id: Int): Intent =
            Intent(ctx, RingActivity::class.java)
                .putExtra(Scheduler.EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

        /** Only RingActivity calls this, after the last challenge is solved. */
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AlarmService::class.java))
        }
    }
}
