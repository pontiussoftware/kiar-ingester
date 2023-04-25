package ch.pontius.kiar.ingester.cli

import ch.pontius.kiar.ingester.IngesterServer
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument

/**
 * A CLI command that can be used to schedule a named job.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ScheduleCommand(private val server: IngesterServer): CliktCommand(name = "schedule", help = "Schedules a job with the given name for execution.") {
    /** Name of the index to drop. */
    private val jobName: String by argument(
        name = "job",
        help = "Name of the job that should be scheduled."
    )

    override fun run() = try {
        server.schedule(this.jobName)
    } catch (e: IllegalArgumentException) {
        println("Failed to schedule job: ${e.message}")
    }
}