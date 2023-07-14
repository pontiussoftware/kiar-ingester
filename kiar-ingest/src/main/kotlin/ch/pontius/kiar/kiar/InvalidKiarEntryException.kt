package ch.pontius.kiar.kiar

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class InvalidKiarEntryException(path: String): IllegalStateException("Unsupported suffix for KIAR entry: $path.")