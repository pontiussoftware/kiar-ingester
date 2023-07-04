package ch.pontius.kiar.utilities

import ch.pontius.kiar.api.routes.session.MIN_LENGTH_PASSWORD

/** A [Regex] to validate e-mail addresses. */
private val EMAILREGEX = Regex("^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*(\\.\\w{2,3})+$")

/**
 * Generates a new, random password of given length.
 *
 * @param length The length of the password.
 */
fun generatePassword(length: Int): String {
    val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..length).map { chars.random() }.joinToString("")
}

/**
 * Validates a password; it must contain at least 8 digits, an upper- and lowercase letter, a number and a special character
 */
fun String.validatePassword(): Boolean = this.length > MIN_LENGTH_PASSWORD &&
    Regex("^\\p{ASCII}*$").containsMatchIn(this) &&
    Regex("[A-Z]").containsMatchIn(this) &&
    Regex("[a-z]").containsMatchIn(this) &&
    Regex("[0-9]").containsMatchIn(this)

/**
 * Validates an e-mail address.
 *
 * @return True if this [String] is a valid e-mail address.
 */
fun String.validateEmail(): Boolean = EMAILREGEX.matches(this)