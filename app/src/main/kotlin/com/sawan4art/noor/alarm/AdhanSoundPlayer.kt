package com.sawan4art.noor.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.sawan4art.noor.domain.adhan.AdhanSoundKind
import com.sawan4art.noor.util.VibrationRinger
import com.sawan4art.noor.domain.adhan.AdhanVoices
import java.io.File

/**
 * مشغّل المعاينة في صفحة الإعدادات — نفس مصادر ومجرى صوت الأذان
 * الحقيقي (USAGE_ALARM) فيكون ما تسمعه في المعاينة هو عين ما
 * يسمعه وقت الأذان.
 */
object AdhanSoundPlayer {

    private var player: MediaPlayer? = null

    /** سياق المعاينة الحالية — لإيقاف الاهتزاز في stop() بلا معامل (بلاغ v1.71) */
    private var playingContext: Context? = null

    val isPlaying: Boolean
        get() = runCatching { player?.isPlaying == true }.getOrDefault(false)

    /** تشغيل معاينة — يوقف أي معاينة سابقة أولاً */
    fun start(
        context: Context,
        kind: AdhanSoundKind,
        soundId: String,
        customPath: String,
        /** اهتزازٌ مواكبٌ للمعاينة إن كان مفتاح هذا الصوت مفعّلاً (بلاغ v1.71) */
        vibrate: Boolean = false,
    ) {
        stop()
        playingContext = context.applicationContext
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val prepared = runCatching {
            when {
                soundId == AdhanVoices.CUSTOM_ID && customPath.isNotEmpty() &&
                    File(customPath).exists() -> MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(customPath)
                    prepare()
                }

                soundId == AdhanVoices.NOTIFICATION_ID -> {
                    var uri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    if (uri == null) null else MediaPlayer().apply {
                        setAudioAttributes(attrs)
                        setDataSource(context, uri)
                        prepare()
                    }
                }

                else -> {
                    val resId = AdhanVoices.builtinResId(context, kind, soundId)
                    if (resId == 0) null else MediaPlayer().apply {
                        setAudioAttributes(attrs)
                        setDataSource(context, Uri.parse("android.resource://${context.packageName}/$resId"))
                        prepare()
                    }
                }
            }
        }.getOrNull() ?: return
        prepared.setOnCompletionListener { stop() }
        prepared.setOnErrorListener { _, _, _ ->
            stop()
            true
        }
        player = prepared
        prepared.start()
        if (vibrate) {
            VibrationRinger.startLooping(context)
        }
    }

    fun stop() {
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        player = null
        // المعاينة انتهت أو أُوقفت — الاهتزاز ينقطع معها (بلاغ v1.71)
        playingContext?.let { VibrationRinger.cancel(it) }
        playingContext = null
    }
}
