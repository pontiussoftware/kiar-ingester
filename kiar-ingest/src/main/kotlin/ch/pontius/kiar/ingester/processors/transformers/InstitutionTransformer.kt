package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.api.model.institution.Institution
import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.apache.solr.common.SolrInputDocument

/**
 * A [Transformer] that a) validates incoming [SolrInputDocument]s with respect to required fields, b) checks and correct the association of the document
 * with a present [Institution] and participant and c) enriches verified documents with meta information that can be derived from the [Institution].
 *
 * @author Ralph Gasser
 * @version 1.2.1
 */
class InstitutionTransformer(override val input: Source<SolrInputDocument>): Transformer<SolrInputDocument, SolrInputDocument> {
    /**
     * Converts this [InstitutionTransformer] to a [Flow]
     *
     * @param context The [ProcessingContext] for the [Flow]
     * @return [Flow]
     */
    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> {
        return this.input.toFlow(context).filter { doc ->
            /* Fetch institution field from document. */
            val uuid = doc.get<String>(Field.UUID)
            if (uuid == null) {
                context.log(JobLog(null, null, null, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Failed to verify document: Field 'uuid' is missing."))
                return@filter false
            }

            /* Validate institution entry; if it's missing, try to derive it from the participant. */
            var institutionName = doc.asString(Field.INSTITUTION)
            if (institutionName == null) {
                /* We can now try to derive the institution from the participant. */
                val institution = context.institutions.values.singleOrNull { it.participantName == context.jobTemplate.participantName }
                if (institution == null) {
                    context.log(JobLog(null, uuid, null, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Field 'institution' could not be derived from '_participant_'."))
                    return@filter false
                }
                doc.setField(Field.INSTITUTION, institution.name)
                institutionName = institution.name
            }

            /* Fetch corresponding database entry. */
            val entry = context.institutions[institutionName]
            if (entry == null) {
                context.log(JobLog(null, uuid, null, JobLogContext.METADATA, JobLogLevel.VALIDATION, "Failed to verify document: Institution '$institutionName' is unknown."))
                return@filter false
            }

            /* Check presence of collection name. */
            val collectionName = doc.asString(Field.COLLECTION)
            if (collectionName == null) {
                doc.setField(Field.COLLECTION, institutionName)
                context.log(JobLog(null, uuid, null, JobLogContext.METADATA, JobLogLevel.WARNING, "Collection not specified; using institution name instead."))
            }

            /* Enrich Apache Solr with institution-based information. */
            if (!doc.has(Field.PARTICIPANT)) {
                doc.setField(Field.PARTICIPANT, entry.participantName)
            }
            if (!doc.has(Field.CANTON)) {
                doc.setField(Field.CANTON, entry.canton.longName)
            }
            if (!doc.has(Field.INSTITUTION_EMAIL)) {
                doc.setField(Field.INSTITUTION_EMAIL, entry.email)
            }
            if (!doc.has(Field.COPYRIGHT) && !entry.defaultCopyright.isNullOrBlank()) {
                doc.setField(Field.COPYRIGHT, entry.defaultCopyright)
            }
            if (!doc.has(Field.RIGHTS_STATEMENT) && !entry.defaultRightStatement.isNullOrBlank()) {
                doc.setField(Field.RIGHTS_STATEMENT, entry.defaultRightStatement)
            }

            /* Return true. */
            return@filter true
        }
    }
}