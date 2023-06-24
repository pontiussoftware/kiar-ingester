package ch.pontius.kiar.cli

import ch.pontius.kiar.api.routes.session.MIN_LENGTH_PASSWORD
import ch.pontius.kiar.api.routes.session.MIN_LENGTH_USERNAME
import ch.pontius.kiar.api.model.session.Role
import ch.pontius.kiar.api.routes.session.SALT
import ch.pontius.kiar.database.institution.DbRole
import ch.pontius.kiar.database.institution.DbUser
import ch.pontius.kiar.utilities.validatePassword
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.jakewharton.picnic.table
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull
import org.mindrot.jbcrypt.BCrypt

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
/**
 * A collection of [CliktCommand]s for [DbUser] management
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class UserCommand(store: TransientEntityStore) : NoOpCliktCommand(name = "user", help = "Commands surrounding the management of users.", printHelpOnEmptyArgs = true) {
    init {
        this.subcommands(Create(store), Delete(store), List(store), Roles(store) )
    }

    override fun aliases() = mapOf(
        "ls" to listOf("list"),
        "remove" to listOf("delete"),
        "rm" to listOf("delete"),
        "drop" to listOf("delete"),
        "add" to listOf("create")
    )

    /**
     * [CliktCommand] to create a new [DbUser].
     */
    inner class Create(private val store: TransientEntityStore) : CliktCommand(name = "create", help = "Creates a new user.", printHelpOnEmptyArgs = true) {
        /** The name of the newly created user. */
        private val username: String by option("-u", "--username", help = "Username of at least $MIN_LENGTH_USERNAME characters length. Must be unique!")
            .required()
            .validate { require(it.length >= MIN_LENGTH_USERNAME) { "Username must consist of at least $MIN_LENGTH_USERNAME characters." } }

        /** The password of the newly created user. */
        private val password: String by option("-p", "--password", help = "Password of at least $MIN_LENGTH_PASSWORD characters length.")
            .required()
            .validate { require(it.length >= MIN_LENGTH_PASSWORD) { "Password must consist of at least $MIN_LENGTH_PASSWORD characters." } }

        /** The desired [DbRole] of the newly created user. */
        private val role: Role by option("-r", "--role", help = "Role of the new user.").convert { Role.valueOf(it) }.required()

        override fun run() {
            val username = this.store.transactional {
                if (!this@Create.password.validatePassword()) {
                    System.err.println("Invalid password. Password must consist of printable ASCII characters and have at least a length of eight characters and it must contain at least one upper- and lowercase letter and one digit.")
                    return@transactional
                }

                val user = DbUser.new {
                    name = this@Create.username.lowercase()
                    password = BCrypt.hashpw(this@Create.password, SALT)
                    inactive = false
                    role = this@Create.role.toDb()
                }
                user.name
            }
            println("New user '$username' created.")
        }
    }

    /**
     * [CliktCommand] to delete a [DbUser].
     */
    inner class Delete(private val store: TransientEntityStore) : CliktCommand(name = "delete", help = "Deletes an existing user.", printHelpOnEmptyArgs = true) {
        private val username: String by option("-u", "--username", help = "Username of the user to be deleted.").required()
        override fun run() {
            this.store.transactional {
                val user = DbUser.filter { it.name eq this@Delete.username }.firstOrNull()
                if (user == null) {
                    println("User could not be deleted because it doesn't exist!")
                } else {
                    user.email = null /* Users are not actually deleted; just inactivated. */
                    user.institution = null
                    user.inactive = true
                }
            }
        }
    }

    /**
     * [CliktCommand] to list all [DbUser]s.
     */
    inner class List(private val store: TransientEntityStore): CliktCommand(name = "list", help = "Lists all Users") {
        override fun run() = this.store.transactional(true) {
            val users = DbUser.filter { it.inactive eq false }.asSequence()
            println("Available users:")
            println(
                table {
                    cellStyle {
                        border = true
                        paddingLeft = 1
                        paddingRight = 1
                    }
                    header {
                        row("id", "username", "role")
                    }
                    body {
                        users.forEach {
                            row(it.name, it.role)
                        }
                    }
                }
            )
        }
    }

    /**
     * [CliktCommand] to list all [DbRole]s.
     */
    inner class Roles(private val store: TransientEntityStore): CliktCommand(name = "roles", help = "Lists all available roles.") {
        override fun run() = this.store.transactional(true) {
            println("Available roles: ${Role.values().joinToString(", ")}")
        }
    }
}