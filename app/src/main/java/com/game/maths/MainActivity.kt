package com.game.maths

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.rgb(111, 209, 248)
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.rgb(255, 246, 226)
        setContent { MaterialTheme { MathPlanetApp() } }
    }
}

private enum class ExitPrompt { NONE, LEAVE_ROUND, EXIT_APP }

@Composable
private fun MathPlanetApp(model: MathViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycle = LocalLifecycleOwner.current
    // Saved audio settings are applied before HOME/PLAY effects issue first speech.
    // Previously initial configure happened later in an asynchronous LaunchedEffect.
    val audio = remember {
        AudioDirector(context.applicationContext).also { it.configure(model.settings) }
    }
    val screen = model.screen
    val settings = model.settings
    // Read this Compose state so mutations inside the pure-Kotlin session trigger recomposition.
    @Suppress("UNUSED_VARIABLE")
    val revision = model.revision
    val session = model.session
    val questionIndex = session?.questionIndex
    var exitPrompt by remember { mutableStateOf(ExitPrompt.NONE) }
    var saidHello by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(audio, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> audio.setResumed(true)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> audio.setResumed(false)
                else -> Unit
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        if (lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            audio.setResumed(true)
        }
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            audio.close()
        }
    }
    LaunchedEffect(settings) { audio.configure(settings) }
    LaunchedEffect(screen) {
        if (screen == Screen.HOME && !saidHello) {
            delay(600)
            audio.sayWelcome()
            saidHello = true
        }
    }
    LaunchedEffect(screen, questionIndex, session) {
        delay(230)
        when (screen) {
            Screen.PLAY -> session?.problem?.let { audio.sayQuestion(it, session.questionNumber) }
            Screen.RESULT -> audio.sayFinish()
            else -> Unit
        }
    }

    fun goBack(fromSystem: Boolean = false) {
        // Android performs its own back-gesture haptic. Do not overlay a second vibration.
        if (BackFeedbackPolicy.playAppTap(fromSystem)) audio.tap()
        when (screen) {
            Screen.HOME -> exitPrompt = ExitPrompt.EXIT_APP
            Screen.HUB -> model.home()
            Screen.SOLO -> { audio.stopVoice(); model.hub() }
            Screen.ARENA_PICK -> model.hub()
            Screen.ARENA -> { audio.stopVoice(); model.arenaPicker() }
            Screen.DUEL_PICK -> model.hub()
            Screen.DUEL -> { audio.stopVoice(); model.duelPicker() }
            Screen.PARENTS -> model.hub()
            Screen.PLAY -> exitPrompt = ExitPrompt.LEAVE_ROUND
            Screen.SETTINGS -> {
                audio.configure(settings)
                model.exitSettings()
            }
            Screen.RESULT -> {
                audio.stopVoice()
                model.home()
            }
        }
    }
    BackHandler(enabled = true) {
        if (exitPrompt != ExitPrompt.NONE) {
            // System back dismisses the confirmation; no additional app vibration.
            exitPrompt = ExitPrompt.NONE
        } else goBack(fromSystem = true)
    }

    Box(Modifier.fillMaxSize().background(PlanetBackground)) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                settings = settings,
                totalStars = model.allStars,
                completedRounds = model.allRounds,
                audio = audio,
                onStart = { audio.tap(); model.hub() },
                onSettings = { audio.tap(); model.settings() }
            )
            Screen.HUB -> AdventureHub(
                rounds = model.soloRounds,
                maxNumber = settings.maxNumber,
                onBack = { audio.tap(); model.home() },
                onSolo = { audio.tap(); model.launchSolo(it) },
                onClassic = { audio.tap(); model.start() },
                onArena = { audio.tap(); model.arenaPicker() },
                onDuel = { audio.tap(); model.duelPicker() },
                onSettings = { audio.tap(); model.settings() },
                onParents = { audio.tap(); model.parentDashboard() }
            )
            Screen.SOLO -> androidx.compose.runtime.key(model.adventureRun, model.shopRole) {
                SoloAdventureScreen(
                    game = model.soloGame,
                    rounds = model.soloRounds, maxNumber = settings.maxNumber,
                    shopRole = model.shopRole, onShopRoleChange = model::switchShopRole,
                    context = context, audio = audio,
                    review = model.reviewQuestions,
                    onBack = { audio.stopVoice(); model.hub() },
                    onAgain = { audio.stopVoice(); model.replayAdventure() }
                )
            }
            Screen.ARENA_PICK -> BattlePicker(
                maxNumber = settings.maxNumber,
                onBack = { audio.tap(); model.hub() },
                onStart = { audio.tap(); model.launchArena(it) }
            )
            Screen.ARENA -> androidx.compose.runtime.key(model.adventureRun) {
                BattleAdventureScreen(
                    options = model.arenaOptions,
                    maxNumber = settings.maxNumber,
                    context = context, audio = audio,
                    onBack = { audio.stopVoice(); model.arenaPicker() },
                    onAgain = { audio.stopVoice(); model.replayAdventure() }
                )
            }
            Screen.DUEL_PICK -> ArithmeticDuelPicker(
                maxNumber = settings.maxNumber,
                onBack = { audio.tap(); model.hub() },
                onStart = { audio.tap(); model.launchDuel(it) }
            )
            Screen.DUEL -> androidx.compose.runtime.key(model.adventureRun) {
                ArithmeticDuelScreen(
                    options = model.duelOptions,
                    maxNumber = settings.maxNumber, context = context, audio = audio,
                    onBack = { audio.stopVoice(); model.duelPicker() },
                    onAgain = { audio.stopVoice(); model.replayAdventure() }
                )
            }
            Screen.PARENTS -> ParentDashboard(
                context = context, maxNumber = settings.maxNumber,
                onBack = { audio.tap(); model.hub() },
                onPractice = { problems ->
                    audio.tap()
                    model.launchSolo(SoloGame.REVIEW, problems.filter { it.answer in 0..settings.maxNumber &&
                        it.a in 0..settings.maxNumber && it.b in 0..settings.maxNumber &&
                        (!it.isMoney || (it.price in 0..settings.maxNumber && it.paid in 0..settings.maxNumber)) })
                }
            )
            Screen.PLAY -> {
                val round = session
                if (round != null) PlayScreen(
                    round = round,
                    revision = revision,
                    audio = audio,
                    onBack = { goBack() },
                    onChoose = { value ->
                        val outcome = model.choose(value)
                        if (outcome != AttemptOutcome.IGNORED) {
                            if (outcome == AttemptOutcome.CORRECT ||
                                outcome == AttemptOutcome.CORRECT_AFTER_RETRY) audio.success()
                            else audio.retry()
                            round.problem?.let { audio.sayAttempt(outcome, it, value) }
                        }
                    },
                    // High-frequency keypad presses give tactile feedback, but must not
                    // spawn overlapping SoundPool clicks during PCM narration.
                    onEnterDigit = { digit -> audio.keypadTap(); model.enterDigit(digit) },
                    onDeleteDigit = { audio.keypadTap(); model.deleteDigit() },
                    onToggleSign = { audio.keypadTap(); model.toggleNumericSign() },
                    onSubmitNumeric = {
                        val entered = round.numericDigits.toIntOrNull()
                        if (entered != null) {
                            val outcome = model.submitNumericAnswer()
                            if (outcome != AttemptOutcome.IGNORED) {
                                if (outcome == AttemptOutcome.CORRECT ||
                                    outcome == AttemptOutcome.CORRECT_AFTER_RETRY) audio.success()
                                else audio.retry()
                                round.problem?.let { audio.sayAttempt(outcome, it, entered) }
                            }
                        }
                    },
                    onSkip = {
                        audio.tap()
                        audio.stopVoice()
                        if (model.skip()) model.next()
                    },
                    onNext = { audio.tap(); audio.stopVoice(); model.next() },
                    onAdjust = { audio.tap(); model.settings() }
                )
            }
            Screen.SETTINGS -> SettingsScreen(
                initial = settings,
                audio = audio,
                onBack = { goBack() },
                onSave = { updated ->
                    model.save(updated)
                    audio.configure(model.settings)
                },
                onPreviewAudio = { audio.preview() },
                onAudioPreviewSettings = { audio.configure(it) }
            )
            Screen.RESULT -> ResultScreen(
                round = session,
                allStars = model.allStars,
                audio = audio,
                onAgain = { audio.tap(); model.start() },
                onHome = { audio.tap(); audio.stopVoice(); model.home() }
            )
        }

        if (exitPrompt != ExitPrompt.NONE) {
            PlanetExitDialog(
                isExitApp = exitPrompt == ExitPrompt.EXIT_APP,
                // The OS supplies feedback for system back; don't add a second haptic.
                onDismiss = { exitPrompt = ExitPrompt.NONE },
                onStay = {
                    audio.tap()
                    exitPrompt = ExitPrompt.NONE
                },
                onConfirm = {
                    audio.tap()
                    val confirm = exitPrompt
                    exitPrompt = ExitPrompt.NONE
                    audio.stopVoice()
                    if (confirm == ExitPrompt.EXIT_APP) activity?.finish() else model.home()
                }
            )
        }
    }
}
