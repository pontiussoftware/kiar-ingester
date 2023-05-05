package ch.pontius.kiar.database.institution

import ch.pontius.kiar.api.routes.session.Role
import ch.pontius.kiar.database.config.mapping.DbEntityMapping
import ch.pontius.kiar.ingester.parsing.values.ValueParsers
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

    /**
     * A convenience method used to convert this [DbRole] to a [Role] instance.
     *
     * @return This [Role].
     */
    fun toApi(): Role = Role.valueOf(this.description)
}