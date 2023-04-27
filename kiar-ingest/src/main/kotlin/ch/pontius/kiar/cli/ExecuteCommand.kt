package ch.pontius.kiar.cli

import ch.pontius.kiar.ingester.IngesterServer
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument

/**
 * A CLI command that can be used to execute a job directly.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ExecuteCommand(private val server: IngesterServer): CliktCommand(name = "execute", help = "Executes a job with the given name.") {
    /** Name of the index to drop. */
    private val jobName: String by argument(
        name = "job",
        help = "Name of the job that should be executed."
    )

    override fun run() = try {
        this.server.execute(this.jobName)
    } catch (e: IllegalArgumentException) {
        println("Failed to execute job: ${e.message}")
    }
}