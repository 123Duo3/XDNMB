import SwiftUI

/// First-iteration NMB rich text renderer.
/// Just shows the pre-computed plain text from the shared parser. A future
/// pass will map shared `NmbRichText` segments (links, references, colors) to
/// `AttributedString` ranges with custom URL schemes for >>No.XXX taps.
struct NmbRichTextView: View {
    let html: String
    let plainText: String

    var body: some View {
        Text(plainText)
            .textSelection(.enabled)
            .fixedSize(horizontal: false, vertical: true)
    }
}
