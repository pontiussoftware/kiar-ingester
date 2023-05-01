package ch.pontius.kiar.imdas.museum

import ch.pontius.kiar.config.ImdasConfig
import org.apache.logging.log4j.LogManager
import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager

/**
 * An accessor class for museum information from imdas pro.
 *
 * Compatible with version 6 and 7 (database version).
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class MuseumAccessor(config: ImdasConfig) : Closeable {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }

    /** The [Connection] used to access [Museum]s. */
    private val connection: Connection = DriverManager.getConnection(config.toURL())

    /**
     * Fetches a [List] of all [Museum]s.
     *
     * @return [List] of [Museum]
     */
    fun fetch(): List<Museum> {
        val institution = mutableListOf<Museum>()
        try {
            this.connection.prepareStatement("select  museum.name as name,museum.isil as isil,address.street as street, address.postcode as postcode, term.name as city, address.destrict as canton,address.email as email,address.http as website from museum inner join address on (museum.address_id = address.address_id) inner join thes_rel on (address.city_id = thes_rel.thes_rel_id) inner join term on (thes_rel.term_id = term.term_id)").use { statement ->
                statement.executeQuery().use { result ->
                    while (result.next()) {
                        institution.add(Museum(result))
                    }
                }
            }

        } catch (e: Throwable) {
            LOGGER.error("An error occurred while loading museum information: ${e.message}")
        } finally {
            return institution
        }
    }


    /**
     * Close [MuseumAccessor] and associated [Connection].
     */
    override fun close() = this.connection.close()
}