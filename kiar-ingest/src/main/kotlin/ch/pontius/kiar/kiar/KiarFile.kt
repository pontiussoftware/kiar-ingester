package ch.pontius.kiar.kiar

import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path
import java.util.LinkedList
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * A class to read KIAR files as handled by th
 *
 * @author Ralph Gasser
 * @version 1.1.0
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

    /** A [List] of metadata [ZipEntry] found in this [KiarFile]. */
    private val metadata = LinkedList<KiarEntry>()

    /** A [List] of resource [ZipEntry] found in this [KiarFile]. */
    private val resources = LinkedList<ZipEntry>()

    /** The path separator used by this [KiarFile]. */
    var separator: String = "/"
        private set

    init {
        var separatorChange = 0
        for (item in this.zip.stream()) {
            if (!item.isDirectory) {
                /* Update directory separator. */
                if (item.name.contains("\\") && this.separator != "\\") {
                    this.separator = "\\"
                    separatorChange += 1
                    require(separatorChange <= 1) { "File '${this.path}' is not a valid KIAR file: inconsistent directory separators." }
                } else if (item.name.contains("/") && this.separator != "/") {
                    this.separator = "/"
                    separatorChange += 1
                    require(separatorChange <= 1) { "File '${this.path}' is not a valid KIAR file: inconsistent directory separators." }
                }

                /* Process actual item. */
                when {
                    item.name.startsWith("${METADATA_FOLDER_NAME}${this.separator}", true) -> this.metadata.add(KiarEntry(item))
                    item.name.startsWith("${RESOURCES_FOLDER_NAME}${this.separator}", true) -> this.resources.add(item)
                }
            }
        }
        require(this.metadata.size > 0) { "File '${this.path}' is not a valid KIAR file or is empty." }
    }

    /**
     * Returns the number of entries contained in this [KiarFile].
     *
     * @return Size of this [KiarFile]
     */
    fun size(): Int = this.metadata.size

    /**
     * Returns an [Iterator] over the [KiarFile.KiarEntry] contained in this [KiarFile].
     */
    override fun iterator(): Iterator<KiarFile.KiarEntry> = this.metadata.iterator()

    /**
     * Closes the [ZipFile] backing this [KiarFile]
     */
    override fun close() = this.zip.close()

    /**
     * An entry in a [KiarFile].
     */
    inner class KiarEntry(private val entry: ZipEntry) {

        /** The [KiarEntryType] of this [KiarEntry]. */
        val type: KiarEntryType = when {
            this.entry.name.endsWith(KiarEntryType.JSON.suffix, true) -> KiarEntryType.JSON
            this.entry.name.endsWith(KiarEntryType.XML.suffix, true) -> KiarEntryType.XML
            else -> throw InvalidKiarEntryException(this.entry.name)
        }

        /** The UUID that identifies this [KiarEntry]. */
        val uuid: UUID = UUID.fromString(this.entry.name.replace(this.type.suffix, "").replace("${METADATA_FOLDER_NAME}${this@KiarFile.separator}", "").lowercase())

        /** A [List] of [ZipEntry] that represent resource that belong to this [KiarEntry]. */
        private val resources: List<ZipEntry> by lazy {
            this@KiarFile.resources.filter { e ->
                e.name.startsWith("${RESOURCES_FOLDER_NAME}${this@KiarFile.separator}${this.uuid}", true) &&
                (e.name.endsWith("jpg", ignoreCase = true)  || e.name.endsWith("jfif", ignoreCase = true) || e.name.endsWith("jpeg", ignoreCase = true)
                || e.name.endsWith("png", ignoreCase = true) || e.name.endsWith("tif", ignoreCase = true) || e.name.endsWith("tiff", ignoreCase = true))
            }
        }

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
    }
}