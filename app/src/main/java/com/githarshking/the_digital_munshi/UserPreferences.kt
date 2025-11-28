package com.githarshking.the_digital_munshi

import android.content.Context

object UserPreferences {
    private const val PREF_NAME = "munshi_user_prefs"
    private const val KEY_NAME = "user_name"
    private const val KEY_OCCUPATION = "user_occupation"
    private const val KEY_DESCRIPTION = "user_desc"
    private const val KEY_IS_ONBOARDED = "is_onboarded"

    fun saveUser(context: Context, name: String, occupation: String, desc: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString(KEY_NAME, name)
            putString(KEY_OCCUPATION, occupation)
            putString(KEY_DESCRIPTION, desc)
            putBoolean(KEY_IS_ONBOARDED, true)
            apply()
        }
    }

    fun getUserDetails(context: Context): Triple<String, String, String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return Triple(
            prefs.getString(KEY_NAME, "User") ?: "User",
            prefs.getString(KEY_OCCUPATION, "General Worker") ?: "General Worker",
            prefs.getString(KEY_DESCRIPTION, "") ?: ""
        )
    }

    fun isOnboarded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_ONBOARDED, false)
    }
}