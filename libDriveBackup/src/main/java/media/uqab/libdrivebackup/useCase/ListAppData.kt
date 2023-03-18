package media.uqab.libdrivebackup.useCase

import androidx.annotation.WorkerThread
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import java.io.IOException
import java.util.*

/**
 * Class to demonstrate use-case of list 10 files in the application data folder.
 */
internal object ListAppData {
    /**
     * list down files in the application data folder.
     *
     * @return list of 10 files.
     * @throws [IOException], [IllegalStateException], [GoogleJsonResponseException]
     */
    @Throws(IOException::class, IllegalStateException::class, GoogleJsonResponseException::class)
    @WorkerThread
    fun listAppData(
        credentials: GoogleAccountCredential
    ): FileList {
        return getService(credentials).files().list()
            .setSpaces("appDataFolder")
            .setFields("nextPageToken, files(id, name)")
            .setPageSize(10)
            .execute()
    }
}