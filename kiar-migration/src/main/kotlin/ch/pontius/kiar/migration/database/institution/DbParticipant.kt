package ch.pontius.kiar.migration.database.institution

import ch.pontius.kiar.database.institutions.Participants
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEntity
import kotlinx.dnq.XdNaturalEntityType
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.xdRequiredStringProp
import org.jetbrains.exposed.v1.jdbc.insert

/**
 * A [DbParticipant] as managed by the KIAR Tool.
 *
 * A participant in the KIAR data model is an entity that can inegst data using the tools
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbParticipant(entity: Entity) : XdEntity(entity) {
    companion object: XdNaturalEntityType<DbParticipant>() {
        fun migrate() {
            all().asSequence().forEach { dbParticipant ->
                Participants.insert {
                    it[name] = dbParticipant.name
                }
            }
        }
    }

    /** The name held by this [DbParticipant]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)
}