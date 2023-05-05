package ch.pontius.kiar.cli

import ch.pontius.kiar.config.Config
import ch.pontius.kiar.ingester.IngesterServer
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import jetbrains.exodus.database.TransientEntityStore
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import java.io.IOException
import java.util.regex.Pattern

/**
 * The Cli main-class.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class Cli(config: Config, server: IngesterServer, store: TransientEntityStore) {

    companion object {
        /** The default prompt -- just fancification */
        private const val PROMPT = "\uD83E\uDD55"

        /** RegEx for splitting input lines. */
        private val LINE_SPLIT_REGEX: Pattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")
    }

    /** Basic [NoOpCliktCommand] that contains all commands supported by this [Cli]. */
    private val clikt = object : NoOpCliktCommand(name = "ingester", help = "The base command for all CLI commands.") {
        init {
            subcommands(
                UserCommand(store),
                ScheduleCommand(server),
                ExecuteCommand(server),
                QuitCommand(server)
            )
        }
    }

    /** Flag indicating whether [Cli] has been stopped. */
    @Volatile
    private var stopped: Boolean = false

    /**
     * Tries to execute the given CLI command.
     */
    fun execute(command: String) = try {
        this.clikt.parse(splitLine(command))
        println()
    } catch (e: Exception) {
        when (e) {
            is com.github.ajalt.clikt.core.NoSuchSubcommand,
            is com.github.ajalt.clikt.core.MissingArgument,
            is com.github.ajalt.clikt.core.MissingOption,
            is com.github.ajalt.clikt.core.BadParameterValue,
            is com.github.ajalt.clikt.core.NoSuchOption,
            is com.github.ajalt.clikt.core.UsageError -> println(e.localizedMessage)
            else -> println(e.printStackTrace())
        }
    }

    /**
     * Blocking REPL of the CLI
     */
    fun loop() {
        val terminal = try {
            TerminalBuilder.builder().jna(true).build()
        } catch (e: IOException) {
            System.err.println("Could not initialize terminal: ${e.message}. Ending C(arrot)LI...")
            return
        }

        /* Start CLI loop. */
        val lineReader = LineReaderBuilder.builder().terminal(terminal).appName("Ingester").build()
        while (!this.stopped) {
            /* Catch ^D end of file as exit method */
            val line = try {
                lineReader.readLine(PROMPT).trim()
            } catch (e: EndOfFileException) {
                System.err.println("Could not read from terminal.")
                break
            } catch (e: UserInterruptException) {
                System.err.println("Ingester was interrupted by the user (Ctrl-C).")
                break
            }

            if (line.lowercase() == "help") {
                println(clikt.getFormattedHelp())
                continue
            }
            if (line.isBlank()) {
                continue
            }

            /* Execute command. */
            this.execute(line)

            /* Sleep for a few milliseconds. */
            Thread.sleep(100)
        }
    }

    //based on https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double/366532
    private fun splitLine(line: String?): List<String> {
        if (line == null || line.isEmpty()) {
            return emptyList()
        }
        val matchList: MutableList<String> = ArrayList()
        val regexMatcher = LINE_SPLIT_REGEX.matcher(line)
        while (regexMatcher.find()) {
            when {
                regexMatcher.group(1) != null -> matchList.add(regexMatcher.group(1))
                regexMatcher.group(2) != null -> matchList.add(regexMatcher.group(2))
                else -> matchList.add(regexMatcher.group())
            }
        }
        return matchList
    }
}