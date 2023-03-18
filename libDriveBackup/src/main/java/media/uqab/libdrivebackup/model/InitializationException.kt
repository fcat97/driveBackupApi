package media.uqab.libdrivebackup.model

/**
 * Thrown when a class can't be initialized
 */
data class InitializationException(val msg: String): Exception(msg)