package com.tos.drivebackup.drive_backup

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.tos.drivebackup.drive_backup.BackupSchema.CURRENT_BACKUP_SCHEMA_VER
import com.tos.drivebackup.drive_backup.BackupSchema.getBackupFile
import media.uqab.libdrivebackup.GoogleDriveBackupManager
import media.uqab.libdrivebackup.model.FileInfo
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*

object DriveBackupUtils {
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
                    response("failed to save backup! ${it.message}")
                    Log.e(TAG, "backupToDrive: failed to save backup!", it)
                }
            ) { fileID ->
                response("Backup successful")
                Log.d("DriveBackupUtils", "backupToGoogleDrive: $fileID")
            }
        }
    }

    fun restoreBackup(
        context: Context,
        backupManager: GoogleDriveBackupManager,
        response: (String) -> Unit
    ) {
        backupManager.getFiles(
            onFailed = {
                response("failed to restore backup ${it.stackTraceToString()}")
            }
        ) {
            if (it.isEmpty()) {
                response("No backup found")
                return@getFiles
            }

            // log
            response("[restore]:\n$it")

            val lastBackUp = it.filter { f ->
                isValidBackupFile(f.name) && getBackupVersion(f.name) == CURRENT_BACKUP_SCHEMA_VER
            }.maxByOrNull { f -> getBackupTime(f.name) }

            if (lastBackUp == null) {
                response("no backup found to restore")
                return@getFiles
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

                        val success = when(getBackupVersion(f.name)) {
                            1 -> Restore.restoreForSchemaVer1(context, json)
                            else -> false
                        }

                        if (success) {
                            response("backup restored successfully")
                        } else {
                            response("something went wrong.")
                        }
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
        return "backup_schema\\d+_\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}.json".toRegex().containsMatchIn(fileName)
    }
    private fun getBackupTime(fileName: String): Long {
        val date = "\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}".toRegex().find(fileName)?.value ?: return 0
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.ENGLISH)
        return sdf.parse(date)?.time ?: 0
    }
    private fun getBackupVersion(fileName: String): Int {
        return "backup_schema\\d+".toRegex().find(fileName)?.value?.drop("backup_schema".length)?.toIntOrNull() ?: 0
    }

    private const val TAG = "DriveBackupUtils"
}