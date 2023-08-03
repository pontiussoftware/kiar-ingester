package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.masterdata.DbRightStatement
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Field
import ch.pontius.kiar.ingester.solrj.asString
import ch.pontius.kiar.ingester.solrj.has
import ch.pontius.kiar.ingester.solrj.setField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.dnq.query.asSequence
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrInputDocument

/**
 * A [Transformer] that enriches incoming [SolrInputDocument]s with rights information.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class RightsTransformer(override val input: Source<SolrInputDocument>): Transformer<SolrInputDocument, SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger(InstitutionTransformer::class.java)
    }

    /** [MutableMap] of [DbRightStatement] entries. */
    private val rights = DbRightStatement.all().asSequence().associate { it.short to Triple(it.short, it.long, it.url) }.toMutableMap()

    init {
        /* Special case for KIM.bl / AMBL objects. */
        this.rights["Freier Zugriff, keine Nachnutzung"] = Triple("InC", "In Copyright - Re-use Not Permitted", "https://rightsstatements.org/vocab/InC/1.0/")
    }

    /**
     * Converts this [InstitutionTransformer] to a [Flow]
     *
     * @param context The [ProcessingContext] for the [Flow]
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = this.input.toFlow(context).filter { doc ->
        /* Fetch UUID field from document. */
        val uuid = doc.asString(Field.UUID)
        if (uuid == null) {
            LOGGER.error("Failed to verify document: Field 'uuid' is missing (jobId = {}, participantId = {}, docId = {}).", context.jobId, context.participant, uuid)
            context.log.add(JobLog(null, "<undefined>", null, JobLogContext.METADATA, JobLogLevel.SEVERE, "Document skipped: Field 'uuid' is missing."))
            return@filter false
        }

        /* Enrich rights statement entry. */
        if (!doc.has(Field.RIGHTS_STATEMENT)) { /* Default value. */
            doc.setField(Field.RIGHTS_STATEMENT, "CNE")
            doc.setField(Field.RIGHTS_STATEMENT_LONG, "Copyright Not Evaluated")
            doc.setField(Field.RIGHTS_STATEMENT_URL, "https://rightsstatements.org/vocab/CNE/1.0/")
        } else {
            val value = doc.asString(Field.RIGHTS_STATEMENT)
            val entry = this@RightsTransformer.rights[value]
            if (entry == null) {
                LOGGER.warn("Failed to verify document: Rights statement '$value' is unknown (jobId = {}, participantId = {}, docId = {}).", context.jobId, context.participant, uuid)
                context.log.add(JobLog(null, uuid, null, JobLogContext.METADATA, JobLogLevel.ERROR, "Document skipped: Rights statement '$value' is unknown."))
                context.skipped += 1
                return@filter false
            }
            doc.setField(Field.RIGHTS_STATEMENT_LONG, entry.first)
            doc.setField(Field.RIGHTS_STATEMENT_LONG, entry.second)
            doc.setField(Field.RIGHTS_STATEMENT_URL,  entry.third)
        }
        true
    }
}