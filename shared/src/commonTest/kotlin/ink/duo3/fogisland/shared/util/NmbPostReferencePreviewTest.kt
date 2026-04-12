package ink.duo3.fogisland.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NmbPostReferencePreviewTest {
    @Test
    fun `collect reference candidates from rich text and numeric tokens`() {
        val richText = parseNmbRichText("前文 >>123456<br>No.12345<br>12345678")

        assertEquals(
            setOf(123456L, 12345L, 12345678L),
            collectNmbPostReferenceCandidates(richText)
        )
    }

    @Test
    fun `linkify numeric references only when predicate allows`() {
        val linked = linkifyNmbPureNumericReferences(
            richText = parseNmbRichText("前文 1234 12345678"),
            shouldLink = { postId, rawText -> postId == 1234L || rawText.length == 8 }
        )

        val linkedIds = linked.segments.mapNotNull {
            (it.linkTarget as? NmbLinkTarget.PostReference)?.postId
        }
        assertEquals(listOf(1234L, 12345678L), linkedIds)
    }

    @Test
    fun `split lines and detect standalone reference line`() {
        val richText = parseNmbRichText(">>123456<br>正文<br> No.12345 ")
        val lines = splitNmbRichTextLines(richText)

        assertEquals(3, lines.size)
        assertEquals(123456L, findStandaloneNmbPostReferenceId(lines[0]))
        assertNull(findStandaloneNmbPostReferenceId(lines[1]))
        assertEquals(12345L, findStandaloneNmbPostReferenceId(lines[2]))
    }

    @Test
    fun `split lines preserving breaks can round trip styled newlines`() {
        val richText = parseNmbRichText("[h]黑幕一<br>黑幕二[/h]<br>正文")

        val restored = joinNmbRichTextLines(
            splitNmbRichTextLinesPreservingBreaks(richText)
        )

        assertEquals(richText, restored)
    }

    @Test
    fun `do not treat decimals as numeric references`() {
        val richText = parseNmbRichText("价格是 12.34，不是引用")

        assertTrue(collectNmbPostReferenceCandidates(richText).isEmpty())
    }

    @Test
    fun `do not collect quoted thread ids or small roll ranges as post reference candidates`() {
        val richText = parseNmbRichText(">>12345678 <small>&gt;87654321</small> >>123456")

        assertEquals(
            setOf(123456L),
            collectNmbPostReferenceCandidates(richText)
        )
    }
}
