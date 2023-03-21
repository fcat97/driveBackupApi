package com.tos.drivebackup.drive_backup

import com.google.gson.JsonObject

data class Backup(
    val app_version: Int,
    val data: JsonObject,
)