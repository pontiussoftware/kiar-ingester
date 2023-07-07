package ch.pontius.kiar.api.model.config.solr

import ch.pontius.kiar.api.model.config.templates.JobType
import ch.pontius.kiar.database.config.jobs.DbJobType
import ch.pontius.kiar.database.config.solr.DbCollectionType

/**
 * The type of [ApacheSolrCollection].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class CollectionType {
    OBJECT,
    PERSON,
    MUSEUM;

    /**
     * Convenience method to convert this [CollectionType] to a [DbCollectionType]. Requires an ongoing transaction!
     *
     * @return [DbCollectionType]
     */
    fun toDb(): DbCollectionType = when(this) {
        OBJECT -> DbCollectionType.OBJECT
        PERSON-> DbCollectionType.PERSON
        MUSEUM -> DbCollectionType.MUSEUM
    }
}