package media.uqab.libdrivebackup.useCase

import android.content.Intent
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

internal object GetOneTapSignInIntent {
    /**
     * Get intent to show one tap sign-in dialog
     *
     * @param activity [ComponentActivity]
     * @param credentialID "WEB_APPLICATION" Credential ID of API from Google console's credential.
     *
     * @return [Intent] to launch the one tap sign-in screen.
     */
    fun getSignInIntent(activity: ComponentActivity, credentialID: String): Intent {
        val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(credentialID)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

        val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(activity, gso)
        return googleSignInClient.signInIntent
    }
}