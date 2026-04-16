#if canImport(UIKit)
import UIKit
public typealias PlatformImage = UIImage
#elseif canImport(AppKit)
import AppKit
public typealias PlatformImage = NSImage
#endif

import SwiftUI

#if canImport(UIKit)
private extension Image {
    init(platformImage: PlatformImage) { self = Image(uiImage: platformImage) }
}
#elseif canImport(AppKit)
private extension Image {
    init(platformImage: PlatformImage) { self = Image(nsImage: platformImage) }
}
#endif

/// Image thumbnail for a thread/post, matching Android's ThreadImagePreview sizing:
/// - Fixed height: 128 pt
/// - Min width:     64 pt
/// - Max width:    full container width
/// - Width derived from the loaded image's intrinsic aspect ratio, clamped to [min, max]
/// - Corner radius: 8 pt
struct NmbThreadImagePreview: View {
    let url: URL
    let fallbackUrl: (() async -> URL?)?

    init(url: URL, fallbackUrl: (() async -> URL?)? = nil) {
        self.url = url
        self.fallbackUrl = fallbackUrl
    }

    private func makePlatformImage(from data: Data) -> PlatformImage? {
    #if canImport(UIKit)
        return UIImage(data: data)
    #elseif canImport(AppKit)
        return NSImage(data: data)
    #else
        return nil
    #endif
    }

    private enum LoadState { case loading, loaded(PlatformImage), failed }
    @State private var state: LoadState = .loading

    var body: some View {
        GeometryReader { geo in
            imageContent(maxWidth: geo.size.width)
        }
        .frame(height: 128)
        .task(id: url) {
            state = .loading
            if let img = await loadImage(from: url) {
                state = .loaded(img)
                return
            }
            if let fallbackUrl,
               let fallback = await fallbackUrl(),
               fallback != url,
               let img = await loadImage(from: fallback) {
                state = .loaded(img)
                return
            }
            state = .failed
        }
    }

    private func loadImage(from url: URL) async -> PlatformImage? {
        guard let (data, _) = try? await URLSession.shared.data(from: url) else {
            return nil
        }
        return makePlatformImage(from: data)
    }

    @ViewBuilder
    private func imageContent(maxWidth: CGFloat) -> some View {
        let w = previewWidth(maxWidth: maxWidth)
        Group {
            switch state {
            case .loading:
                ZStack {
                    Color.secondary.opacity(0.2)
                    ProgressView()
                }
            case .loaded(let img):
                Image(platformImage: img)
                    .resizable()
                    .scaledToFill()
                    .clipped()
            case .failed:
                ZStack {
                    Color.secondary.opacity(0.2)
                    Image(systemName: "photo").foregroundStyle(.tertiary)
                }
            }
        }
        .frame(width: w, height: 128)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }

    private func previewWidth(maxWidth: CGFloat) -> CGFloat {
        let height: CGFloat = 128
        let minWidth: CGFloat = 64
        guard case .loaded(let img) = state, img.size.height > 0 else {
            // While loading: square, capped to available width
            return min(height, maxWidth)
        }
        let natural = height * (img.size.width / img.size.height)
        return min(max(natural, minWidth), maxWidth)
    }
}
