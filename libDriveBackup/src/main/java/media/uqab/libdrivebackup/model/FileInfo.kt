package media.uqab.libdrivebackup.model

import java.util.Date

/**
 * Data class containing Drive's file info
 */
data class FileInfo(
    val fileID: String,
    val name: String,
    val extension: String,
    val mimeType: String,
    val lastModified: Date?,
    val size: Long,
    val webLink: String?,
)