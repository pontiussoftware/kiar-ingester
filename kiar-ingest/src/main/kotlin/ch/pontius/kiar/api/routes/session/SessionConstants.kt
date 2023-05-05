package ch.pontius.kiar.api.routes.session

import org.mindrot.jbcrypt.BCrypt

/** Name of the session constant used to store the user ID. */
const val SESSION_USER_ID = "USER_ID"

/** Name of the session constant used to store the user ID. */
const val SESSION_USER_NAME = "USER_NAME"

/** The minimum length of a password. */
const val MIN_LENGTH_PASSWORD = 8

/** The minimum length of a username. */
const val MIN_LENGTH_USERNAME = 5

/** The salt used for password encryption. */
val SALT = BCrypt.gensalt()