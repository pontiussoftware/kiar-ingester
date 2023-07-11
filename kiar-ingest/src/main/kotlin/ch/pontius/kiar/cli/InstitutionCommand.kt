package ch.pontius.kiar.cli

import ch.pontius.kiar.cli.models.Institution
import ch.pontius.kiar.database.institution.DbInstitution
import ch.pontius.kiar.database.institution.DbParticipant
import ch.pontius.kiar.database.institution.DbUser
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.creator.findOrNew
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.query
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * A collection of [CliktCommand]s for [DbUser] management
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class InstitutionCommand(store: TransientEntityStore) : NoOpCliktCommand(name = "institution", help = "Commands surrounding the management of museum institutions.", printHelpOnEmptyArgs = true) {
    init {
        this.subcommands(Import(store))
    }

    inner class Import(private val store: TransientEntityStore) : CliktCommand(name = "import", help = "Import an imdas pro JSON export to create new institutions.", printHelpOnEmptyArgs = true) {

        /** The name of the newly created user. */
        private val path: Path by option("-i", "--input", help = "Path to the file that contains the institution metadata.").convert { Paths.get(it) }.required()

        /** The name of the newly created user. */
        private val participant: String by option("-p", "--participant", help = "Path to the file that contains the institution metadata.").required()

        /**
         * Performs the import of the institution masterdata.
         */
        override fun run() {
           val institutions = Files.newInputStream(this.path, StandardOpenOption.READ).use {
               Json { coerceInputValues = true }.decodeFromStream<List<Institution>>(it)
           }

            this.store.transactional {
                for (i in institutions) {
                    DbInstitution.new {
                        this.name = i.name
                        this.displayName = i.name
                        this.participant = DbParticipant.findOrNew(DbParticipant.filter { it.name eq this@Import.participant }) { this.name = this@Import.participant }
                        this.isil = i.isil
                        this.street = i.street
                        this.city = i.city
                        this.zip = i.postcode
                        this.canton = i.canton
                        this.email = i.email
                        this.homepage = i.website
                        this.publish = true
                    }
                }
            }
            println("Successfully imported ${institutions.size} institutions!")
        }
    }
}