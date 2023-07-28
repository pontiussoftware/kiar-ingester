package ch.pontius.kiar.kiar

import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.LinkedList
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.streams.toList

/**
 * A class to read KIAR files as handled by th
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class KiarFile(private val path: Path): Closeable, Iterable<KiarFile.KiarEntry> {

    companion object {
        /** Name of the metadata folder. This is a KIAR default! */
        const val METADATA_FOLDER_NAME = "metadata"

        /** Name of the resources folder. This is a KIAR default! */
        const val RESOURCES_FOLDER_NAME = "resources"
    }

    /** The [ZipFile] wrapped by this [KiarFile]. */
    val zip = ZipFile(this.path.toFile())

    /** A [List] of [KiarFile.KiarEntry] found in this [KiarFile]. */
    private val entries = LinkedList<KiarFile.KiarEntry>()

    /** The path separator used by this [KiarFile]. */
    var separator: String = "/"
        private set

    init {
        var separatorChange = 0
        for (item in this.zip.stream()) {
            if (!item.isDirectory) {
                if (item.name.startsWith("${METADATA_FOLDER_NAME}/")) {
                    if (this.separator != "/") {
                        this.separator = "/"
                        separatorChange += 1
                    }
                    this.entries.add(KiarEntry(item))
                } else if (item.name.startsWith("${METADATA_FOLDER_NAME}\\")) {
                    if (this.separator != "\\") {
                        this.separator = "\\"
                        separatorChange += 1
                    }
                    this.entries.add(KiarEntry(item))
                }
                if (separatorChange > 1) {
                    throw IllegalArgumentException("File '${this.path}' is not a valid KIAR file: inconsistent directory separators.")
                }
            }
        }
        require(this.entries.size > 0) { "File '${this.path}' is not a valid KIAR file or is empty." }
    }

    /**
     * Returns the number of entries contained in this [KiarFile].
     *
     * @return Size of this [KiarFile]
     */
    fun size(): Int = this.entries.size

    /**
     * Returns an [Iterator] over the [KiarFile.KiarEntry] contained in this [KiarFile].
     */
    override fun iterator(): Iterator<KiarFile.KiarEntry> = this.entries.iterator()

    /**
     * Closes the [ZipFile] backing this [KiarFile]
     */
    override fun close() = this.zip.close()

    /**
     * An entry in a [KiarFile].
     *
     * @author Ralph Gasser
     * @version 1.0.0
     */
    inner class KiarEntry(private val entry: ZipEntry) {

        /** The [KiarEntryType] of this [KiarEntry]. */
        val type: KiarEntryType = when {
            this.entry.name.endsWith(KiarEntryType.JSON.suffix) -> KiarEntryType.JSON
            this.entry.name.endsWith(KiarEntryType.XML.suffix) -> KiarEntryType.XML
            else -> throw InvalidKiarEntryException(this.entry.name)
        }

        /** The UUID that identifies this [KiarEntry]. */
        val uuid: String = this.entry.name.replace(this.type.suffix, "").replace("${METADATA_FOLDER_NAME}${this@KiarFile.separator}", "")

        /** A [List] of [ZipEntry] that represent resource that belong to this [KiarEntry]. */
        private val resources: List<ZipEntry> = this@KiarFile.zip.stream().filter { e ->
           e.name.startsWith("${RESOURCES_FOLDER_NAME}${this@KiarFile.separator}${this.uuid}") &&
           (e.name.startsWith("jpg", ignoreCase = true)  || e.name.startsWith("jfif", ignoreCase = true) || e.name.startsWith("jpeg", ignoreCase = true) || e.name.startsWith("png", ignoreCase = true)
                   || e.name.startsWith("tif", ignoreCase = true) || e.name.startsWith("tiff", ignoreCase = true))
        }.toList()

        /**
         * Opens and returns a [InputStream] for this [KiarEntry].
         *
         * @return [InputStream]
         */
        fun open(): InputStream = this@KiarFile.zip.getInputStream(this.entry)

        /**
         * Returns the number of resources for this [KiarEntry].
         *
         * @return The number of resources.
         */
        fun resources(): Int = this.resources.size

        /**
         * Opens the resource at the given position as [InputStream]
         *
         * @param index The position of the resource to open.
         * @return [InputStream] associated with the resource.
         */
        fun openResource(index: Int): InputStream = this@KiarFile.zip.getInputStream(this.resources[index])

        /**
         * Tries to open the resource at the given [index] as [BufferedImage]
         *
         * @param index The position of the resource to open.
         * @return [InputStream] associated with the resource.
         */
        fun tryOpenImage(index: Int): InputStream = this@KiarFile.zip.getInputStream(this.resources[index])

    }
}