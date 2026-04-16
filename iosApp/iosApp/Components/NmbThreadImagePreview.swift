import SwiftUI

/// Image thumbnail for a thread/post, matching Android's ThreadImagePreview sizing:
/// - Fixed height: 128 pt
/// - Min width:     64 pt
/// - Max width:    full container width
/// - Width derived from the loaded image's intrinsic aspect ratio, clamped to [min, max]
/// - Corner radius: 8 pt
struct NmbThreadImagePreview: View {
    let url: URL

    private enum LoadState { case loading, loaded(UIImage), failed }
    @State private var state: LoadState = .loading

    var body: some View {
        GeometryReader { geo in
            imageContent(maxWidth: geo.size.width)
        }
        .frame(height: 128)
        .task(id: url) {
            state = .loading
            guard
                let (data, _) = try? await URLSession.shared.data(from: url),
                let img = UIImage(data: data)
            else {
                state = .failed
                return
            }
            state = .loaded(img)
        }
    }

    @ViewBuilder
    private func imageContent(maxWidth: CGFloat) -> some View {
        let w = previewWidth(maxWidth: maxWidth)
        Group {
            switch state {
            case .loading:
                Color(.systemFill).overlay(ProgressView())
            case .loaded(let img):
                Image(uiImage: img)
                    .resizable()
                    .scaledToFill()
                    .clipped()
            case .failed:
                Color(.systemFill)
                    .overlay(Image(systemName: "photo").foregroundStyle(.tertiary))
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
