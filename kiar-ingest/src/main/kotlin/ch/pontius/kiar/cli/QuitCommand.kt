package ch.pontius.kiar.cli

import ch.pontius.kiar.ingester.IngesterServer
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import kotlin.system.exitProcess

/**
 * A CLI command that can be used to end the server.
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
class QuitCommand(private val server: IngesterServer): CliktCommand(name = "quit") {
    override fun help(context: Context) = "Quits the KIAR ingester server."
    override fun run() {
        this.server.stop()
        exitProcess(0)
    }
}