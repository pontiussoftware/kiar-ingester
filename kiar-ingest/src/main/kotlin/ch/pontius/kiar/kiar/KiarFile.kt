package ch.pontius.kiar.kiar

import java.io.Closeable
import java.nio.file.Path
import java.util.LinkedList
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * A class to read KIAR files as handled by th
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class KiarFile(private val path: Path): Closeable, Iterable<KiarEntry> {

    companion object {
        /** Name of the metadata folder. This is a KIAR default! */
        const val METADATA_FOLDER_NAME = "metadata"

        /** Name of the resources folder. This is a KIAR default! */
        const val RESOURCES_FOLDER_NAME = "resources"
    }

    /** The [ZipFile] wrapped by this [KiarFile]. */
    private val zip = ZipFile(this.path.toFile())

    /** A [List] of [KiarEntry] found in this [KiarFile]. */
    private val entries: List<KiarEntry> by lazy {
        val list = LinkedList<KiarEntry>()
        for (e in this.zip.entries()) {
            if (e.name != "${METADATA_FOLDER_NAME}/" && e.name.startsWith("${METADATA_FOLDER_NAME}/")) {
                try {
                    list.add(KiarEntry(e, this.zip))
                } catch (e: InvalidKiarEntryException) {
                    throw e
                }
            }
        }
        list
    }

    init {
        require(this.zip.getEntry(METADATA_FOLDER_NAME)?.isDirectory == true) { "File '${this.path}' is not a valid KIAR file." }
        require(this.zip.getEntry(RESOURCES_FOLDER_NAME)?.isDirectory == true) { "File '${this.path}' is not a valid KIAR file." }
    }

    /**
     * Returns the number of entries contained in this [KiarFile].
     *
     * @return Size of this [KiarFile]
     */
    fun size(): Int = this.entries.size

    /**
     * Returns an [Iterator] over the [KiarEntry] contained in this [KiarFile].
     */
    override fun iterator(): Iterator<KiarEntry> = this.entries.iterator()

    /**
     * Closes the [ZipFile] backing this [KiarFile]
     */
    override fun close() = this.zip.close()
}