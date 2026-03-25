package ink.duo3.fogisland.shared.repository

import ink.duo3.fogisland.shared.model.CacheCleanupTtlPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForumRepositoryCleanupPolicyTest {

    @Test
    fun resolveCleanupExpireBeforeReturnsNullForNever() {
        assertNull(
            resolveCleanupExpireBefore(
                policy = CacheCleanupTtlPolicy.NEVER,
                nowEpochMillis = 1_000_000L
            )
        )
    }

    @Test
    fun resolveCleanupExpireBeforeSubtractsTtl() {
        assertEquals(
            10_000_000L - 30L * 24 * 60 * 60 * 1000,
            resolveCleanupExpireBefore(
                policy = CacheCleanupTtlPolicy.ONE_MONTH,
                nowEpochMillis = 10_000_000L
            )
        )
        assertEquals(
            10_000_000L - 92L * 24 * 60 * 60 * 1000,
            resolveCleanupExpireBefore(
                policy = CacheCleanupTtlPolicy.THREE_MONTHS,
                nowEpochMillis = 10_000_000L
            )
        )
    }
}
