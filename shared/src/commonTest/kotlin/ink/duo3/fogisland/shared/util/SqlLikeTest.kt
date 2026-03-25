package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class SqlLikeTest {

    @Test
    fun escapesLikeWildcardsAndEscapeChar() {
        assertEquals(
            expected = """foo\%bar\_baz\\tail""",
            actual = escapeSqlLikeArgument("""foo%bar_baz\tail""")
        )
    }

    @Test
    fun keepsPlainTextUntouched() {
        assertEquals(
            expected = "normal-text-123",
            actual = escapeSqlLikeArgument("normal-text-123")
        )
    }
}
