package ch.pontius.kiar.servers.oai

/**
 * Enumeration of OAI-PMH verbs.
 *
 * See [OAI-PMH Protocol](https://www.openarchives.org/OAI/openarchivesprotocol.html#VerbDefinitions)
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Verbs {
    IDENTIFY,
    LISTSETS,
    LISTMETADATAFORMATS,
    LISTIDENTIFIERS,
    LISTRECORDS,
    GETRECORD
}