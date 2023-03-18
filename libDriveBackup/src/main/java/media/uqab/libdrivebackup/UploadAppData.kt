package media.uqab.libdrivebackup

import android.util.Log
import androidx.annotation.WorkerThread
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.IOException
import java.util.*


/**
 * Class to demonstrate use-case of create file in the application data folder.
 */
object UploadAppData {
    private const val TAG = "UploadAppData"
    private const val CREDENTIALS_FILE_PATH = "/credentials.json"

    /**
     * Creates a file in the application data folder.
     *
     * @return Created file's Id.
     */
    @Throws(IOException::class, IllegalStateException::class)
    @WorkerThread
    fun uploadAppData(
        credentials: GoogleAccountCredential,
        uploadingFile: java.io.File,
        mimeType: String
    ): String {
        Log.d(TAG, "uploadAppData: \n" + """
            $credentials
            ${uploadingFile.name}
            $mimeType
        """.trimIndent())

        // Build a new authorized API client service.
        val service = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credentials
        )
            .setApplicationName("Drive Backup Demo")
            .build()

        return try {
            // File's metadata.
            val fileMetadata = File()
            fileMetadata.name = uploadingFile.name
            fileMetadata.parents = listOf("appDataFolder")
            val mediaContent = FileContent(mimeType, uploadingFile)
            val file = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            println("File ID: " + file.id)

            SignInActivity.terminalOutput.postValue( "[output]: \n" + file.id)

            file.id
        } catch (e: GoogleJsonResponseException) {
            Log.d(TAG, "Unable to create file: " + e.details)
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "uploadAppData: ${e.message}")
            e.printStackTrace()
            "failed"
        }
    }
}