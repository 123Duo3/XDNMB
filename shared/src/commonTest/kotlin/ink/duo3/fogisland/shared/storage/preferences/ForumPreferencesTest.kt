package ink.duo3.fogisland.shared.storage.preferences

import ink.duo3.fogisland.shared.model.CacheCleanupTtlPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForumPreferencesTest {

    @Test
    fun subscriptionUuidAllowsLengthUpTo64() {
        assertTrue(isSubscriptionUuidFormatValid("a".repeat(64)))
        assertFalse(isSubscriptionUuidFormatValid("a".repeat(65)))
    }

    @Test
    fun normalizeSubscriptionUuidRejectsTooLongInput() {
        assertNotNull(normalizeSubscriptionUuid("abc123"))
        assertNull(normalizeSubscriptionUuid("a".repeat(65)))
    }

    @Test
    fun cleanupTtlPolicyDefaultsToNeverForBlank() {
        assertEquals(CacheCleanupTtlPolicy.NEVER, decodeCleanupTtlPolicy(null))
        assertEquals(CacheCleanupTtlPolicy.NEVER, decodeCleanupTtlPolicy(""))
        assertEquals(CacheCleanupTtlPolicy.NEVER, decodeCleanupTtlPolicy("  "))
    }

    @Test
    fun cleanupTtlPolicyFallsBackToNeverForUnknownValue() {
        assertEquals(CacheCleanupTtlPolicy.NEVER, decodeCleanupTtlPolicy("UNKNOWN_POLICY"))
    }

    @Test
    fun cleanupTtlPolicyParsesKnownValue() {
        assertEquals(
            CacheCleanupTtlPolicy.THREE_MONTHS,
            decodeCleanupTtlPolicy(CacheCleanupTtlPolicy.THREE_MONTHS.name)
        )
    }
}
