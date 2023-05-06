package ch.pontius.kiar.utilities

/**
 * Generates a new, random password of given length.
 *
 * @param length The length of the password.
 */
fun generatePassword(length: Int): String {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..length).map { chars.random() }.joinToString("")
}