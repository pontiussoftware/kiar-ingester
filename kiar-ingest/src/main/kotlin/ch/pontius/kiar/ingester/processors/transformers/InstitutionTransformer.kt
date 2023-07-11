package ch.pontius.kiar.ingester.processors.transformers

import ch.pontius.kiar.api.model.job.JobLog
import ch.pontius.kiar.api.model.job.JobLogContext
import ch.pontius.kiar.api.model.job.JobLogLevel
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.ingester.processors.ProcessingContext
import ch.pontius.kiar.ingester.processors.sources.Source
import ch.pontius.kiar.ingester.solrj.Constants
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_INSTITUTION
import ch.pontius.kiar.ingester.solrj.Constants.FIELD_NAME_UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import org.apache.solr.common.SolrInputDocument

/**
 * A [Transformer] that derives the fields '_canton_' and '_participant_' from the field 'institution'.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
class InstitutionTransformer(override val input: Source<SolrInputDocument>, parameters: Map<String,String>): Transformer<SolrInputDocument, SolrInputDocument> {

    /** Fetch all institution information */
    private val institutions = DbInstitution.filter { it.publish eq true }.asSequence().map { it.name to Pair(it.participant.name, it.canton) }.toMap()

    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = this.input.toFlow(context).filter {
        /* Fetch institution field from document. */
        val institution = it[FIELD_NAME_INSTITUTION]?.value
        val uuid = it[FIELD_NAME_UUID]?.value as String
        if (institution == null) {
            context.log.add(JobLog(null, uuid, JobLogContext.METADATA, JobLogLevel.WARNING, "Document skipped: Field 'institution' is missing."))
            return@filter false
        }

        /* Fetch corresponding database entry. */

        val entry = this@InstitutionTransformer.institutions[institution]
        if (entry == null) {
            context.log.add(JobLog(null, uuid, JobLogContext.METADATA, JobLogLevel.WARNING, "Document skipped: Could not find database entry for institution '${institution}'."))
            return@filter false
        }

        /* Update Solr document. */
        it.addField(Constants.FIELD_NAME_PARTICIPANT, entry.first)
        it.addField(Constants.FIELD_NAME_CANTON, entry.second)

        /* Return true. */
        return@filter true
    }
}