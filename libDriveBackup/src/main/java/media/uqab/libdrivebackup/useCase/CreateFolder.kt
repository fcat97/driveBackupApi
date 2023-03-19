package media.uqab.libdrivebackup.useCase

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.model.File

internal object CreateRootFolder {
    fun create(credential: GoogleAccountCredential): String {
        val fileMetadata = File()
        fileMetadata.name = "Test"
        fileMetadata.mimeType = "application/vnd.google-apps.folder"
        fileMetadata.parents = listOf("appDataFolder")

        val file: File = getService(credential).files()
            .create(fileMetadata)
            .setFields("id")
            .execute()

        return file.id
    }
}