package com.synesthesia.mobile

import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * One process-wide ExoPlayer instance is shared by the Compose UI and MediaSessionService.
 * Media3 publishes the standard playback notification and exposes transport controls to
 * Android's lock screen, Bluetooth devices, headsets and other media controllers.
 */
object PlaybackPlayer {
    @Volatile private var instance: ExoPlayer? = null

    fun get(context: Context): ExoPlayer = instance ?: synchronized(this) {
        instance ?: ExoPlayer.Builder(context.applicationContext).build().also { player ->
            player.setAudioAttributes(AudioAttributes.DEFAULT, true)
            instance = player
        }
    }

    fun release() {
        synchronized(this) {
            instance?.release()
            instance = null
        }
    }
}

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = PlaybackPlayer.get(this)
        val openPlayerIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_PLAYER, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            4102,
            openPlayerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    // If the user explicitly dismisses the app from Recents, stop playback. The last
    // media ID is persisted by the UI and will be restored as a paused selection next launch.
    override fun onTaskRemoved(rootIntent: Intent?) {
        PlaybackPlayer.get(this).pause()
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        PlaybackPlayer.release()
        super.onDestroy()
    }
}
