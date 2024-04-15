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
 * @version 1.2.0
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

    /**
     * Returns an [Iterator] over the [KiarFile.KiarEntry] contained in this [KiarFile].
     */
    override fun iterator(): Iterator<KiarFile.KiarEntry> = object: Iterator<KiarFile.KiarEntry> {

        /** The path separator used by this [KiarFile]. */
        private var separator: String = "/"

        /** A [List] of metadata [ZipEntry] found in this [KiarFile]. */
        private val metadata = LinkedList<ZipEntry>()

        /** A [List] of resource [ZipEntry] found in this [KiarFile]. */
        private val resources = LinkedList<ZipEntry>()

        init {
            var separatorChange = 0
            this@KiarFile.zip.stream().use { stream ->
                for (item in stream) {
                    if (!item.isDirectory) {
                        /* Update directory separator. */
                        if (item.name.contains("\\") && this.separator != "\\") {
                            this.separator = "\\"
                            separatorChange += 1
                            require(separatorChange <= 1) { "File '${this@KiarFile.path}' is not a valid KIAR file: inconsistent directory separators." }
                        } else if (item.name.contains("/") && this.separator != "/") {
                            this.separator = "/"
                            separatorChange += 1
                            require(separatorChange <= 1) { "File '${this@KiarFile.path}' is not a valid KIAR file: inconsistent directory separators." }
                        }

                        /* Process actual item. */
                        when {
                            item.name.startsWith("${METADATA_FOLDER_NAME}${this.separator}", true) &&
                            (item.name.endsWith(KiarEntryType.JSON.suffix) || item.name.endsWith(KiarEntryType.XML.suffix))-> this.metadata.add(item)
                            item.name.startsWith("${RESOURCES_FOLDER_NAME}${this.separator}", true) &&
                            (item.name.endsWith("jpg", ignoreCase = true)  || item.name.endsWith("jfif", ignoreCase = true) || item.name.endsWith("jpeg", ignoreCase = true)
                            || item.name.endsWith("png", ignoreCase = true) || item.name.endsWith("tif", ignoreCase = true) || item.name.endsWith("tiff", ignoreCase = true)) -> this.resources.add(item)
                        }
                    }
                }
            }
            require(this.metadata.size > 0) { "File '${this@KiarFile.path}' is not a valid KIAR file or is empty." }
        }

        /**
         * Returns true, if metadata entries are left.
         */
        override fun hasNext(): Boolean = this.metadata.isNotEmpty()

        /**
         * Returns next [KiarFile.KiarEntry].
         */
        override fun next(): KiarEntry {
            /* Poll next entry and make sure, that. */
            val metadata = this.metadata.poll()
            check(metadata != null) { "No more entries left in KIAR file '${this@KiarFile.path}'." }

            /* Obtain type and UUID for entry. */
            val type: KiarEntryType = when {
                metadata.name.endsWith(KiarEntryType.JSON.suffix, true) -> KiarEntryType.JSON
                metadata.name.endsWith(KiarEntryType.XML.suffix, true) -> KiarEntryType.XML
                else -> throw InvalidKiarEntryException(metadata.name)
            }
            val uuid: UUID = UUID.fromString(metadata.name.replace(type.suffix, "").replace("${METADATA_FOLDER_NAME}${this.separator}", "").lowercase())

            /* Prepare resources. */
            val resources = LinkedList<ZipEntry>()
            this.resources.removeIf { e ->
                val match = e.name.startsWith("${RESOURCES_FOLDER_NAME}${this.separator}${uuid}", true) &&
                (e.name.endsWith("jpg", ignoreCase = true)  || e.name.endsWith("jfif", ignoreCase = true) || e.name.endsWith("jpeg", ignoreCase = true)
                || e.name.endsWith("png", ignoreCase = true) || e.name.endsWith("tif", ignoreCase = true) || e.name.endsWith("tiff", ignoreCase = true))
                if (match) resources.add(e)
                match
            }

            /* Return KiarEntry. */
            return KiarEntry(uuid, type, metadata, resources)
        }
    }

    /**
     * Closes the [ZipFile] backing this [KiarFile]
     */
    override fun close() = this.zip.close()

    /**
     * An entry in a [KiarFile].
     */
    inner class KiarEntry(val uuid: UUID, val type: KiarEntryType, private val metadata: ZipEntry, private val resources: List<ZipEntry>) {
        /**
         * Opens and returns a [InputStream] for this [KiarEntry].
         *
         * @return [InputStream]
         */
        fun open(): InputStream = this@KiarFile.zip.getInputStream(this.metadata)

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