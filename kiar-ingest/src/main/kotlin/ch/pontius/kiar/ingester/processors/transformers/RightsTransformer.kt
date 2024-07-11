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
    private val rights = DbRightStatement.all().asSequence().associate { it.short to it.toApi() }.toMutableMap()

    init {
        /* Special case for KIM.bl and AMBL objects. */
        this.rights["Public Domain Mark"] = DbRightStatement.PDM.toApi()
        this.rights["Urheberrechtsschutz nicht bewertet"] = DbRightStatement.CNE.toApi()
        this.rights["Urheberrechtsschutz"] = DbRightStatement.InC.toApi()
        this.rights["Urheberrechtsschutz - Nutzung zu Bildungszwecken erlaubt"] = DbRightStatement.InC_EDU.toApi()
        this.rights["Freier Zugriff, keine Nachnutzung"] = DbRightStatement.InC.toApi()
        this.rights["Alle Rechte vorbehalten"] = DbRightStatement.InC.toApi()

        /* Special case: Typos */
        this.rights["CC BY-SA-NC 4.0"] = DbRightStatement.CC_BY_NC_SA.toApi()
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
            context.log(JobLog(null, "<undefined>", null, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Document skipped: Field 'uuid' is missing."))
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
                context.log(JobLog(null, uuid, null, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Document skipped: Rights statement '$value' is unknown."))
                return@filter false
            }
            doc.setField(Field.RIGHTS_STATEMENT_LONG, entry.longName)
            doc.setField(Field.RIGHTS_STATEMENT_URL,  entry.url)
        }
        true
    }
}