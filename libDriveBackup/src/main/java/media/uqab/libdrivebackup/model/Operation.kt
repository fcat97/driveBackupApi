package media.uqab.libdrivebackup.model

internal enum class Operation(val title: String) {
    UPLOAD_FILE("upload_file"),
    READ_FILE_LIST("read_uploaded")
}