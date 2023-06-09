package media.uqab.libdrivebackup

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import media.uqab.libdrivebackup.model.*
import media.uqab.libdrivebackup.useCase.*
import media.uqab.libdrivebackup.useCase.GetCredential.getCredential
import media.uqab.libdrivebackup.useCase.GetOneTapSignInIntent.getSignInIntent
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    private val credentialID: String
) {
    init {
        if (activity.lifecycle.currentState != Lifecycle.State.INITIALIZED) {
            throw InitializationException("Must initialize before OnStart but initialized in ${activity.lifecycle.currentState}")
        }

        // attach an observer to cancel all running executor services
        activity.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    _executor?.shutdown()
                }
            }
        })

        if (credentialID.isBlank()) throw InitializationException("Credential ID not provided")
        if (appID.isEmpty()) throw InitializationException("App Name not provided")

        Constants.APP_NAME = appID
    }

    /**
     * A single threaded executor to run all the background tasks
     */
    private var _executor: ExecutorService? = null

    /**
     * Run this block when user grants drive uses permission. Must be set before
     * each operation.
     */
    private var onConsent: ((credential: GoogleAccountCredential) -> Unit)? = null

    /**
     * Run this block when any operation is failed.
     */
    private var onFailed: ((msg: Exception) -> Unit)? = null

    /**
     * Verified [GoogleAccountCredential].
     */
    private var credential: GoogleAccountCredential? = null

    /**
     * Launcher for user consent
     */
    private val consentLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val credential = getCredential(activity, it.data)

        if(it.resultCode == Activity.RESULT_OK && credential != null) {
            this.credential = credential // update credential

            onConsent?.invoke(credential)
        } else {
            onFailed?.invoke(UserPermissionDeniedException())
            Log.w(TAG, "user permission denied")
        }
    }

    /**
     * Fetch latest 10 uploaded file IDs.
     */
    fun getFiles(
        onFailed: ((Exception) -> Unit)? = null,
        result: (List<FileInfo>) -> Unit
    ) = requestConsentAndProceed(onFailed) {
        execute(onFailed) {
            try {
                val files = GetFiles.getFiles(it).files.map {
                    FileInfo(
                        fileID = it.id,
                        name = it.name,
                        extension = if (it.fileExtension != null) { it.fileExtension } else { "" },
                        mimeType = if (it.mimeType != null) it.mimeType else "",
                        lastModified = if (it.modifiedTime == null) null else Date(it.modifiedTime.value),
                        size = it.getSize() ?: 0,
                        webLink = it.webContentLink
                    )
                }

                activity.runOnUiThread {
                    result(files)
                }
            } catch (e: Exception) {
                Log.w(TAG, "failed to get files", e)

                activity.runOnUiThread {
                    onFailed?.invoke(e)
                }
            }
        }
    }

    /**
     * Get [FileInfo] of [fileID] if exists, otherwise [onFailed] is called.
     *
     * @param fileID id of file to query
     * @param onFailed called if any error occurs
     * @param result queried file's [FileInfo].
     */
    fun getFile(
        fileID: String,
        onFailed: ((Exception) -> Unit)? = null,
        result: (FileInfo) -> Unit
    ) = requestConsentAndProceed(onFailed) { c ->
        execute(onFailed) {
            try {
                val file = GetFile.getFile(fileID, c).let {
                    FileInfo(
                        fileID = it.id,
                        name = it.name,
                        extension = if (it.fileExtension != null) { it.fileExtension } else { "" },
                        mimeType = if (it.mimeType != null) it.mimeType else "",
                        lastModified = if (it.modifiedTime == null) null else Date(it.modifiedTime.value),
                        size = it.getSize() ?: 0,
                        webLink = it.webContentLink
                    )
                }

                activity.runOnUiThread { result(file) }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    onFailed?.invoke(e)
                }
            }
        }
    }

    /**
     * Upload a [java.io.File] to google drive's app specific folder.
     */
    fun uploadFile(
        file: File,
        mimeType: String,
        onFailed: ((Exception) -> Unit)? = null,
        onUpload: (fileID: String) -> Unit
    ) = requestConsentAndProceed(onFailed) {
        execute(onFailed) {
            try {
                val fileID = UploadAppData.uploadAppData(it, file, mimeType)

                activity.runOnUiThread {
                    onUpload(fileID)
                }
            } catch (e: Exception) {
                Log.w(TAG, "failed to upload file", e)
                activity.runOnUiThread {
                    onFailed?.invoke(e)
                }
            }
        }
    }

    /**
     * Download a file.
     *
     * @param fileID Google drive's file id. To get all uploaded files use [getFiles].
     * @param outputFile file where the output will be written. Must be accessible by user, such as
     * app's owned directory in SD card.
     * @param onDownload response when the file is completely downloaded and written to [outputFile].
     * @param onFailed called when this operations fails.
     */
    fun downloadFile(
        fileID: String,
        outputFile: File,
        onFailed: ((Exception) -> Unit)? = null,
        onDownload: (File) -> Unit
    ) = requestConsentAndProceed(onFailed) {
        execute(onFailed) {
            val fos = FileOutputStream(outputFile)
            try {
                val baOs = DownloadFile.downloadFile(it, fileID)

                baOs.writeTo(fos)

                activity.runOnUiThread {
                    onDownload(outputFile)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download file: $fileID", e)

                activity.runOnUiThread {
                    onFailed?.invoke(e)
                }
            } finally {
                fos.close()
            }
        }
    }

    /**
     * Create root folder in app data directory.
     *
     * ---
     * **Experimental api**
     */
    fun createRootFolder(
        onFailed: ((Exception) -> Unit)? = null,
        onCreate: (String) -> Unit
    ) = requestConsentAndProceed(onFailed) {
        execute(onFailed) {
            try {
                val folderID = CreateRootFolder.create(it)
                activity.runOnUiThread {
                    onCreate(folderID)
                }
            } catch (e: Exception) {
                Log.w(TAG, "failed to create root folder", e)

                activity.runOnUiThread {
                    onFailed?.invoke(e)
                }
            }
        }
    }

    /**
     * Delete a file from Drive.
     *
     * @param fileID file id to delete.
     * @param onDelete callback to invoke when file is deleted successfully.
     */
    fun deleteFile(
        fileID: String,
        onFailed: ((Exception) -> Unit)? = null,
        onDelete: () -> Unit
    ) = requestConsentAndProceed(onFailed) {
        execute(onFailed) {
            try {
                DeleteFile.delete(it, fileID)
                activity.runOnUiThread {
                    onDelete()
                }
            } catch (e: Exception) {
                activity.run {
                    onFailed?.invoke(e)
                }
                Log.w(TAG, "failed to delete file $fileID", e)
            }
        }
    }

    fun signOut(
        onFailed: ((Exception) -> Unit)?,
        onSuccess: () -> Unit
    ) {
        try {
            SignOutUser.signOut(activity, credentialID)
            credential = null
            onSuccess()
        } catch (e: Exception) {
            onFailed?.invoke(e)
        }
    }

    /**
     * Get currently logged in email if present
     *
     * @param currentUser lambda to return current logged in email address
     * @param onFailed [NoUserFoundException] will be sent if no user found
     */
    fun getCurrentUser(
        onFailed: ((Exception) -> Unit)?,
        currentUser: (userInfo: UserInfo) -> Unit
    ) {
        try {
            val info = GetSignedInEmail.getSignedInEmail(activity)
            if (info != null) currentUser(info)
            else onFailed?.invoke(NoUserFoundException())
        } catch (e: Exception) {
            onFailed?.invoke(e)
        }
    }

    fun signIn(
        onFailed: ((Exception) -> Unit)?,
        onSuccess: (UserInfo) -> Unit
    ) = requestConsentAndProceed(onFailed) {
        val info = GetSignedInEmail.getSignedInEmail(activity)
        if (info != null) onSuccess(info)
        else onFailed?.invoke(Exception(""))
    }

    /**
     * Request for User Consent to grant access to his/her drive account.
     *
     * @param onConsent action to do when user permission is granted. This returns a [Intent] object
     * if successful, otherwise null.
     */
    private fun requestConsentAndProceed(
        onFailed: ((Exception) -> Unit)? = null,
        onConsent: (GoogleAccountCredential) -> Unit
    ) {
        // use previous credential
        if (credential != null) {
            onConsent(credential!!)
            return
        }

        // set operation when user consent is approved.
        this.onConsent = onConsent

        // set operation when operation fails
        this.onFailed = onFailed

        // request for user permission
        val signInIntent = getSignInIntent(activity, credentialID)
        consentLauncher.launch(signInIntent)
    }

    /**
     * Execute the task on the executor.
     *
     */
    private fun execute(onFailed: ((Exception) -> Unit)?, task: Runnable) {
        try {
            getExecutor().submit(task)
        } catch (e: Exception) {
            activity.runOnUiThread {
                onFailed?.invoke(e)
            }
        }
    }

    private fun getExecutor(): ExecutorService {
        if (_executor == null) _executor = Executors.newSingleThreadExecutor()
        else if (_executor!!.isTerminated) _executor = Executors.newSingleThreadExecutor()

        return _executor!!
    }

    companion object {
        private const val TAG = "BackupManager"
    }
}