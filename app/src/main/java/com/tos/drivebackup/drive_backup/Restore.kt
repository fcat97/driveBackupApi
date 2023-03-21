package com.tos.drivebackup.drive_backup

import android.content.Context
import com.tos.drivebackup.drive_backup.Backup

internal object Restore {
    internal fun restoreForSchemaVer1(context: Context, backup: Backup): Boolean {
        val is_touch_enabled = backup.data.get("is_touch_enabled").asBoolean
        val dark_mode = backup.data.get("dark_mode").asBoolean
        val selected_print = backup.data.get("selected_print").asString
        val last_read = backup.data.get("last_read").asInt
        val bookmarks = backup.data.get("bookmarks").asString
        val reciter = backup.data.get("reciter").asString

        return true
    }
}