package com.verticaos.games

import java.io.DataOutputStream

object RootShellManager {
    private var rootProcess: Process? = null
    private var os: DataOutputStream? = null

    fun initialize() {
        if (rootProcess == null) {
            try {
                rootProcess = Runtime.getRuntime().exec("su")
                os = DataOutputStream(rootProcess!!.outputStream)
            } catch (e: Exception) {
                // Silently fail if SU is not available yet
                rootProcess = null
                os = null
            }
        }
    }

    @Synchronized
    fun executeCommand(command: String) {
        if (os == null) initialize()

        var executed = false
        if (os != null) {
            try {
                os?.writeBytes("$command\n")
                os?.flush()
                executed = true
            } catch (e: Exception) {
                e.printStackTrace()
                rootProcess = null
                os = null
            }
        }

        if (!executed) {
            try {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun forceStop(packageName: String) {
        executeCommand("am force-stop $packageName")
    }

    fun injectKey(keyCode: Int) {
        executeCommand("input keyevent $keyCode")
    }

    fun shutdown() {
        try {
            os?.let {
                it.writeBytes("exit\n")
                it.flush()
                it.close()
            }
            rootProcess?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        os = null
        rootProcess = null
    }
}
