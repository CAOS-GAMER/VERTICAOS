package com.verticaos.games

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class CreditInjectionService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var injectionJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val totalPulses = intent?.getIntExtra("PULSES_TO_INJECT", 0)
            .takeIf { it != null && it > 0 }
            ?: intent?.getIntExtra("CREDITS", 0)
            ?: 0

        val delaySeconds = intent?.getIntExtra("DELAY_SECONDS", -1)
            .takeIf { it != null && it >= 0 }
            ?: intent?.getIntExtra("DELAY", 5)
            ?: 5

        val pulseIntervalMs = intent?.getLongExtra("PULSE_INTERVAL_MS", 250L) ?: 250L
        val isGameLaunch = intent?.getBooleanExtra("IS_GAME_LAUNCH", false) ?: false

        if (totalPulses > 0) {
            startCreditInjection(totalPulses, delaySeconds, pulseIntervalMs, isGameLaunch)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startCreditInjection(
        totalPulses: Int,
        delaySeconds: Int,
        pulseIntervalMs: Long,
        isGameLaunch: Boolean
    ) {
        injectionJob?.cancel()

        injectionJob = serviceScope.launch {
            try {
                val targetDelayMs = delaySeconds * 1000L
                val actualDelayMs = if (isGameLaunch) {
                    // Garantir um tempo mínimo de 2.5s na abertura do jogo para o app carregar e focar
                    maxOf(targetDelayMs, 2500L)
                } else {
                    targetDelayMs
                }

                if (actualDelayMs > 0) {
                    delay(actualDelayMs)
                }

                Log.d("CreditInjectionService", "Injetando $totalPulses pulsos após $actualDelayMs ms de delay")

                repeat(totalPulses) { index ->
                    RootShellManager.injectKey(12) // KeyCode 12 = Tecla 5 (Coin / Credit pulse)
                    if (index < totalPulses - 1) {
                        delay(pulseIntervalMs)
                    }
                }

                delay(150L)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CreditInjectionService", "Erro na injeção de créditos: ${e.message}", e)
            } finally {
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
