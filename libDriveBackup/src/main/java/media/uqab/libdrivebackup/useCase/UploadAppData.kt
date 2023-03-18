package media.uqab.libdrivebackup.useCase

import android.util.Log
import androidx.annotation.WorkerThread
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.IOException


/**
 * Class to create file in the application data folder.
 */
internal object UploadAppData {
    /**
     * Creates a file in the application data folder.
     *
     * @return Created file's Id.
     *
     * @throws [IOException], [IllegalStateException], [GoogleJsonResponseException]
     */
    @Throws(IOException::class, IllegalStateException::class, GoogleJsonResponseException::class)
    @WorkerThread
    fun uploadAppData(
        credentials: GoogleAccountCredential,
        uploadingFile: java.io.File,
        mimeType: String
    ): String {

        /*Log.d(
            TAG, "uploadAppData: \n" + """
            credential: $credentials
            fileName: ${uploadingFile.name}
            mimeType: $mimeType
        """.trimIndent())*/

        // File's metadata.
        val fileMetadata = File()
        fileMetadata.name = uploadingFile.name
        fileMetadata.parents = listOf("appDataFolder")
        val mediaContent = FileContent(mimeType, uploadingFile)
        val file = getService(credentials).files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()

        /*println("File ID: " + file.id)*/

        return file.id
    }
}