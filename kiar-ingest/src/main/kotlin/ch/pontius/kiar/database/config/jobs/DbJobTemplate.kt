package ch.pontius.kiar.database.config.jobs

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.database.config.mapping.DbEntityMapping
import ch.pontius.kiar.database.config.solr.DbCollection
import ch.pontius.kiar.database.config.solr.DbSolr
import ch.pontius.kiar.database.config.transformers.DbTransformer
import ch.pontius.kiar.database.institution.DbParticipant
import ch.pontius.kiar.ingester.processors.sinks.ApacheSolrSink
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.processors.sources.XmlFileSource
import ch.pontius.kiar.ingester.processors.transformers.Transformer
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Path

/**
 * The template for a job used by the KIAR tools.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbJobTemplate(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<DbJobTemplate>()

    /** The name of this [DbJobTemplate]. */
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** The [DbJobType] of this [DbJobTemplate]. */
    var type by xdLink1(DbJobType)

    /** The [DbParticipant] this [DbJobTemplate] belongs to. */
    var participant by xdLink1(DbParticipant)

    /** The [DbCollection]s this [DbJobTemplate] maps to. */
    var solr by xdLink1(DbSolr)

    /** The [DbEntityMapping] this [DbJobTemplate] employs. */
    var mapping: DbEntityMapping by xdLink1(DbEntityMapping)

    /** The [DbEntityMapping] this [DbJobTemplate] employs. */
    val transformers by xdChildren0_N(DbTransformer::template)

    /** Flag indicating, if this [DbJobTemplate] should be started automatically once the file appears. */
    var startAutomatically by xdBooleanProp()

    /** Flag indicating, if this [DbJobTemplate] should be started automatically once the file appears. */
    var deleted by xdBooleanProp()

    /**
     * Returns the [Path] to the expected ingest file for this [DbJobTemplate].
     *
     * Requires an ongoing transaction!
     */
    fun sourcePath(config: Config): Path = config.ingestPath.resolve(this.participant.name).resolve("${this.name} + ${this.type.suffix}")

    /**
     * Generates and returns a new [Transformer] instance from this [DbTransformer] entry.
     *
     * Requires an ongoing transactional context!
     *
     * @param config The KIAR tools [Config] object.
     * @return [Transformer]
     */
    fun newInstance(config: Config): ApacheSolrSink {
        /* Generate file source. */
        val source: Source<SolrInputDocument> = when (this.type.description) {
            "XML" -> XmlFileSource(this.name, this.sourcePath(config), this.mapping.toApi())
            else -> throw IllegalStateException("Unsupported transformer type '${this.type.description}'. This is a programmer's error!")
        }
        var root = source

        /* Generate all transformers source. */
        for (t in this.transformers.asSequence()) {
            root = t.newInstance(root)
        }

        /* Return ApacheSolrSink. */
        return ApacheSolrSink(root, this.solr.toApi())
    }
}