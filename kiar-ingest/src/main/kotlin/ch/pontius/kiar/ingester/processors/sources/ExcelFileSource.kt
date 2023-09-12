package ch.pontius.kiar.ingester.processors.sources

import ch.pontius.kiar.api.model.config.mappings.AttributeMapping
import ch.pontius.kiar.api.model.config.mappings.EntityMapping
import ch.pontius.kiar.ingester.processors.ProcessingContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.logging.log4j.LogManager
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrInputDocument
import java.nio.file.Path

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ExcelFileSource(private val file: Path, private val config: EntityMapping, private val skipResources: Boolean = false, private val deleteFileWhenDone: Boolean = true): Source<SolrInputDocument> {

    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** List of required [AttributeMapping]. */
    private val required: List<AttributeMapping> = this.config.attributes.filter { it.required }


    override fun toFlow(context: ProcessingContext): Flow<SolrInputDocument> = flow {
        /* for (entry in excel) */
        val doc = SolrInputDocument();

        emit(doc)
        /* End for. */
    }
}