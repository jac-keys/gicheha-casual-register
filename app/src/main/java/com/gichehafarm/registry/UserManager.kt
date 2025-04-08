package com.gichehafarm.registry

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow

data class User(val username: String, val password: String, val role: String)

class UserManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val PREF_KEY_NAME = "preferred_name"
        private const val USER_KEY = "users"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preferredNameFlow = MutableStateFlow(getPreferredName())
    val preferredNameFlow: StateFlow<String> = _preferredNameFlow.asStateFlow()

    // Registers a new user; returns false if the user already exists
    fun registerUser(username: String, password: String, role: String): Boolean {
        val existingUsers = getUsers().toMutableList()
        if (existingUsers.any { it.username == username }) return false // User already exists

        existingUsers.add(User(username, password, role))
        saveUsers(existingUsers)
        return true
    }

    // Authenticates user and returns their role if credentials match
    fun authenticateUser(username: String, password: String): String? {
        return getUsers().find { it.username == username && it.password == password }?.role
    }

    // Fetches stored users from SharedPreferences
    private fun getUsers(): List<User> {
        val usersJson = sharedPreferences.getString(USER_KEY, "[]")
        return usersJson?.let { JsonUtils.fromJson(it) } ?: emptyList()
    }

    // Saves users to SharedPreferences
    private fun saveUsers(users: List<User>) {
        val usersJson = JsonUtils.toJson(users)
        sharedPreferences.edit().putString(USER_KEY, usersJson).apply()
    }

    // Returns the stored preferred name or default "HR"
    fun getPreferredName(): String {
        return sharedPreferences.getString(PREF_KEY_NAME, "HR") ?: "HR"
    }

    // Provides a flow to observe preferred name changes
    fun getPreferredNameFlow(): Flow<String> {
        return preferredNameFlow
    }

    // Updates preferred name and triggers flow update
    fun savePreferredName(name: String) {
        sharedPreferences.edit().putString(PREF_KEY_NAME, name).apply()
        _preferredNameFlow.value = name
    }
}
