import SwiftUI
import Shared

/// Identifier for whatever the user has selected in the sidebar.
/// Mirrors the shared `CatalogSource` but stays Codable-friendly for `@SceneStorage`
/// later if we want persistence.
enum CatalogSelection: Hashable {
    case timeline(id: Int64, displayName: String)
    case forum(id: Int64, displayName: String)

    var displayName: String {
        switch self {
        case .timeline(_, let name), .forum(_, let name): return name
        }
    }

    func toShared() -> CatalogSource {
        switch self {
        case .timeline(let id, let name):
            return CatalogSource(type: .timeline, id: id, title: name, subtitle: nil)
        case .forum(let id, let name):
            return CatalogSource(type: .forum, id: id, title: name, subtitle: nil)
        }
    }
}

/// "板块" tab. NavigationSplitView shows the forum tree on the leading column
/// and the catalog (timeline / forum thread list) on the trailing column.
/// On iPhone the split collapses to a stack; we pre-select the default timeline
/// so the first launch lands on content (Files-app-style).
struct BrowseSplitView: View {
    @State private var selection: CatalogSelection? = .timeline(id: -1, displayName: "时间线")
    @State private var columnVisibility: NavigationSplitViewVisibility = .automatic

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            ForumSidebarView(selection: $selection)
                .navigationTitle("板块")
        } detail: {
            if let selection {
                NavigationStack {
                    ThreadListScreen(selection: selection)
                }
                .id(selection)
            } else {
                ContentUnavailableView("选择一个板块", systemImage: "rectangle.stack")
            }
        }
        .navigationSplitViewStyle(.balanced)
    }
}
