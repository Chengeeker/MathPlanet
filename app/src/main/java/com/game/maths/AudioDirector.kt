package com.game.maths

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

/** Controls offline speech, background music, short effects and haptics independently. */
class AudioDirector(private val context: Context) {
    private val lock = Any()
    private val main = Handler(Looper.getMainLooper())
    private val hapticGate = HapticGate(minimumIntervalMillis = 120L)
    private val clickGate = UiSoundGate(minimumIntervalMillis = 180L)
    private val startupSpeech = StartupSpeechGate()
    private var resumed = false
    private var narrating = false
    private var externalNarration = false

    /** Arbitrary practice TTS needs the same exclusive music routing as clip narration. */
    fun setExternalNarrating(active: Boolean) {
        synchronized(lock) {
            if (disposed) return
            externalNarration = active
            updateMusicLocked()
        }
    }
    private var disposed = false
    private var settings = GameSettings()

    // AudioTrack and MediaPlayer must not run concurrently on PA2373. Dispatch
    // on the main looper, but apply an on-main "speaking=true" synchronously
    // BEFORE VoicePlayback schedules its PCM worker. This removes the 1-frame
    // race where music is still active as voice playback begins.
    private val voice = VoicePlayback(context) { speaking ->
        val applyState = Runnable {
            synchronized(lock) {
                if (!disposed) {
                    narrating = speaking
                    updateMusicLocked()
                }
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) applyState.run()
        else main.post(applyState)
    }
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val sounds = SoundPool.Builder().setMaxStreams(2)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build())
        .build()
    private val tapSound = sounds.load(context, R.raw.tap, 1)
    private val successSound = sounds.load(context, R.raw.success, 1)
    private val retrySound = sounds.load(context, R.raw.retry, 1)
    private val music = runCatching {
        MediaPlayer.create(context, R.raw.adventure_loop)?.apply {
            isLooping = true
            setVolume(0f, 0f)
        }
    }.onFailure { Log.e("MathPlanetAudio", "Background music initialization failed", it) }.getOrNull()

    private var fadeToken = 0
    private var currentMusicVolume = 0f

    val ready get() = voice.ready
    val voiceStatus get() = if (ready)
        "内置中文配音：已加载 " + voice.clipCount + " 个片段"
        else "配音资源损坏，请重新安装"
    val playbackStatus get() = voice.status

    fun configure(next: GameSettings) {
        val pending = synchronized(lock) {
            if (disposed) return
            settings = next
            voice.setMode(next.voicePlaybackMode)
            if (!next.voiceEnabled) {
                startupSpeech.clear()
                voice.stop()
                voice.reportOff()
            } else {
                voice.reportOn()
                if (resumed) voice.prepare()
            }
            updateMusicLocked()
            if (resumed && next.voiceEnabled) startupSpeech.onForeground() else null
        }
        pending?.let(voice::play)
    }

    fun setResumed(isResumed: Boolean) {
        val pending = synchronized(lock) {
            if (disposed) return
            resumed = isResumed
            if (!resumed) {
                voice.stop()
            } else if (settings.voiceEnabled) {
                // Warm the PCM output route once, before the first spoken request.
                voice.prepare()
            }
            updateMusicLocked()
            if (resumed && settings.voiceEnabled) startupSpeech.onForeground() else null
        }
        pending?.let {
            Log.i("MathPlanetAudio", "Playing deferred initial narration after ON_RESUME")
            voice.play(it)
        }
    }

    private fun updateMusicLocked() {
        if (disposed) return
        runCatching {
            val player = music ?: return@runCatching
            val target = AudioMixPolicy.musicTarget(settings.musicVolume, narrating || externalNarration)
            val enabled = resumed && settings.musicEnabled && target > .001f
            if (!enabled) {
                fadeToken++
                currentMusicVolume = 0f
                player.setVolume(0f, 0f)
                if (player.isPlaying) {
                    player.pause()
                    if (narrating) Log.i("MathPlanetAudio", "BGM paused before PCM narration")
                }
                return@runCatching
            }
            if (!player.isPlaying) {
                currentMusicVolume = 0f
                player.setVolume(0f, 0f)
                player.start()
                Log.i("MathPlanetAudio", "BGM resumed between narrations")
            }
            // Fade only on resume; ducking still leaves a competing MediaPlayer
            // track active and was confirmed to slow speech on the PA2373 tablet.
            fadeMusicLocked(target, 220L)
        }.onFailure { Log.w("MathPlanetAudio", "Background music volume/state problem", it) }
    }

