package media.uqab.libdrivebackup

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import media.uqab.libdrivebackup.model.Constants
import media.uqab.libdrivebackup.model.FileInfo
import media.uqab.libdrivebackup.model.InitializationException
import media.uqab.libdrivebackup.useCase.CreateRootFolder
import media.uqab.libdrivebackup.useCase.DeleteFile
import media.uqab.libdrivebackup.useCase.GetCredential.getCredential
import media.uqab.libdrivebackup.useCase.GetOneTapSignInIntent.getSignInIntent
import media.uqab.libdrivebackup.useCase.GetFiles
import media.uqab.libdrivebackup.useCase.UploadAppData
import java.util.*

/**
 * A manager class to view, update, delete and modify
 * Google Drive's app specific folder backup.
 *
 * @param appID Name of the application that's using it i.e. com.example.app.
 * @param activity [ComponentActivity] or any of it's subclasses to handle all operations.
 * @param credentialID "Web application's" Client ID from "OAuth 2.0 Client IDs".
 *
 * @throws InitializationException if this class is initialized after [Lifecycle.State.STARTED].
 *
 * @see "https://developers.google.com/drive/api/guides/appdata"
 *
 * @author github/fCat97
 */
class GoogleDriveBackupManager(
    appID: String,
    private val activity: ComponentActivity,
    private val credentialID: String,
) {
    init {
        if (activity.lifecycle.currentState != Lifecycle.State.INITIALIZED) {
            throw InitializationException("Must initialize before OnStart but initialized in ${activity.lifecycle.currentState}")
        }

        if (credentialID.isBlank()) throw InitializationException("Credential ID not provided")
        if (appID.isEmpty()) throw InitializationException("App Name not provided")

        Constants.APP_NAME = appID
    }

    /**
     * Run this block when user grants drive uses permission. Must be set before
     * each operation.
     */
    private var onConsent: ((credential: GoogleAccountCredential) -> Unit)? = null
    private val consentLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val credential = getCredential(activity, it.data)

        if(it.resultCode == Activity.RESULT_OK && credential != null) {
            onConsent?.invoke(credential)
        } else {
            Log.w(TAG, "user permission denied")
        }
    }

    /**
     * Fetch latest 10 backup file IDs. Returns an empty list if any exception occurs.
     */
    fun getBackupIDs(backups: (List<FileInfo>) -> Unit) = requestConsentAndProceed {
        Thread {
            try {
                val files = GetFiles.getFiles(it).files.map {
                    FileInfo(
                        fileID = it.id,
                        name = it.name,
                        extension = if (it.fileExtension != null) { it.fileExtension } else { "" },
                        mimeType = if (it.mimeType != null) it.mimeType else "",
                        lastModified = if (it.modifiedTime == null) null else Date(it.modifiedTime.value),
                        size = it.getSize() ?: 0
                    )
                }
                activity.runOnUiThread { backups(files) }
            } catch (e: Exception) {
                Log.w(TAG, "failed to get files", e)
                backups(emptyList())
            }
        }.start()
    }

    /**
     * Upload a [java.io.File] to google drive's app specific folder.
     */
    fun uploadFile(file: java.io.File, mimeType: String, onUpload: (fileID: String) -> Unit) = requestConsentAndProceed {
        Thread {
            try {
                val fileID = UploadAppData.uploadAppData(it, file, mimeType)
                activity.runOnUiThread { onUpload(fileID) }
            } catch (e: Exception) {
                Log.w(TAG, "failed to upload file", e)
            }
        }.start()
    }

    /**
     * Create root folder in app data directory.
     *
     * ---
     * **Experimental api**
     */
    fun createRootFolder(onCreate: (String) -> Unit) = requestConsentAndProceed {
        Thread {
            try {
                val folderID = CreateRootFolder.create(it)
                activity.runOnUiThread { onCreate(folderID) }
            } catch (e: Exception) {
                Log.w(TAG, "failed to create root folder", e)
            }
        }.start()
    }

    /**
     * Delete a file from Drive.
     *
     * @param fileID file id to delete.
     * @param onDelete callback to invoke when file is deleted successfully.
     */
    fun deleteFile(fileID: String, onDelete: () -> Unit) = requestConsentAndProceed {
        Thread {
            try {
                DeleteFile.delete(it, fileID)
                activity.runOnUiThread { onDelete() }
            } catch (e: Exception) {
                Log.w(TAG, "failed to delete file $fileID", e)
            }
        }.start()
    }

    /**
     * Request for User Consent to grant access to his/her drive account.
     *
     * @param onConsent action to do when user permission is granted. This returns a [Intent] object
     * if successful, otherwise null.
     */
    private fun requestConsentAndProceed(onConsent: (GoogleAccountCredential) -> Unit) {
        // set operation when user consent is approved.
        this.onConsent = onConsent

        val signInIntent = getSignInIntent(activity, credentialID)
        consentLauncher.launch(signInIntent)
    }

    companion object {
        private const val TAG = "BackupManager"
    }
}