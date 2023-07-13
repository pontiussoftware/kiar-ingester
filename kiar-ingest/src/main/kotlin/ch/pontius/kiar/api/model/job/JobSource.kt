package ch.pontius.kiar.api.model.job

/**
 * The source of a [Job], i.e., from where it was created.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class JobSource {
    WATCHER,
    WEB;
}