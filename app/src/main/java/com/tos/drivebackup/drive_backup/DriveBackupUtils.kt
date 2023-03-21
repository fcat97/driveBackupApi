package com.tos.drivebackup.drive_backup

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.tos.drivebackup.BuildConfig
import com.tos.drivebackup.drive_backup.BackupSchema.getBackupFile
import media.uqab.libdrivebackup.GoogleDriveBackupManager
import media.uqab.libdrivebackup.model.FileInfo
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.List

object DriveBackupUtils {
    @RequiresApi(api = 21)
    fun backupToDrive(
        context: Context,
        backupManager: GoogleDriveBackupManager,
        response: (String) -> Unit
    ) {
        val file = getBackupFile(context)
        if (file == null) {
            response("failed to generate backup!")
            return
        } else {
            response("[backupToDrive]: \n" + file.getFileContent())

            backupManager.uploadFile(
                file = file,
                mimeType = "application/json",
                onFailed = {
                    response("failed to save backup! ${it.stackTrace}")
                    Log.e(TAG, "backupToDrive: failed to save backup!", it)
                }
            ) { fileID ->
                response("Backup successful")
                Log.d("DriveBackupUtils", "backupToGoogleDrive: $fileID")
            }
        }
    }

    @RequiresApi(api = 21)
    fun restoreBackup(
        context: Context,
        backupManager: GoogleDriveBackupManager,
        response: (String) -> Unit
    ) {
        backupManager.getBackupIDs(
            onFailed = {
                response("failed to restore backup ${it.message}")
            }
        ) {
            if (it.isEmpty()) {
                response("No backup found")
                return@getBackupIDs
            }

            // log
            response("[restore]:\n$it")

            val lastBackUp = it.filter { f ->
                isValidBackupFile(f.name)
            }.filter { f ->
                val version = getBackupVersion(f.name)

                response("restoreBackup: $version")

                if (version == 0) {
                    false
                } else {
                    // filter backup that are equal or below current app version
                    version <= BuildConfig.VERSION_CODE
                }
            }.maxByOrNull { f -> getBackupTime(f.name) }

            if (lastBackUp == null) {
                response("no backup found to restore")
                return@getBackupIDs
            }

            response(lastBackUp.toString())

            // file to save downloaded backup
            val outputFile = File(context.cacheDir, lastBackUp.name)

            backupManager.downloadFile(
                lastBackUp.fileID,
                outputFile,
                onFailed = { e ->
                    response("failed to download backup ${e.message}")
                    Log.e(TAG, "restoreBackup: failed", e)
                },
                onDownload = { f ->
                    try {
                        val reader = FileReader(f)
                        val json = Gson().fromJson(reader, Backup::class.java)
                        response("[backup downloaded]:\n$json")

                        response("backup restored successfully")
                    } catch (e: Exception) {
                        response("failed to restore")
                    }
                }
            )
        }
    }

    private fun File.getFileContent(): String {
        val reader = FileReader(this)
        val string = reader.readText()
        reader.close()
        return string
    }

    private fun List<FileInfo>.toString(): String {
        val sb = StringBuilder()
        forEach { sb.appendLine("${it.name} ${it.fileID}") }
        return sb.toString()
    }

    private fun isValidBackupFile(fileName: String): Boolean {
        return "backup_app\\d+_\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}.json".toRegex().containsMatchIn(fileName)
    }
    private fun getBackupTime(fileName: String): Long {
        val date = "\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}".toRegex().find(fileName)?.value ?: return 0
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH)
        return sdf.parse(date)?.time ?: 0
    }
    private fun getBackupVersion(fileName: String): Int {
        return "_app\\d+".toRegex().find(fileName)?.value?.drop("_app".length)?.toIntOrNull() ?: 0
    }

    private const val TAG = "DriveBackupUtils"
}