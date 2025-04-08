package com.gichehafarm.registry.data

data class WorkerAttendance(
    val workNumber: String,
    val date: String,
    val present: Boolean,
    val surname: String,
    val firstName: String
)