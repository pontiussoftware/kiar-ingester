package ch.pontius.kiar.kiar

import java.io.InputStream
import java.util.*
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * An entry in a [KiarFile].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class KiarEntry(private val entry: ZipEntry, private val zip: ZipFile) {

    /** The [KiarEntryType] of this [KiarEntry]. */
    val type: KiarEntryType = when {
        this.entry.name.endsWith(KiarEntryType.JSON.suffix) -> KiarEntryType.JSON
        this.entry.name.endsWith(KiarEntryType.XML.suffix) -> KiarEntryType.XML
        else -> throw InvalidKiarEntryException(this.entry.name)
    }

    /** The UUID that identifies this [KiarEntry]. */
    val uuid: String = this.entry.name.replace(this.type.suffix, "").replace("${KiarFile.METADATA_FOLDER_NAME}/", "")


    /** A [List] of [ZipEntry] that represent resource that belong to this [KiarEntry]. */
    private val resources: List<ZipEntry> by lazy {
        val regex =  Regex("^${KiarFile.RESOURCES_FOLDER_NAME}/${this.uuid}(_([0-9]+))*\\.(jpg|jpeg|jfif|png|tif|tiff)\$")
        val list = LinkedList<ZipEntry>()
        for (e in this.zip.entries()) {
            if (e.name.matches(regex)) {
                list.add(e)
            }
        }
        list
    }

    /**
     * Opens and returns a [InputStream] for this [KiarEntry].
     *
     * @return [InputStream]
     */
    fun open(): InputStream = this.zip.getInputStream(this.entry)

    /**
     * Returns the number of resources for this [KiarEntry].
     *
     * @return The number of resources.
     */
    fun resources(): Int = this.resources.size

    /**
     * Opens the resource at the given position.
     *
     * @param index The position of the resource to open.
     * @return [InputStream] associated with the resource.
     */
    fun openResource(index: Int): InputStream = this.zip.getInputStream(this.resources[index])
}