#if os(macOS)
import SwiftUI

/// Root window content for the macOS app.
/// Uses BrowseSplitView as the primary interface — NavigationSplitView
/// behaves as a proper sidebar+detail layout on macOS automatically.
struct RootWindowView: View {
    var body: some View {
        BrowseSplitView()
    }
}
#endif
