package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NmbTipsTest {

    @Test
    fun matchesOnlyWhenUserHashRemoteIdAndSentinelTimeAllMatch() {
        assertTrue(
            isNmbTipsPost(
                userHash = "Tips",
                remotePostId = 9_999_999L,
                postedAtRaw = "2099-01-01 00:00:01"
            )
        )
    }

    @Test
    fun rejectsSpoofedNameOnlyCasesByIgnoringMutableFields() {
        assertFalse(
            isNmbTipsPost(
                userHash = "Tips",
                remotePostId = 123_456L,
                postedAtRaw = "2099-01-01 00:00:01"
            )
        )
    }

    @Test
    fun rejectsWrongTimestampEvenWhenRemoteIdMatches() {
        assertFalse(
            isNmbTipsPost(
                userHash = "Tips",
                remotePostId = 9_999_999L,
                postedAtRaw = "2026-03-24(二)02:55:51"
            )
        )
    }

    @Test
    fun rejectsNonTipsUserHashEvenWhenOtherSentinelsMatch() {
        assertFalse(
            isNmbTipsPost(
                userHash = "nmbxd",
                remotePostId = 9_999_999L,
                postedAtRaw = "2099-01-01 00:00:01"
            )
        )
    }
}
