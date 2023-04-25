package ch.pontius.kiar.uploader.model.database.institution

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * The [DbRole] a [DbUser] can have.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbRole(entity: Entity) : XdEnumEntity(entity)  {
    companion object : XdEnumEntityType<DbRole>() {
        val ADMINISTRATOR by enumField { description = "ADMINISTRATOR" }
        val MANAGER by enumField { description = "MANAGER" }
        val VIEWER by enumField { description = "VIEWER" }
    }

    var description by xdRequiredStringProp(unique = true)
}