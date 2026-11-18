package com.game.maths

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

enum class Screen { HOME, HUB, PLAY, SOLO, ARENA_PICK, ARENA, DUEL_PICK, DUEL, PARENTS, SETTINGS, RESULT }

private object SavedProgress {
    private const val PREF = "math_planet_settings"
    private const val VOLUME_MIGRATED = "music_default_25_migrated"

    private fun loadMusicVolume(p: SharedPreferences): Float {
        val migrated = p.getBoolean(VOLUME_MIGRATED, false)
        val previous = if (p.contains("music_volume"))
            p.getFloat("music_volume", MusicVolumePolicy.defaultVolume) else null
        val volume = MusicVolumePolicy.resolve(previous, migrated)
        if (!migrated) {
            p.edit().putFloat("music_volume", volume).putBoolean(VOLUME_MIGRATED, true).apply()
        }
        return volume
    }

    fun loadSettings(context: Context): GameSettings {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val op = p.getString("ops", "ADD,SUBTRACT").orEmpty()
            .split(",").mapNotNull { key -> Operation.entries.firstOrNull { it.name == key } }.toSet()
        return GameSettings(
            minNumber = p.getInt("min", 0),
            maxNumber = p.getInt("max", 10),
            terms = p.getInt("terms", 2),
            operations = op,
            voiceEnabled = p.getBoolean("voice", true),
            soundEnabled = p.getBoolean("sound", true),
            musicEnabled = p.getBoolean("music", true),
            musicVolume = loadMusicVolume(p),
            hapticsEnabled = p.getBoolean("haptic", true),
            roundLength = p.getInt("rounds", 10),
            answerMode = AnswerMode.fromStored(p.getString("answer_mode", null)),
            allowNegative = false,
            voicePlaybackMode = VoicePlaybackMode.fromStored(p.getString("voice_playback_mode", null))
        ).normalized()
    }
    fun save(context: Context, s: GameSettings) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt("min", s.minNumber)
            .putInt("max", s.maxNumber)
            .putInt("terms", s.terms)
            .putString("ops", s.operations.joinToString(",") { it.name })
            .putBoolean("voice", s.voiceEnabled)
            .putBoolean("sound", s.soundEnabled)
            .putBoolean("music", s.musicEnabled)
            .putFloat("music_volume", s.musicVolume)
            .putBoolean("haptic", s.hapticsEnabled)
            .putInt("rounds", s.roundLength)
            .putString("answer_mode", s.answerMode.name)
            .putBoolean("allow_negative", s.allowNegative)
            .putString("voice_playback_mode", s.voicePlaybackMode.name)
            .apply()
    }
    private const val RECENT_KEY = "recent_arithmetic_skills"

    fun recentSkills(context: Context): Set<String> =
        RecentProblemHistory.read(
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(RECENT_KEY, null)
        ).toSet()

    fun recordSkill(context: Context, learningKey: String) {
        val preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val updated = RecentProblemHistory.add(preferences.getString(RECENT_KEY, null), learningKey)
        preferences.edit().putString(RECENT_KEY, updated).apply()
    }

    fun stars(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .getInt("collected_stars", 0)
    fun rounds(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .getInt("completed_rounds", 0)
    fun saveProgress(context: Context, stars: Int, rounds: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt("collected_stars", stars)
            .putInt("completed_rounds", rounds).apply()
    }
}

/** Retains exact question, attempts and stars across tablet orientation changes. */
class MathViewModel(application: Application) : AndroidViewModel(application) {
    var settings by mutableStateOf(SavedProgress.loadSettings(application))
        private set
    var screen by mutableStateOf(Screen.HOME)
        private set
    var session by mutableStateOf<RoundSession?>(null)
        private set
    var revision by mutableIntStateOf(0)
        private set
    var allStars by mutableIntStateOf(SavedProgress.stars(application))
        private set
    var allRounds by mutableIntStateOf(SavedProgress.rounds(application))
        private set

    private val appContext = getApplication<Application>()
    private val adventurePreferences = appContext.getSharedPreferences("math_planet_adventure", Context.MODE_PRIVATE)
    // One source of truth: the single-player round length is the saved GameSettings value.
    val soloRounds: Int get() = settings.roundLength
    var shopRole by mutableStateOf(
        ShopRole.entries.firstOrNull {
            it.name == adventurePreferences.getString("shop_role", "CUSTOMER")
        } ?: ShopRole.CUSTOMER)
        private set
    var cannonTimed by mutableStateOf(adventurePreferences.getBoolean("timed", false))
        private set
    var cannonTopic by mutableStateOf(
        SkillTopic.entries.firstOrNull {
            it.name == adventurePreferences.getString("cannon_topic", "MIXED")
        } ?: SkillTopic.MIXED)
        private set
    var strategyHints by mutableStateOf(adventurePreferences.getBoolean("hints", true))
        private set
    var soloGame by mutableStateOf(SoloGame.SHOP)
        private set
    var arenaOptions by mutableStateOf(ArenaOptions())
        private set
    var duelOptions by mutableStateOf(DuelOptions())
        private set
    var reviewQuestions by mutableStateOf<List<SkillQuestion>>(emptyList())
        private set
    var adventureRun by mutableIntStateOf(0)
        private set

    fun hub() { screen = Screen.HUB }
    fun parentDashboard() { screen = Screen.PARENTS }
    fun arenaPicker() { screen = Screen.ARENA_PICK }
    fun duelPicker() { screen = Screen.DUEL_PICK }
    fun launchDuel(options: DuelOptions) {
        duelOptions = options
        adventureRun++
        screen = Screen.DUEL
    }
    fun launchSolo(game: SoloGame, review: List<SkillQuestion> = emptyList()) {
        soloGame = game
        reviewQuestions = review
        adventureRun++
        screen = Screen.SOLO
    }
    fun launchArena(options: ArenaOptions) {
        arenaOptions = options
        adventureRun++
        screen = Screen.ARENA
    }
    fun replayAdventure() { adventureRun++ }
    fun switchShopRole(role: ShopRole) {
        if (role != shopRole) {
            shopRole = role
            adventurePreferences.edit().putString("shop_role", role.name).apply()
            adventureRun++
        }
    }
    fun updateCannonTimed(value: Boolean) {
        cannonTimed = value
        adventurePreferences.edit().putBoolean("timed", value).apply()
    }
    fun updateCannonTopic(topic: SkillTopic) {
        cannonTopic = topic
        adventurePreferences.edit().putString("cannon_topic", topic.name).apply()
    }
    fun setHints(value: Boolean) {
        strategyHints = value
        adventurePreferences.edit().putBoolean("hints", value).apply()
    }

    private var startedQuestionAt = android.os.SystemClock.elapsedRealtime()
    private var incorrectThisQuestion = false
    private fun rememberCurrentQuestion() {
        session?.problem?.let {
            SavedProgress.recordSkill(getApplication(), RoundProblemPlanner.learningKey(it))
        }
    }

    fun start() {
        startedQuestionAt = android.os.SystemClock.elapsedRealtime()
        incorrectThisQuestion = false
        session = RoundSession(
            settings, recentSkills = SavedProgress.recentSkills(getApplication())
        )
        rememberCurrentQuestion()
        revision++
        screen = Screen.PLAY
    }

    private fun logClassic(outcome: AttemptOutcome) {
        if (outcome == AttemptOutcome.RETRY) { incorrectThisQuestion = true; return }
        if (outcome == AttemptOutcome.IGNORED) return
        val problem = session?.problem ?: return
        val success = (outcome == AttemptOutcome.CORRECT ||
            outcome == AttemptOutcome.CORRECT_AFTER_RETRY) && !incorrectThisQuestion
        TrainingHistory.add(appContext, TrainingRecord("算数探险",
            problem.expression, RoundProblemPlanner.learningKey(problem),
            success, success, android.os.SystemClock.elapsedRealtime() - startedQuestionAt))
    }
    fun choose(value: Int): AttemptOutcome {
        val result = session?.choose(value) ?: AttemptOutcome.IGNORED
        logClassic(result)
        if (result != AttemptOutcome.IGNORED) revision++
        return result
    }

    fun submitNumericAnswer(): AttemptOutcome {
        val result = session?.submitNumericAnswer() ?: AttemptOutcome.IGNORED
        logClassic(result)
        if (result != AttemptOutcome.IGNORED) revision++
        return result
    }

    fun enterDigit(digit: Int) { session?.enterDigit(digit) }
    fun deleteDigit() { session?.deleteDigit() }
    fun toggleNumericSign() { session?.toggleNumericSign() }

    fun skip(): Boolean {
        val didSkip = session?.skip() ?: false
        if (didSkip) { logClassic(AttemptOutcome.REVEAL); revision++ }
        return didSkip
    }

    fun next() {
        val s = session ?: return
        if (!s.next()) return
        if (!s.finished) { rememberCurrentQuestion(); startedQuestionAt = android.os.SystemClock.elapsedRealtime(); incorrectThisQuestion = false }
        revision++
        if (s.finished) {
            allStars += s.stars
            allRounds++
            SavedProgress.saveProgress(getApplication(), allStars, allRounds)
            screen = Screen.RESULT
        }
    }

    fun home() { screen = Screen.HOME }
    private var settingsReturnScreen = Screen.HOME
    fun settings() {
        settingsReturnScreen = when (screen) {
            Screen.HUB, Screen.DUEL_PICK, Screen.ARENA_PICK -> screen
            else -> Screen.HOME
        }
        screen = Screen.SETTINGS
    }
    fun exitSettings() { screen = settingsReturnScreen }

    fun save(next: GameSettings) {
        settings = next.copy(minNumber = 0, allowNegative = false).normalized()
        SavedProgress.save(getApplication(), settings)
        screen = settingsReturnScreen
    }
}
