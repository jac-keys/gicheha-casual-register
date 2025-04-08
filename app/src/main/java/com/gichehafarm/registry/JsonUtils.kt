package com.gichehafarm.registry

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonUtils {
    private val gson = Gson()

    fun toJson(users: List<User>): String = gson.toJson(users)

    fun fromJson(json: String): List<User> {
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type)
    }
}
