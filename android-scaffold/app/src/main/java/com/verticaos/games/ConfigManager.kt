package com.verticaos.games

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("verticaos_prefs", Context.MODE_PRIVATE)

    fun getEnabledPackages(): Set<String> {
        return prefs.getStringSet("enabled_packages", null) ?: emptySet()
    }

    private fun setEnabledPackages(packages: Set<String>) {
        prefs.edit().putStringSet("enabled_packages", packages).apply()
    }

    fun isPackageEnabled(packageName: String): Boolean {
        // Se o conjunto estiver vazio e nunca foi salvo, por padrão consideramos desativado 
        // para que o usuário tenha controle total do que quer mostrar.
        val enabled = prefs.getStringSet("enabled_packages", null) ?: return false
        return enabled.contains(packageName)
    }

    fun togglePackage(packageName: String) {
        val current = getEnabledPackages().toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        setEnabledPackages(current)
    }

    fun getGameDelay(packageName: String): Int {
        val delay = prefs.getInt("delay_$packageName", -1)
        return if (delay == -1) 10 else delay
    }

    fun setGameDelay(packageName: String, delay: Int) {
        prefs.edit().putInt("delay_$packageName", delay).apply()
    }

    fun getPulsesPerCredit(packageName: String): Int {
        return prefs.getInt("pulses_$packageName", 1)
    }

    fun setPulsesPerCredit(packageName: String, pulses: Int) {
        prefs.edit().putInt("pulses_$packageName", pulses).apply()
    }

    /**
     * Retorna o modo de operação:
     * 0 = FREEPLAY (Jogos liberados, sem injeção de ficha)
     * 1 = MODO HABILIDADE (Requer créditos/fichas)
     */
    fun getOperationMode(): Int {
        return prefs.getInt("operation_mode", 1) // Padrão: Modo Habilidade
    }

    fun setOperationMode(mode: Int) {
        prefs.edit().putInt("operation_mode", mode).apply()
    }
}
