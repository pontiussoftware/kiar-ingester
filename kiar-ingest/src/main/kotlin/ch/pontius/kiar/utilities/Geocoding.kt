package ch.pontius.kiar.utilities

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class to perform geocoding.
 *
 * @see https://nominatim.org/release-docs/develop/api/Reverse/
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object Geocoding {

    /** The [Json] parser used by [Geocoding]. */
    private val JSON = Json { ignoreUnknownKeys = true }

    /** The [Logger] used by this [Geocoding]. */
    private val LOGGER: Logger = LogManager.getLogger()

    /**
     * Attempts geocoding to obtain coordinates for the provided address.
     *
     * @param address The address to obtain coordinates for.
     * @return [GeocodingResponse]
     */
    fun geocode(street: String?, city: String, zip: Int): GeocodingResponse? = try {
        /** Reads the search pattern from the parameters map.*/
        val url: URL = if (street != null) {
            URL("https://nominatim.openstreetmap.org/search?street=${street.replace(' ', '+')}&city=${city.replace(' ', '+')}&postalcode=$zip&format=json&limit=1&email=info@kimnet.ch")
        } else {
            URL("https://nominatim.openstreetmap.org/search?city=${city.replace(' ', '+')}&postalcode=$zip&format=json&limit=1&email=info@kimnet.ch")
        }
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Kiar/1.0.0; +https://www.kimnet.ch)")
        if (connection.getResponseCode() != 200) {
            LOGGER.error("Failed to generate coordinates from address: HTTP Status Code ${connection.getResponseCode()}")
            null
        } else {
            val response = connection.inputStream.use {
                this.JSON.decodeFromStream<List<GeocodingResponse>>(it)
            }
            response.firstOrNull()
        }
    } catch (e: Throwable) {
        LOGGER.error("Failed to generate coordinates from address.", e)
        null
    }


    /**
     * A response as generated by the [Geocoding] class.
     */
    @Serializable
    data class GeocodingResponse(val display_name: String, val lat: Float, val lon: Float, val type: String)
}