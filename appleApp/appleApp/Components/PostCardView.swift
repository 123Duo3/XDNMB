import SwiftUI
import Shared

/// Card mode (isThreadHeader=true) mirrors Android's NmbPostCard.
/// Flat item mode (isThreadHeader=false) mirrors NmbPostFlatItem.
struct PostCardView: View {
    @Environment(AppContainer.self) private var container
    let post: NmbPost
    let isThreadHeader: Bool
    var forumName: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            headerRow
            contentBlock
            if isThreadHeader {
                footerRow
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    // MARK: Header

    /// Card mode (outside thread):
    ///   hash(semibold, red if admin) [· SAGE 👎]           timestamp
    ///
    /// Flat item mode (inside thread):
    ///   No.id · hash(bold+accent if PO, red if admin) [· PO] [· SAGE 👎]  timestamp
    private var headerRow: some View {
        HStack(alignment: .center, spacing: 0) {
            // Flat item prepends "No.id · "
            if !isThreadHeader {
                Text("No.\(String(post.remoteId)) · ")
                    .foregroundStyle(.secondary)
            }

            // Hash: in card mode isPoster has no effect on color/weight
            Text(post.userHash)
                .monospaced()
                .fontWeight(!isThreadHeader && post.isPoster ? .bold : .regular)
                .foregroundStyle(
                    post.admin          ? Color.red :
                    (!isThreadHeader && post.isPoster) ? Color.accentColor :
                    Color.secondary
                )

            // "· PO" label: flat item only
            if !isThreadHeader && post.isPoster {
                Text(" · PO")
                    .foregroundStyle(.accent)
            }

            if post.sage {
                Text(" · SAGE")
                    .foregroundStyle(.red)
                Image(systemName: "hand.thumbsdown.fill")
                    .font(.system(size: 10))
                    .foregroundStyle(.red)
                    .padding(.leading, 2)
            }

            Spacer(minLength: 8)

            Text(container.forumBridge.formatTime(
                epochMillis: post.postedAtEpochMillis,
                settings: container.timeSettings
            ))
            .foregroundStyle(.secondary)
        }
        .font(.caption)
        .padding(.bottom, 8)
    }

    // MARK: Content

    private var contentBlock: some View {
        let thumbUrlString = container.forumBridge.buildThumbUrl(image: post.image, ext: post.ext)
        let thumbUrl = thumbUrlString.flatMap { URL(string: $0) }
        return NmbPostContent(
            title: container.forumBridge.normalizeTitle(title: post.title),
            subtitle: container.forumBridge.normalizeName(name: post.name),
            contentHtml: post.contentHtml,
            contentText: post.contentText,
            imageUrl: thumbUrl,
            fallbackImageUrl: {
                guard let thumbUrlString else { return nil }
                let fallback = try? await container.forumBridge.buildFallbackThumbUrl(
                    image: post.image,
                    ext: post.ext,
                    currentUrl: thumbUrlString
                )
                return fallback.flatMap { URL(string: $0) }
            },
            bodyMaxLines: isThreadHeader ? 6 : nil
        )
    }

    // MARK: Footer (card mode only)

    private var footerRow: some View {
        HStack(alignment: .center) {
            // "No.{id}" + optional " · forumName" — mirrors Android buildNmbContextText
            Group {
                if let name = forumName, !name.isEmpty {
                    Text("No.\(String(post.remoteId))") + Text(" · ") + Text(name)
                } else {
                    Text("No.\(String(post.remoteId))")
                }
            }
            .foregroundStyle(.secondary)

            Spacer()

            if let count = post.replyCount?.intValue {
                Label("\(count)", systemImage: "bubble.right")
                    .foregroundStyle(.secondary)
            }
        }
        .font(.caption)
        .padding(.top, 8)
    }

}
