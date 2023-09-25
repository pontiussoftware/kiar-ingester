package ch.pontius.kiar.utilities

/**
 * Adds a suffix to a string if it does not already end with it.
 *
 * @param suffix The suffix to add.
 * @return The string with the suffix.
 */
fun String.withSuffix(suffix: String): String = if (this.endsWith(suffix)) this else "$this$suffix"