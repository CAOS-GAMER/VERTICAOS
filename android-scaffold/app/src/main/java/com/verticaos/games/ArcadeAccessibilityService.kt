package com.verticaos.games

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Serviço de Acessibilidade essencial para capturar a tecla '3' (SAIR) e a tecla '5' (CRÉDITO)
 * de dentro de qualquer jogo executado no sistema.
 */
class ArcadeAccessibilityService : AccessibilityService() {

    private var currentPackageName: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        forcePortraitRotation()
    }

    private fun forcePortraitRotation() {
        try {
            // 0 = Paisagem, 1 = Retrato, 2 = Paisagem Invertida, 3 = Retrato Invertido
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 1)
            // Desativa a rotação automática
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrEmpty()) {
                currentPackageName = pkg
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode

            // 1. Tecla 3 ou Botão B do Gamepad para SAIR do jogo atual
            if (keyCode == KeyEvent.KEYCODE_3 || 
                keyCode == KeyEvent.KEYCODE_NUMPAD_3 ||
                keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                
                val intent = Intent(this, GameMonitorService::class.java).apply {
                    action = "FORCE_EXIT"
                }
                startService(intent)
                return true 
            }

            // 2. Tecla 5 para INSERIR CRÉDITO quando estiver rodando um jogo em segundo plano
            if (keyCode == KeyEvent.KEYCODE_5 || keyCode == KeyEvent.KEYCODE_NUMPAD_5) {
                if (currentPackageName.isNotEmpty() && currentPackageName != packageName) {
                    val config = ConfigManager(applicationContext)
                    val mode = config.getOperationMode()

                    if (mode == 1) { // Modo Habilidade
                        val creditManager = CreditManager(applicationContext)
                        creditManager.addCredit()

                        val pulsesPerCredit = config.getPulsesPerCredit(currentPackageName)
                        val delaySeconds = config.getGameDelay(currentPackageName)

                        val (_, totalPulsesToInject) = creditManager.calculateCreditsToInject(pulsesPerCredit)
                        if (totalPulsesToInject > 0) {
                            creditManager.deductPulses(totalPulsesToInject)

                            val serviceIntent = Intent(applicationContext, CreditInjectionService::class.java).apply {
                                putExtra("PULSES_TO_INJECT", totalPulsesToInject)
                                putExtra("DELAY_SECONDS", delaySeconds)
                                putExtra("PULSE_INTERVAL_MS", 250L)
                                putExtra("IS_GAME_LAUNCH", false)
                            }
                            startService(serviceIntent)
                        }
                        return true
                    }
                }
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {}
}
