#if os(iOS)
import SwiftUI

/// Bottom tab bar — the "spine" of the iOS app. Only the Browse tab is wired
/// to real data in this iteration; the others are placeholders so we can
/// commit to the information architecture early.
struct RootTabView: View {
    var body: some View {
        TabView {
            Tab("板块", systemImage: "rectangle.stack") {
                BrowseSplitView()
            }
            Tab("收藏", systemImage: "bookmark") {
                FavoritesScreen()
            }
            Tab("设置", systemImage: "gearshape") {
                SettingsScreen()
            }
            Tab(role: .search) {
                SearchScreen()
            }
        }
    }
}
#endif
