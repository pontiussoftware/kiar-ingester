package ch.pontius.kiar.utilities

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.sessions.*
import java.time.Duration

/**
 * An in-memory [SessionStorage] backed by a Caffeine cache that expires sessions after a period of inactivity.
 *
 * This reproduces the behaviour of a servlet container's in-memory session store (idle timeout, sessions are lost on restart).
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class CaffeineSessionStorage(idleTimeout: Duration = Duration.ofMinutes(30)) : SessionStorage {

    /** The cache holding the serialized session data. */
    private val sessions = Caffeine.newBuilder().expireAfterAccess(idleTimeout).maximumSize(10_000).build<String, String>()

    override suspend fun write(id: String, value: String) {
        this.sessions.put(id, value)
    }

    override suspend fun read(id: String): String = this.sessions.getIfPresent(id) ?: throw NoSuchElementException("Session $id not found.")

    override suspend fun invalidate(id: String) {
        this.sessions.invalidate(id)
    }
}
