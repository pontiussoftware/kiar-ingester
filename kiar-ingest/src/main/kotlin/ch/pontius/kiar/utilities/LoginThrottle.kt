package ch.pontius.kiar.utilities

import ch.pontius.kiar.api.model.status.ErrorStatusException
import ch.pontius.kiar.utilities.LoginThrottle.WINDOW
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-memory throttling of failed login attempts.
 *
 * Failures are counted per (lower-cased) username and per client address within a sliding [WINDOW]. Once a counter
 * reaches its limit, further attempts for that key are rejected with HTTP 429 until the window has passed. The
 * per-user limit protects an account against password guessing regardless of where the attempts come from; the
 * (much higher) per-address limit slows down password spraying across many accounts. A successful login resets the
 * user's counter.
 *
 * Note: behind a reverse proxy without forwarded-header handling, all clients share the proxy's address; the per-address
 * limit is therefore deliberately generous.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object LoginThrottle {

    /** Failed attempts per username before further attempts are rejected. */
    const val MAX_FAILURES_PER_USER = 5

    /** Failed attempts per client address before further attempts are rejected. */
    const val MAX_FAILURES_PER_ADDRESS = 100

    /** The window after which failure counters expire. */
    private val WINDOW: Duration = Duration.ofMinutes(15)

    /** Failure counters by username. */
    private val byUser = Caffeine.newBuilder().expireAfterWrite(WINDOW).maximumSize(100_000).build<String, AtomicInteger>()

    /** Failure counters by client address. */
    private val byAddress = Caffeine.newBuilder().expireAfterWrite(WINDOW).maximumSize(100_000).build<String, AtomicInteger>()

    /**
     * Ensures that a login attempt for [username] from [address] is currently permitted.
     *
     * @throws ErrorStatusException (429) if either limit has been reached.
     */
    fun check(username: String, address: String) {
        val userFailures = this.byUser.getIfPresent(this.key(username))?.get() ?: 0
        val addressFailures = this.byAddress.getIfPresent(address)?.get() ?: 0
        if (userFailures >= MAX_FAILURES_PER_USER || addressFailures >= MAX_FAILURES_PER_ADDRESS) {
            throw ErrorStatusException(429, "Too many failed login attempts. Please try again in ${WINDOW.toMinutes()} minutes.")
        }
    }

    /**
     * Records a failed login attempt.
     */
    fun failure(username: String, address: String) {
        this.byUser.get(this.key(username)) { AtomicInteger(0) }.incrementAndGet()
        this.byAddress.get(address) { AtomicInteger(0) }.incrementAndGet()
    }

    /**
     * Records a successful login, which clears the user's failure counter.
     */
    fun success(username: String) {
        this.byUser.invalidate(this.key(username))
    }

    private fun key(username: String): String = username.trim().lowercase()
}
