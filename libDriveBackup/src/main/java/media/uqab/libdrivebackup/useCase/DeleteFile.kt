package media.uqab.libdrivebackup.useCase

import androidx.annotation.WorkerThread
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.io.IOException

/**
 * Class to delete files from the application data folder.
 */
internal object DeleteFile {
    /**
     * Delete a file from the application data folder.
     *
     * @throws [IOException], [IllegalStateException], [GoogleJsonResponseException]
     */
    @Throws(IOException::class, IllegalStateException::class, GoogleJsonResponseException::class)
    @WorkerThread
    fun delete(credentials: GoogleAccountCredential, fileId: String) {
        getService(credentials).files().delete(fileId).execute()
    }
}