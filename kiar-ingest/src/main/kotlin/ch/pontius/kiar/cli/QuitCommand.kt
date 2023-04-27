package ch.pontius.kiar.cli

import ch.pontius.kiar.ingester.IngesterServer
import com.github.ajalt.clikt.core.CliktCommand
import kotlin.system.exitProcess

/**
 * A CLI command that can be used to end the server.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class QuitCommand(private val server: IngesterServer): CliktCommand(name = "quit", help = "Quits the KIAR ingester server.") {
    override fun run() {
        this.server.stop()
        exitProcess(0)
    }
}