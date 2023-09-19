package ch.pontius.kiar.api.model.config.transformers

import ch.pontius.kiar.database.config.transformers.DbTransformerType

/**
 * Enumeration of [TransformerConfig]s supported by the KIAR tools
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class TransformerType {
    DISPLAY,
    SYSTEM,
    RIGHTS;

    /**
     * Converts this [TransformerType] into a [DbTransformerType]. Requires an ongoing transaction.
     *
     * @return [DbTransformerType].
     */
    fun toDb(): DbTransformerType = when(this) {
        DISPLAY -> DbTransformerType.DISPLAY
        SYSTEM -> DbTransformerType.SYSTEM
        RIGHTS -> DbTransformerType.RIGHTS
    }
}