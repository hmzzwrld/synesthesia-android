package com.synesthesia.mobile

import android.content.Context
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
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        PlaybackPlayer.release()
        super.onDestroy()
    }
}
