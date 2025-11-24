package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.api.model.masterdata.RightStatement
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.*
import ch.pontius.kiar.servers.sru.SruServer
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.apache.solr.common.SolrInputDocument

/** The [KLogger] instance for [SruServer]. */
private val logger: KLogger = KotlinLogging.logger {}

/**
 * A [Transformer] that enriches incoming [SolrInputDocument]s with rights information.
 *
 * @author Ralph Gasser
 * @version 1.1.1
 */
class RightsTransformer(override val input: Source<SolrInputDocument>): Transformer<SolrInputDocument, SolrInputDocument> {

    /** [MutableMap] of [RightStatement] entries. */
    private val rights = RightStatement.DEFAULT.associateBy { it.shortName }.toMutableMap()

    init {
        /* Special case for KIM.bl and AMBL objects. */
        this.rights["Urheberrechtsschutz"] = RightStatement.DEFAULT[0]
        this.rights["Freier Zugriff, keine Nachnutzung"] = RightStatement.DEFAULT[0]
        this.rights["Alle Rechte vorbehalten"] = RightStatement.DEFAULT[0]
        this.rights["Urheberrechtsschutz - Nutzung zu Bildungszwecken erlaubt"] =  RightStatement.DEFAULT[1]
        this.rights["Urheberrechtsschutz nicht bewertet"] = RightStatement.DEFAULT[2]
        this.rights["Public Domain Mark"] = RightStatement.DEFAULT[4]
        this.rights["CC BY-SA-NC 4.0"] = RightStatement.DEFAULT[10]
    }

    /**
     * Converts this [InstitutionTransformer] to a [Flow]
     *
     * @param context The [ProcessingContext] for the [Flow]
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = this.input.toFlow(context).filter { doc ->
        /* Fetch UUID field from document. */
        val uuid = doc.get<String>(Field.UUID)
        if (uuid == null) {
            logger.error { "Failed to verify document: Field 'uuid' is missing (jobId = ${context.jobId}, participantId = ${context.jobTemplate.participantName}, docId = $uuid)." }
            context.log(JobLog(null, null, null, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Document skipped: Field 'uuid' is missing."))
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
                logger.warn { "Failed to verify document: Rights statement '$value' is unknown (jobId = ${context.jobId}, participantId = ${context.jobTemplate.participantName}, docId = $uuid)." }
                context.log(JobLog(null, uuid, null, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Document skipped: Rights statement '$value' is unknown."))
                return@filter false
            }
            doc.setField(Field.RIGHTS_STATEMENT_LONG, entry.longName)
            doc.setField(Field.RIGHTS_STATEMENT_URL,  entry.url)
        }
        true
    }
}