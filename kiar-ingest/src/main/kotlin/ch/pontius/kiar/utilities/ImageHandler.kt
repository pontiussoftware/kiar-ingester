package ch.pontius.kiar.utilities

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.ImageWriter
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.jpeg.iptc.IptcRecord
import org.apache.commons.imaging.formats.jpeg.iptc.IptcTypes
import org.apache.commons.imaging.formats.jpeg.iptc.JpegIptcRewriter
import org.apache.commons.imaging.formats.jpeg.iptc.PhotoshopApp13Data
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * A utility object that provides methods to handle images.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object ImageHandler {

    /** The [Logger] instance used by this [ImageHandler]. */
    private val LOGGER: Logger = LoggerFactory.getLogger(ImageHandler::class.java)

    /**
     * Stores the provided [ImmutableImage] under the provided [Path]. Also makes sure, that selected image metadata is transferred.
     *
     * @param image The [ImmutableImage] to store.
     * @param metadata The [ImageMetadata] to write to the image.
     * @param writer The [ImageWriter] to use.
     * @param path The [Path] to store the image under.
     * @return True if the image was stored successfully, false otherwise.
     */
    fun store(image: ImmutableImage, metadata: ImageMetadata, writer: ImageWriter, path: Path) : Boolean = try {
        /* Write image bytes. */
        var output = ByteArrayOutputStream().use { os ->
            writer.write(image, null, os)
            os.toByteArray()
        }

        /* Obtain existing EXIF metadata. */
        val existingMetadata = Imaging.getMetadata(output)
        val tiffOutputSet = (existingMetadata as? JpegImageMetadata)?.exif?.outputSet ?: TiffOutputSet()
        val exifDirectory = tiffOutputSet.getOrCreateRootDirectory()

        /* Transfer metadata. */
        for (directory in metadata.directories) {
            when (directory.name) {
                "Exif IFD0",
                "Exif IFD1"-> {
                    var edit = false
                    for (tag in directory.tags) {
                        when (tag.name) {
                            "XMP" -> {
                                exifDirectory.add(TiffTagConstants.TIFF_TAG_XMP, *tag.value.toByteArray())
                                edit = true
                            }
                            "Description" -> {
                                exifDirectory.add(TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, tag.value)
                                edit = true
                            }
                            "Artist" -> {
                                exifDirectory.add(TiffTagConstants.TIFF_TAG_ARTIST, tag.value)
                                edit = true
                            }
                            "Copyright" -> {
                                exifDirectory.add(TiffTagConstants.TIFF_TAG_COPYRIGHT, tag.value)
                                edit = true
                            }
                        }
                    }

                    /* Prepare and write transferable metadata (if available). */
                    if (edit) {
                        output = ByteArrayOutputStream().use { os ->
                            ExifRewriter().updateExifMetadataLossless(output, os, tiffOutputSet)
                            os.toByteArray()
                        }
                    }
                }
                "IPTC" -> {
                    /* Extract IPTC metadata. */
                    val newRecords: MutableList<IptcRecord> = LinkedList()
                    for (tag in directory.tags) {
                        when (tag.name) {
                            "Keywords" -> {
                                val record = IptcRecord(IptcTypes.KEYWORDS, tag.value)
                                newRecords.add(record)
                            }
                            "Headline" -> {
                                val record = IptcRecord(IptcTypes.HEADLINE, tag.value)
                                newRecords.add(record)
                            }
                            "Byline",
                            "By-line" -> {
                                val record = IptcRecord(IptcTypes.BYLINE, tag.value)
                                newRecords.add(record)
                            }
                            "Credit" -> {
                                val record = IptcRecord(IptcTypes.CREDIT, tag.value)
                                newRecords.add(record)
                            }
                            "Copyright Notice" -> {
                                val record = IptcRecord(IptcTypes.COPYRIGHT_NOTICE, tag.value)
                                newRecords.add(record)
                            }
                            "Source" -> {
                                val record = IptcRecord(IptcTypes.SOURCE, tag.value)
                                newRecords.add(record)
                            }
                        }
                    }

                    /* Prepare and write transferable metadata (if available). */
                    if (newRecords.isNotEmpty()) {
                        output = ByteArrayOutputStream().use { os ->
                            JpegIptcRewriter().writeIptc(output, os, PhotoshopApp13Data(newRecords, emptyList()))
                            os.toByteArray()
                        }
                    }
                }
                else -> continue
            }
        }

        /* Store file. */
        Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
            it.write(output)
        }
        true
    } catch (e: IOException) {
        LOGGER.error("Failed to save image $path due to IO exception: ${e.message}")
        false
    }
}