package ch.pontius.kiar.database.config.transformers

import ch.pontius.kiar.api.model.config.transformers.TransformerConfig
import ch.pontius.kiar.database.config.jobs.DbJobTemplate
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.processors.transformers.*
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence
import org.apache.solr.common.SolrInputDocument

/**
 * A configuration for an input data transformer.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbTransformer(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbTransformer>()

    /** The [DbTransformerType] of this [DbTransformer]. */
    var type by xdLink1(DbTransformerType)

    /** The value of this [DbTransformerParameter]. */
    val parameters by xdChildren0_N(DbTransformerParameter::transformer)

    /** The [DbJobTemplate] this [DbTransformer] belongs to. */
    val template: DbJobTemplate by xdParent(DbJobTemplate::transformers)

    /**
     * Convenience method to convert this [DbTransformer] to a [TransformerConfig].
     *
     * Requires an ongoing transaction.
     *
     * @return [TransformerConfig]
     */
    fun toApi() = TransformerConfig(this.type.toApi(), this.parameters.asSequence().associate { it.key to it.value })

    /**
     * Generates and returns a new [Transformer] instance from this [DbTransformer] entry.
     *
     * Requires an ongoing transactional context!
     *
     * @param input The input [Source] to hook the [Transformer] to.
     * @return [Transformer]
     */
    fun newInstance(input: Source<SolrInputDocument>): Transformer<SolrInputDocument,SolrInputDocument> {
        val parameters = this.parameters.asSequence().associate { it.key to it.value }
        return when (this.type.description) {
            "DISPLAY" -> DisplayTransformer(input)
            "SYSTEM" -> InstitutionTransformer(input)
            "RIGHTS" -> RightsTransformer(input)
            "UUID" -> HashUuidGenerator(input, parameters)
            else -> throw IllegalStateException("Unsupported transformer type '${this.type.description}'. This is a programmer's error!")
        }
    }
}