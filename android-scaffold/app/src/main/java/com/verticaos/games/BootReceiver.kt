package com.verticaos.games

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-launches the arcade frontend when the device powers on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(launch)
        context.startService(Intent(context, GameMonitorService::class.java))
    }
}
