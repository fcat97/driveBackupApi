package media.uqab.libdrivebackup.useCase

import androidx.annotation.WorkerThread
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.model.FileList
import java.io.IOException

/**
 * Class to list 10 files in the application data folder.
 */
internal object GetFiles {
    /**
     * list down files in the application data folder.
     *
     * @return list of 10 files.
     * @throws [IOException], [IllegalStateException], [GoogleJsonResponseException]
     */
    @Throws(IOException::class, IllegalStateException::class, GoogleJsonResponseException::class)
    @WorkerThread
    fun getFiles(credentials: GoogleAccountCredential): FileList {
        return getService(credentials).files().list()
            .setSpaces("appDataFolder")
            .setFields("nextPageToken, files(id, name)")
            .setPageSize(10)
            .execute()
    }
}