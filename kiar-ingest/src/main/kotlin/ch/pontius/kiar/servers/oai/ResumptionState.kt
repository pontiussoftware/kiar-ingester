package ch.pontius.kiar.servers.oai

import ch.pontius.kiar.servers.mapper.Mapper

/**
 * The state a resumption token stands for: the offset to continue at plus the complete query context, so that a
 * token cannot be replayed against another collection or with a different date window.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ResumptionState(val start: Int, val set: String?, val mapper: Mapper, val collection: String, val from: String?, val until: String?)