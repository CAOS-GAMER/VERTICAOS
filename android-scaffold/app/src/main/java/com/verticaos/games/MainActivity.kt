package com.verticaos.games

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.verticaos.games.ui.LauncherScreen
import com.verticaos.games.ui.VerticaosTheme

class MainActivity : ComponentActivity() {

    private lateinit var gamesRepo: GameRepository
    private lateinit var config: ConfigManager
    private lateinit var creditManager: CreditManager
    private var lastLaunchedPackage: String? = null
    private var selectedIndex: Int = 0
        set(value) { field = value.coerceIn(0, (gamesRepo.size - 1).coerceAtLeast(0)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lockVerticalOrientationAndMetrics()
        enterImmersiveFullscreen()

        try {
            // Desliga a Rotação Automática do sistema operacional
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        RootShellManager.initialize()

        config = ConfigManager(this)
        creditManager = CreditManager(this)
        gamesRepo = GameRepository(packageManager, config).also { it.refresh() }

        startService(Intent(this, GameMonitorService::class.java))

        // ACESSIBILIDADE E ROOT: Abrir automaticamente no primeiro uso para facilitar o setup
        checkInitialPermissions()

        setContent {
            VerticaosTheme {
                val gameList = remember { mutableStateListOf<GameEntry>().apply { addAll(gamesRepo.list) } }
                val enabledPackagesState = remember { mutableStateOf(config.getEnabledPackages()) }
                val currentCredits by creditManager.credits.collectAsState()

                LauncherScreen(
                    games = gameList,
                    allAvailable = gamesRepo.allAvailable,
                    enabledPackages = enabledPackagesState.value,
                    credits = currentCredits,
                    operationMode = config.getOperationMode(),
                    onLaunch = { game -> launchGame(game) },
                    onSelectionChanged = { idx ->
                        selectedIndex = idx
                        creditManager.playNavigationSound()
                    },
                    onToggleGame = { pkg ->
                        config.togglePackage(pkg)
                        gamesRepo.refresh()
                        gameList.clear()
                        gameList.addAll(gamesRepo.list)
                        enabledPackagesState.value = config.getEnabledPackages()
                    },
                    onUpdateDelay = { pkg, delay ->
                        config.setGameDelay(pkg, delay)
                        gamesRepo.refresh()
                        gameList.clear()
                        gameList.addAll(gamesRepo.list)
                    },
                    onUpdatePulses = { pkg, pulses ->
                        config.setPulsesPerCredit(pkg, pulses)
                        gamesRepo.refresh()
                        gameList.clear()
                        gameList.addAll(gamesRepo.list)
                    },
                    onUpdateMode = { mode ->
                        config.setOperationMode(mode)
                        gameList.clear()
                        gameList.addAll(gamesRepo.list)
                    },
                    onGenerateList = {
                        gamesRepo.generateJogosTxt()
                        gamesRepo.refresh()
                        gameList.clear()
                        gameList.addAll(gamesRepo.list)
                        enabledPackagesState.value = config.getEnabledPackages()
                        Toast.makeText(this, "Lista gerada em Downloads/list/jogos.txt!", Toast.LENGTH_LONG).show()
                    },
                    playNavSound = { creditManager.playNavigationSound() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lockVerticalOrientationAndMetrics()
        enterImmersiveFullscreen()
        gamesRepo.refresh()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveFullscreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        creditManager.release()
        RootShellManager.shutdown()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedServiceName = "$packageName/${ArcadeAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedServiceName) == true
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val readPerm = Manifest.permission.READ_EXTERNAL_STORAGE
            val writePerm = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (checkSelfPermission(readPerm) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(writePerm) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(readPerm, writePerm), 101)
            }
        }
    }

    private fun checkInitialPermissions() {
        RootShellManager.initialize()

        checkStoragePermissions()

        if (!isAccessibilityServiceEnabled()) {
            try {
                Toast.makeText(this, "ATIVE O SERVIÇO 'Arcade Verticaos'", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        // ABSOLUTE DECOUPLING: Consume all D-Pad directional keys so they NEVER bubble up or trigger clicks
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            super.dispatchKeyEvent(event)
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Tecla 5 para INSERIR CRÉDITO
        if (keyCode == KeyEvent.KEYCODE_5 || keyCode == KeyEvent.KEYCODE_NUMPAD_5) {
            creditManager.addCredit()
            return true
        }

        // Tecla 3 (ou Numpad 3 ou Botão B) para fechar tudo
        if (keyCode == KeyEvent.KEYCODE_3 ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_3 ||
            keyCode == KeyEvent.KEYCODE_BUTTON_B ||
            keyCode == KeyEvent.KEYCODE_ESCAPE) {

            lastLaunchedPackage?.let { pkg ->
                RootShellManager.forceStop(pkg)
            }

            forceExitRunningGame()
            return true
        }

        // EXCLUSIVE ACTION KEYS FOR START GAME: Key 1, Numpad 1, Enter, D-Pad Center, Button A
        if (keyCode == KeyEvent.KEYCODE_1 ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_1 ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
            keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_BUTTON_A) {
            gamesRepo.list.getOrNull(selectedIndex)?.let { launchGame(it) }
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE) {
            // Suporte a analógico de controles USB/Xbox
        }
        return super.onGenericMotionEvent(event)
    }

    private fun launchGame(game: GameEntry) {
        val mode = config.getOperationMode()
        val pkg = game.packageName
        val intent = packageManager.getLaunchIntentForPackage(pkg)

        if (intent == null) {
            Toast.makeText(this, "ERRO: APK NÃO ENCONTRADO", Toast.LENGTH_SHORT).show()
            return
        }

        // MODO FREEPLAY (Lógica 0)
        if (mode == 0) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            lastLaunchedPackage = pkg
            startActivity(intent)
            return
        }

        // MODO HABILIDADE (Lógica 1)
        val insertedPulses = creditManager.credits.value
        val requiredPulses = game.pulsesPerCredit

        if (insertedPulses < requiredPulses) {
            Toast.makeText(this, "CRÉDITO INSUFICIENTE", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculates total credits and pulses to inject without resetting state early
        val (totalCredits, totalPulsesToInject) = creditManager.calculateCreditsToInject(requiredPulses)

        if (totalPulsesToInject > 0) {
            // Deduz os pulsos consumidos imediatamente para manter o estado atualizado
            creditManager.deductPulses(totalPulsesToInject)

            val serviceIntent = Intent(this, CreditInjectionService::class.java).apply {
                putExtra("PULSES_TO_INJECT", totalPulsesToInject)
                putExtra("CREDITS", totalCredits)
                putExtra("DELAY_SECONDS", game.creditDelaySeconds)
                putExtra("PULSE_INTERVAL_MS", 250L)
                putExtra("IS_GAME_LAUNCH", true)
            }
            startService(serviceIntent)

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            lastLaunchedPackage = pkg
            startActivity(intent)
        }
    }

    private fun forceExitRunningGame() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val launcherPkg = packageName

        try {
            am.appTasks?.forEach { task ->
                val baseIntent = task.taskInfo.baseIntent
                if (baseIntent?.component?.packageName != launcherPkg) {
                    task.finishAndRemoveTask()
                }
            }
        } catch (e: Exception) {}

        val running = am.runningAppProcesses.orEmpty()
        for (proc in running) {
            if (proc.processName != launcherPkg &&
                !proc.processName.startsWith("com.android.") &&
                !proc.processName.startsWith("android")) {
                am.killBackgroundProcesses(proc.processName)
            }
        }

        val home = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                     Intent.FLAG_ACTIVITY_CLEAR_TASK or
                     Intent.FLAG_ACTIVITY_CLEAR_TOP or
                     Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(home)
        enterImmersiveFullscreen()
    }

    private fun lockVerticalOrientationAndMetrics() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val metrics = resources.displayMetrics
        val config = resources.configuration

        // If TV Box hardware drivers report landscape dimensions (width > height) while in portrait,
        // normalize the metrics so Jetpack Compose computes vertical bounds (width <= height) properly.
        if (metrics.widthPixels > metrics.heightPixels) {
            val w = metrics.widthPixels
            val h = metrics.heightPixels
            metrics.widthPixels = h
            metrics.heightPixels = w
            metrics.xdpi = metrics.ydpi.also { metrics.ydpi = metrics.xdpi }
        }

        if (config.orientation != Configuration.ORIENTATION_PORTRAIT) {
            config.orientation = Configuration.ORIENTATION_PORTRAIT
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, metrics)
        }
    }

    private fun enterImmersiveFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}
