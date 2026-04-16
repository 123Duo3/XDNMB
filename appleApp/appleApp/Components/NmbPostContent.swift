import SwiftUI

/// Content block for a post — mirrors Android's NmbPostContent composable.
/// Renders: title (titleLarge) → name/subtitle (titleMedium) → body → image preview.
/// All sub-items separated by 8 pt vertical spacing.
struct NmbPostContent: View {
    let title: String?
    let subtitle: String?      // post.name
    let contentHtml: String
    let contentText: String
    let imageUrl: URL?
    var fallbackImageUrl: (() async -> URL?)? = nil
    var bodyMaxLines: Int? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if hasTitleBlock {
                VStack(alignment: .leading, spacing: 2) {
                    if let t = title {
                        Text(t).font(.title3.bold())
                    }
                    if let s = subtitle {
                        Text(s).font(.headline).foregroundStyle(.secondary)
                    }
                }
            }

            if !contentText.isEmpty || !contentHtml.isEmpty {
                NmbRichTextView(html: contentHtml, plainText: contentText)
                    .font(.callout)
                    .lineLimit(bodyMaxLines)
            }

            if let url = imageUrl {
                NmbThreadImagePreview(url: url, fallbackUrl: fallbackImageUrl)
            }
        }
    }

    private var hasTitleBlock: Bool {
        title != nil || subtitle != nil
    }
}
