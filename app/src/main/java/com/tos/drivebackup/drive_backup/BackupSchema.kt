package com.tos.drivebackup.drive_backup

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.tos.drivebackup.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

internal object BackupSchema {
    private const val TAG = "Schema"
    const val CURRENT_BACKUP_SCHEMA_VER = 1

    internal fun getBackupFile(context: Context): File? {
        val backup = when(CURRENT_BACKUP_SCHEMA_VER) {
            1 -> getForSchemaVer1(context)
            else -> null
        } ?: return null // return if schema version not matched.

        val today = with(SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH)) { format(Date()) }
        val data = GsonBuilder().setPrettyPrinting().create().toJson(backup, Backup::class.java)
        val file = File(context.cacheDir, "backup_schema${CURRENT_BACKUP_SCHEMA_VER}_$today.json")

        return try {
            val fos = FileOutputStream(file)
            fos.write(data.toByteArray(Charset.defaultCharset()))
            fos.close()
            file
        } catch (e: Exception) {
            Log.d(TAG, "createBackupFile: failed to write")
            null
        }
    }

    // ----------------- demo --------------------- //
    // keep each backup version in separate method, this record will make the debug process easy.
    // also write a restore method in [Restore] class for this version.
    private fun getForSchemaVer0(context: Context): Backup {
        return Backup(0, JsonObject())
    }

    private fun getForSchemaVer1(context: Context): Backup {

        val backupData = JsonObject().apply {
            addProperty("is_touch_enabled", true)
            addProperty("dark_mode", false)
            addProperty("selected_print", "demo_print")
            addProperty("last_read", 100)
            addProperty("bookmarks", "[]")
            addProperty("reciter", "maher")
        }

        return Backup(1, backupData)
    }
}