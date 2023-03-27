package media.uqab.libdrivebackup.useCase

import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import media.uqab.libdrivebackup.model.UserInfo

object GetSignedInEmail {
    /**
     * Return already signed in email address if present.
     *
     * @return Signed in email if present, null otherwise
     */
    fun getSignedInEmail(activity: ComponentActivity): UserInfo? {
        return GoogleSignIn.getLastSignedInAccount(activity)?.let {
            UserInfo(it.email, it.displayName)
        }
    }
}