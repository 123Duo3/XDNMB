import SwiftUI

struct FavoritesScreen: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "收藏",
                systemImage: "bookmark",
                description: Text("收藏功能尚未实现")
            )
            .navigationTitle("收藏")
        }
    }
}
