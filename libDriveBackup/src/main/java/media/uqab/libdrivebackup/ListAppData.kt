package media.uqab.libdrivebackup

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.IOException
import java.util.*

/**
 * Class to demonstrate use-case of list 10 files in the application data folder.
 */
object ListAppData {
    /**
     * list down files in the application data folder.
     *
     * @return list of 10 files.
     */
    @Throws(IOException::class)
    fun listAppData(
        credentials: GoogleAccountCredential
    ): FileList {
        // Build a new authorized API client service.
        val service = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credentials
        )
            .setApplicationName("Drive Backup Demo")
            .build()
        return try {
            val files = service.files().list()
                .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name)")
                .setPageSize(10)
                .execute()

            val output = files.files.joinToString { "${it.name}, ${it.id}\n" }
            SignInActivity.terminalOutput.postValue("[output]: \n$output")

            files
        } catch (e: GoogleJsonResponseException) {
            // TODO(developer) - handle error appropriately
            System.err.println("Unable to list files: " + e.details)
            throw e
        }
    }
}