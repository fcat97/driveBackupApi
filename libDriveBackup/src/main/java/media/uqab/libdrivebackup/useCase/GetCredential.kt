package media.uqab.libdrivebackup.useCase

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import media.uqab.libdrivebackup.BuildConfig
import java.util.*

internal object GetCredential {
    private const val TAG = "GetCredential"

    /**
     * Get [GoogleAccountCredential] from [Intent] data got from user consent request.
     */
    internal fun getCredential(activity: ComponentActivity, data: Intent?): GoogleAccountCredential? {
        if (data == null) return null

        try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

            if (BuildConfig.DEBUG) Log.i(
                TAG, "getCredential: \n" + """
                ${account.account}
                ${account.id}
                ${account.givenName}
                ${account.displayName}
                ${account.requestedScopes}
            """.trimIndent())

            val credential = GoogleAccountCredential.usingOAuth2(
                activity,
                Collections.singleton(DriveScopes.DRIVE_APPDATA)
            ).apply {
                selectedAccount = account.account
            }

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
}