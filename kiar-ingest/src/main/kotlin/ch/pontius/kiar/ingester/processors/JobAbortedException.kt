package ch.pontius.kiar.ingester.processors

import kotlinx.coroutines.CancellationException

/**
 * Thrown by a [ch.pontius.kiar.ingester.processors.sources.Source] when the user aborts a running job.
 *
 * It is a [CancellationException], so the processing [kotlinx.coroutines.flow.Flow] terminates exceptionally (which makes
 * every downstream `onCompletion` handler take its rollback / cleanup branch) without being reported as a failure.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class JobAbortedException(jobId: Int) : CancellationException("Job $jobId was aborted by the user.")
