package ch.pontius.kiar.utilities.extensions

import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.api.model.user.Role
import ch.pontius.kiar.api.model.user.User

/**
 * Ensures that this [User] may act on resources of the given participant.
 *
 * Administrators may act on everything. Every other user must belong to an institution whose participant matches.
 * A missing participant (on either side) is denied.
 *
 * @param participantName The name of the participant that owns the resource.
 * @param message The message of the [ErrorStatusException] thrown on denial.
 * @throws ErrorStatusException (403) if access is denied.
 */
fun User.requireParticipant(participantName: String?, message: String) {
    if (this.role == Role.ADMINISTRATOR) return
    val own = this.institution?.participantName
    if (participantName == null || own == null || own != participantName) {
        throw ErrorStatusException(403, message)
    }
}

/**
 * Ensures that this [User] may act on resources of the given institution.
 *
 * Administrators may act on everything. Every other user must belong to exactly that institution.
 * A missing institution (on either side) is denied.
 *
 * @param institutionId The ID of the institution that owns the resource.
 * @param message The message of the [ErrorStatusException] thrown on denial.
 * @throws ErrorStatusException (403) if access is denied.
 */
fun User.requireInstitution(institutionId: Int?, message: String) {
    if (this.role == Role.ADMINISTRATOR) return
    val own = this.institution?.id
    if (institutionId == null || own == null || own != institutionId) {
        throw ErrorStatusException(403, message)
    }
}
