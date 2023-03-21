package com.tos.drivebackup.drive_backup

import android.content.Context
import android.util.Log
import com.google.gson.Gson
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

    internal fun getBackupFile(context: Context): File? {
        val backup = getForAppVersion93(context)

        val today = with(SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH)) { format(Date()) }
        val data = GsonBuilder().setPrettyPrinting().create().toJson(backup, Backup::class.java)
        val file = File(context.cacheDir, "backup_app${BuildConfig.VERSION_CODE}_$today.json")

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
    private fun getForAppVersion92(context: Context): Backup {
        return Backup(92, JsonObject())
    }

    private fun getForAppVersion93(context: Context): Backup {

        val backupData = JsonObject().apply {
            addProperty("is_touch_enabled", true)
            addProperty("dark_mode", false)
            addProperty("selected_print", "demo_print")
            addProperty("last_read", 100)
            addProperty("bookmarks", "[]")
            addProperty("reciter", "maher")
        }

        return Backup(BuildConfig.VERSION_CODE, backupData)
    }
}