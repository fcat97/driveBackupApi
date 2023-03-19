package media.uqab.libdrivebackup

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import media.uqab.libdrivebackup.model.InitializationException
import media.uqab.libdrivebackup.useCase.CreateRootFolder
import media.uqab.libdrivebackup.useCase.DeleteFile
import media.uqab.libdrivebackup.useCase.ListAppData
import media.uqab.libdrivebackup.useCase.UploadAppData
import java.util.*

/**
 * A manager class to view, update, delete and modify
 * Google Drive's app specific folder backup.
 *
 * @param activity [ComponentActivity] to handle all operations.
 * @param credentialID "Web application's" Client ID from "OAuth 2.0 Client IDs".
 *
 * @throws InitializationException if this class is initialized after [Lifecycle.State.STARTED].
 *
 * @see "https://developers.google.com/drive/api/guides/appdata"
 *
 * @author github/fCat97
 */
class GoogleDriveBackupManager(
    private val activity: ComponentActivity,
    private val credentialID: String,
) {
    init {
        if (activity.lifecycle.currentState != Lifecycle.State.INITIALIZED) {
            throw InitializationException("Must initialize before OnStart but initialized in ${activity.lifecycle.currentState}")
        }
    }

    /**
     * Run this block when user grants drive uses permission. Must be set before
     * each operation.
     */
    private var onConsent: ((credential: GoogleAccountCredential) -> Unit)? = null
    private val consentLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val credential = getCredential(it.data)

        if(it.resultCode == Activity.RESULT_OK && credential != null) {
            onConsent?.invoke(credential)
        } else {
            Log.w(TAG, "user permission denied")
        }
    }

    /**
     * Fetch latest 10 backup file IDs. Returns an empty list if any exception occurs.
     */
    fun getBackupIDs(backups: (List<String>) -> Unit) = requestConsentAndProceed {
        Thread {
            try {
                val files = ListAppData.listAppData(it)
                activity.runOnUiThread { backups(files.files.map { f -> f.id }) }
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
        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(credentialID)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

        // set operation when user consent is approved.
        this.onConsent = onConsent

        // launch consent request
        val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(activity, gso)
        consentLauncher.launch(googleSignInClient.signInIntent)
    }

    /**
     * Get [GoogleAccountCredential] from [Intent] data got from user consent request.
     */
    private fun getCredential(data: Intent?): GoogleAccountCredential? {
        if (data == null) return null

        try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

            if (BuildConfig.DEBUG) Log.i(TAG, "handleSignInResult: \n" + """
                ${account.account}
                ${account.id}
                ${account.givenName}
            """.trimIndent())
            // Signed in successfully, show authenticated UI.

            val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
                activity,
                Collections.singleton(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccountName = account.email
            return credential
        } catch (e: Exception) {
            if(e is ApiException && e.statusCode == 10) {
                Log.e(TAG, "Not configured properly. Maybe you used wrong credential.")
            } else {
                Log.e(TAG, "Failed to get credential", e)
            }
            return null
        }
    }

    companion object {
        private const val TAG = "BackupManager"
    }
}