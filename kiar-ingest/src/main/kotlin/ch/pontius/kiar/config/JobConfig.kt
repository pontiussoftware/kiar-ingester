package ch.pontius.kiar.config

import ch.pontius.kiar.ingester.processors.sinks.Sinks
import ch.pontius.kiar.ingester.processors.sources.Sources
import ch.pontius.kiar.ingester.processors.transformers.Transformers
import ch.pontius.kiar.ingester.watcher.FileWatcher
import ch.pontius.kiar.utilities.serialization.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * A configuration item for registering a [FileWatcher]
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class JobConfig(
    /** The name of this [JobConfig]. */
    val name: String,

    /** Path to the source file. Must be compatible with selected [Sources]. */
    @Serializable(with = PathSerializer::class)
    val file: Path,

    /** The type of [Sources] that should be used for the [JobConfig]. */
    val source: Sources,

    /** The type of [Sinks] that should be used for the [JobConfig]. */
    val sink: Sinks,

    /** List of [Transformers] to apply. Can be empty. */
    val transformers: List<TransformerConfig> = emptyList(),

    /** The named mapping configuration to use. Must correspond to an existing configuration. */
    val mappingConfig: String,

    /** The name of the [SolrConfig] to use. Must correspond to an existing configuration. */
    val solrConfig: String,

    /** Set to true, if job should be started upon creation of the file. */
    val startOnCreation: Boolean = false,

    /** Set to true, if file should be deleted upon completion of the Job. */
    val deleteOnCompletion: Boolean = false
)