    private fun fadeMusicLocked(target: Float, durationMs: Long) {
        val player = music ?: return
        fadeToken++
        val token = fadeToken
        val initial = currentMusicVolume
        if (kotlin.math.abs(initial - target) < .002f) {
            currentMusicVolume = target
            player.setVolume(target, target)
            return
        }
        val startAt = SystemClock.uptimeMillis()
        main.post(object : Runnable {
            override fun run() {
                synchronized(lock) {
                    if (disposed || token != fadeToken || !resumed || !settings.musicEnabled)
                        return
                    val fraction = ((SystemClock.uptimeMillis() - startAt).toFloat() /
                        durationMs.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    currentMusicVolume = initial + (target - initial) * fraction
                    runCatching {
                        music?.setVolume(currentMusicVolume, currentMusicVolume)
                    }.onFailure { Log.w("MathPlanetAudio", "BGM fade error", it) }
                    if (fraction < 1f) main.postDelayed(this, 16L)
                }
            }
        })
    }

    fun tap() {
        val now = SystemClock.elapsedRealtime()
        if (settings.soundEnabled && clickGate.shouldPlay(now, narrating)) {
            sounds.play(tapSound, .30f, .30f, 1, 0, 1f)
        }
        vibrateOnce(14)
    }

    /**
     * Numeric keypad presses are high-frequency (unlike one-time multiple choice).
     * Use tactile feedback only: repeated 22.05kHz SoundPool clicks were mixing over
     * the 48kHz PCM narration and BGM on every digit/erase/sign press.
     */
    fun keypadTap() {
        vibrateOnce(11)
    }

    /**
     * Integrated practice questions use a separate device-TTS reader, not
     * classic PCM voice confirmation. Their answer effects must not be
     * suppressed by the classic voiceEnabled flag.
     * One tap -> one result-specific sound and one haptic (never tap()+success()).
     */
    fun practiceAnswer(correct: Boolean) {
        if (disposed) return
        if (PracticeFeedbackPolicy.shouldPlayResultSound(
                settings.soundEnabled, narrating, externalNarration)) {
            val soundId = if (correct) successSound else retrySound
            val volume = if (correct) .70f else .55f
            sounds.play(soundId, volume, volume, 1, 0, 1f)
        }
        vibrateOnce(if (correct) 30 else 38)
    }

    fun success() {
        // When speech feedback is enabled, don't overlay a 22.05 kHz success sound
        // with the next PCM narration. A voice confirmation and haptic still play.
        if (AudioMixPolicy.shouldPlayEffect(settings.soundEnabled, settings.voiceEnabled)) {
            sounds.play(successSound, .70f, .70f, 1, 0, 1f)
        }
        vibrateOnce(30)
    }

    fun retry() {
        if (AudioMixPolicy.shouldPlayEffect(settings.soundEnabled, settings.voiceEnabled)) {
            sounds.play(retrySound, .55f, .55f, 1, 0, 1f)
        }
        vibrateOnce(38)
    }

    @Suppress("DEPRECATION")
    private fun vibrateOnce(ms: Long) {
        if (!settings.hapticsEnabled || vibrator?.hasVibrator() != true) return
        if (!hapticGate.tryAcquire(SystemClock.elapsedRealtime())) return
        runCatching {
            if (Build.VERSION.SDK_INT >= 26)
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, 75))
            else vibrator?.vibrate(ms)
        }
    }

    private fun say(tokens: List<String>) {
        val playable = synchronized(lock) {
            if (disposed) return
            if (!settings.voiceEnabled) {
                voice.reportOff()
                return
            }
            startupSpeech.submit(tokens, resumed).also {
                if (it == null) Log.i("MathPlanetAudio",
                    "First narration deferred: Activity not yet RESUMED; tokens=" + tokens.size)
            }
        }
        playable?.let(voice::play)
    }

    fun sayWelcome() = say(listOf("welcome"))
    fun preview() = say(listOf("test"))
    fun sayFinish() = say(listOf("finish"))

    /** Reuse pre-rendered, offline numeric speech on the existing serialized PCM path. */
    fun sayShopNumbers(price: Int, paid: Int) {
        val tokens = mutableListOf("shop_price")
        tokens += MandarinNumbers.tokens(price)
        tokens += "yuan"
        if (paid > 0) {
            tokens += "shop_paid"
            tokens += MandarinNumbers.tokens(paid)
            tokens += "yuan"
            tokens += "shop_change"
        } else tokens += "shop_pay"
        say(tokens)
    }

    fun sayShopFeedback(correct: Boolean, excessive: Boolean) {
        say(listOf(when {
            correct -> "shop_success"
            excessive -> "shop_more"
            else -> "shop_less"
        }))
    }

    fun sayQuestion(problem: MathProblem, index: Int, repeat: Boolean = false) {
        val result = mutableListOf<String>()
        if (repeat) result += "ask_again"
        result += "q_begin"
        result += MandarinNumbers.tokens(index)
        result += "q_after_index"
        result += MandarinNumbers.tokenizeExpression(problem.expression)
        result += "q_equal"
        say(result)
    }

    fun sayAttempt(outcome: AttemptOutcome, problem: MathProblem, chosen: Int) {
        val reply = when (outcome) {
            AttemptOutcome.CORRECT -> listOf("good")
            AttemptOutcome.CORRECT_AFTER_RETRY -> listOf("good_after_retry")
            AttemptOutcome.RETRY -> listOf("retry")
            AttemptOutcome.REVEAL -> listOf("show_answer") +
                MandarinNumbers.tokens(problem.answer) + explanationTokens(problem)
            AttemptOutcome.IGNORED -> emptyList()
        }
        if (reply.isNotEmpty()) say(listOf("tap") + MandarinNumbers.tokens(chosen) + reply)
    }

    fun sayExplanation(problem: MathProblem) = say(explanationTokens(problem))

    private fun explanationTokens(problem: MathProblem): List<String> {
        val phrases = mutableListOf("explain")
        var value = problem.start
        for ((index, step) in problem.steps.withIndex()) {
            if (index > 0) phrases += "then"
            phrases += MandarinNumbers.tokens(value)
            phrases += when (step.operator) {
                Operation.ADD -> "q_plus"
                Operation.SUBTRACT -> "q_minus"
                Operation.MULTIPLY -> "q_multiply"
                Operation.DIVIDE -> "q_divide"
            }
            phrases += MandarinNumbers.tokens(step.operand)
            phrases += "is"
            value = MathEngine.evaluate(value, listOf(step))
            phrases += MandarinNumbers.tokens(value)
        }
        return phrases
    }

    fun stopVoice() {
        synchronized(lock) {
            startupSpeech.clear()
            voice.stop()
        }
    }

    fun close() = synchronized(lock) {
        disposed = true
        fadeToken++
        startupSpeech.clear()
        voice.close()
        runCatching { music?.stop() }
        runCatching { music?.release() }
        runCatching { sounds.release() }
    }
}
