package com.example.drugdetector

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUser(email: String, token: String? = null) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("operator_email", email)
            .putString("id_token", token)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun getOperatorEmail(): String? = prefs.getString("operator_email", null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}