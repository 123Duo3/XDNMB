package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTextTest {

    @Test
    fun ignoresSourceLineBreaksAfterHtmlBreaks() {
        assertEquals(
            expected = "第一行\n第二行",
            actual = htmlToPlainText("第一行<br>\r\n第二行")
        )
    }

    @Test
    fun preservesDoubleBreaksFromHtml() {
        assertEquals(
            expected = "第一行\n\n第二行",
            actual = htmlToPlainText("第一行<br><br>\r\n\r\n第二行")
        )
    }

    @Test
    fun preservesHtmlWhitespaceAroundLineEdges() {
        assertEquals(
            expected = "  ASCII \n 艺术  ",
            actual = htmlToPlainText("  ASCII <br> 艺术  ")
        )
    }
}
