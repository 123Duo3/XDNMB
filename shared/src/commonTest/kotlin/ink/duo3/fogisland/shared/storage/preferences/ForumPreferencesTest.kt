package ink.duo3.fogisland.shared.storage.preferences

import kotlin.test.Test
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
}
