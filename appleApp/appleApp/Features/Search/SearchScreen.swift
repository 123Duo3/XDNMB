import SwiftUI

struct SearchScreen: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "搜索",
                systemImage: "magnifyingglass",
                description: Text("搜索功能尚未实现")
            )
            .navigationTitle("搜索")
        }
    }
}
