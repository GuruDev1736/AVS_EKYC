package com.avs.avs_ekyc.Constant

import android.content.Context
import android.content.SharedPreferences

object SharedPreferenceManager {

    private const val PREF_NAME = "AVS"

    const val AGENT_NO = "agent_no"
    const val CUST_NO = "cust_no"
    const val USERNAME = "username"
    const val PASSWORD = "password"

    private lateinit var sharedPreferences: SharedPreferences

    // Initialize SharedPreferences (Call this in Application class or MainActivity)
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Save String value
    fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    // Get String value
    fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    // Save Boolean value
    fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    // Get Boolean value
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    // Save Int value
    fun saveInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    // Get Int value
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    // Save Long value
    fun saveLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    // Get Long value
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    // Save Float value
    fun saveFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    // Get Float value
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    // Remove a specific key
    fun removeKey(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    // Clear all data
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
