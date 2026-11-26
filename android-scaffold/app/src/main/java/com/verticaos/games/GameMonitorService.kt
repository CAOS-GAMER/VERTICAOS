package com.verticaos.games

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class GameMonitorService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "FORCE_EXIT") {
            forceExit()
        }
        return START_STICKY
    }

    private fun forceExit() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val launcherPkg = packageName
        
        // 1. Identifica o pacote que estava rodando antes de matar
        // (Tentativa extra caso o lastLaunchedPackage do MainActivity falhe)

        // 2. Limpeza agressiva de tarefas (Compatível API 21+)
        try {
            am.appTasks?.forEach { task ->
                val baseIntent = task.taskInfo.baseIntent
                if (baseIntent?.component?.packageName != launcherPkg) {
                    task.finishAndRemoveTask()
                }
            }
        } catch (e: Exception) {}

        // 3. Kill background processes via Root para garantir limpeza total
        // O RootShellManager já faz o force-stop, aqui reforçamos a limpeza de RAM
        val runningProcesses = am.runningAppProcesses
        if (runningProcesses != null) {
            for (processInfo in runningProcesses) {
                if (processInfo.processName != launcherPkg && 
                    !processInfo.processName.startsWith("com.android.") &&
                    !processInfo.processName.startsWith("android")) {
                    am.killBackgroundProcesses(processInfo.processName)
                }
            }
        }

        // 4. Forçar retorno ao Launcher puxando para frente
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                     Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                     Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(launchIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
