import SwiftUI
import Shared

@Observable
@MainActor
final class ForumSidebarViewModel {
    var timelines: [Timeline] = []
    var forumGroups: [ForumGroup] = []
    var isLoading = false
    var errorMessage: String?

    private let bridge: AppleForumBridge
    private let container: AppContainer

    init(bridge: AppleForumBridge, container: AppContainer) {
        self.bridge = bridge
        self.container = container
    }

    func load() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let result = try await bridge.loadSidebar()
            self.timelines = result.timelines
            self.forumGroups = result.forumGroups
            if result.outcome.isSuccess {
                self.errorMessage = nil
            } else if let message = result.outcome.errorMessage {
                print("[Sidebar] load failed: \(message) kind=\(result.outcome.errorKind ?? "?")")
                self.errorMessage = message
            }
        } catch {
            print("[Sidebar] load threw error: \(error)")
            self.timelines = []
            self.forumGroups = []
            self.errorMessage = friendlyMessage(for: error)
        }
        // Always update the name map with whatever we have, even on partial success.
        // Mirror Android's buildForumNameMap: prefer name, fall back to displayName
        container.forumNameMap = Dictionary(
            uniqueKeysWithValues: forumGroups
                .flatMap(\.forums)
                .map { forum in
                    let label = forum.name.isEmpty ? forum.displayName : forum.name
                    return (forum.id, label)
                }
        )
    }
}
struct ForumSidebarView: View {
    @Environment(AppContainer.self) private var container
    @Binding var selection: CatalogSelection?
    @State private var viewModel: ForumSidebarViewModel?
    /// IDs of sections the user has collapsed. Empty = all expanded.
    @State private var collapsedGroups: Set<Int64> = []
    @State private var timelinesExpanded = true

    var body: some View {
        List(selection: $selection) {
            if let viewModel {
                if !viewModel.timelines.isEmpty {
                    Section(isExpanded: $timelinesExpanded) {
                        ForEach(viewModel.timelines, id: \.id) { timeline in
                            SidebarRow(title: timeline.displayName)
                                .tag(CatalogSelection.timeline(
                                    id: timeline.id,
                                    displayName: timeline.displayName
                                ))
                        }
                    } header: {
                        Text("时间线")
                    }
                }
                ForEach(viewModel.forumGroups, id: \.id) { group in
                    Section(isExpanded: .init(
                        get: { !collapsedGroups.contains(group.id) },
                        set: { isExpanded in
                            if isExpanded { collapsedGroups.remove(group.id) }
                            else { collapsedGroups.insert(group.id) }
                        }
                    )) {
                        ForEach(group.forums, id: \.id) { forum in
                            SidebarRow(title: forum.displayName)
                                .tag(CatalogSelection.forum(
                                    id: forum.id,
                                    displayName: forum.displayName
                                ))
                        }
                    } header: {
                        Text(group.name)
                    }
                }
                if !viewModel.forumGroups.isEmpty, let errorMessage = viewModel.errorMessage {
                    HStack(spacing: 8) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundStyle(.orange)
                        Text(errorMessage).font(.footnote).foregroundStyle(.secondary)
                    }
                }
            }
        }
        .listStyle(.sidebar)
        .overlay {
            if let viewModel, viewModel.forumGroups.isEmpty {
                if viewModel.isLoading {
                    ProgressView()
                } else if let error = viewModel.errorMessage {
                    ContentUnavailableView {
                        Label("无法加载板块", systemImage: "wifi.exclamationmark")
                    } description: {
                        Text(error)
                    } actions: {
                        Button("重试") {
                            Task { await viewModel.load() }
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
            }
        }
        .task {
            if viewModel == nil {
                viewModel = ForumSidebarViewModel(bridge: container.forumBridge, container: container)
            }
            await viewModel?.load()
            // After first successful load, replace the placeholder default
            // selection with the real first timeline so push picks it up.
            if case .timeline(id: -1, _) = selection,
               let first = viewModel?.timelines.first {
                selection = .timeline(id: first.id, displayName: first.displayName)
            }
        }
    }
}
