package ch.pontius.kiar.ingester.parsing.xml

import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory

/**
 * Factory methods for XML parsers that process untrusted input (uploaded XML files and KIAR metadata entries).
 *
 * All factories returned here have DTD processing, external entity resolution and XInclude disabled, which
 * prevents XML external entity (XXE) attacks and entity expansion attacks (e.g. "billion laughs").
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object XmlFactories {

    /** Disallows DOCTYPE declarations altogether; the most effective XXE mitigation. */
    private const val DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl"

    /** Disables resolution of external general entities. */
    private const val EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities"

    /** Disables resolution of external parameter entities. */
    private const val EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities"

    /** Disables loading of external DTDs. */
    private const val LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd"

    /**
     * Creates a new, hardened [SAXParserFactory].
     *
     * @return [SAXParserFactory]
     */
    fun newSaxParserFactory(): SAXParserFactory = SAXParserFactory.newInstance().apply {
        this.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        this.setFeature(DISALLOW_DOCTYPE, true)
        this.setFeature(EXTERNAL_GENERAL_ENTITIES, false)
        this.setFeature(EXTERNAL_PARAMETER_ENTITIES, false)
        this.setFeature(LOAD_EXTERNAL_DTD, false)
        this.isXIncludeAware = false
        this.isNamespaceAware = false
        this.isValidating = false
    }

    /**
     * Creates a new, hardened [DocumentBuilderFactory].
     *
     * @return [DocumentBuilderFactory]
     */
    fun newDocumentBuilderFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        this.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        this.setFeature(DISALLOW_DOCTYPE, true)
        this.setFeature(EXTERNAL_GENERAL_ENTITIES, false)
        this.setFeature(EXTERNAL_PARAMETER_ENTITIES, false)
        this.setFeature(LOAD_EXTERNAL_DTD, false)
        this.isXIncludeAware = false
        this.isExpandEntityReferences = false
        this.isValidating = false
    }
}
