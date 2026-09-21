package ch.pontius.kiar.tasks

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/** The [KLogger] instance for [SafeTimerTask]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A [TimerTask] that never lets an exception escape [run].
 *
 * A [java.util.Timer] cancels itself permanently if any of its tasks throws, which would silently stop every periodic
 * task of the server. Subclasses implement [execute] instead; failures are logged and the next run happens as scheduled.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class SafeTimerTask : TimerTask() {

    /**
     * Performs the actual work of this task. Exceptions are logged by [run].
     */
    protected abstract fun execute()

    final override fun run() {
        try {
            this.execute()
        } catch (e: Throwable) {
            logger.error(e) { "Scheduled task ${this::class.simpleName} failed; it will run again at the next interval." }
        }
    }
}
