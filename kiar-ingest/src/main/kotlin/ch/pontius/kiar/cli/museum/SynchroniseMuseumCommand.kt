package ch.pontius.kiar.cli.museum

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.imdas.museum.MuseumAccessor
import com.github.ajalt.clikt.core.CliktCommand

/**
 * A CLI command that can be used to synchronise museum information with imdas pro.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class SynchroniseMuseumCommand(private val config: Config): CliktCommand(name = "sync-museum", help = "Synchronises museums with imdas pro.") {
    override fun run() {
        val imdas = this.config.imdas
        if (imdas == null) {
            println("No imdas pro configuration provided. Synchronisation must be aborted!")
            return
        }
        val accessor = MuseumAccessor(imdas)
        for (m in accessor.fetch()) {
            println(m.toString())
        }
    }
}