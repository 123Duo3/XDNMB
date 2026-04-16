import SwiftUI
import Shared

#if canImport(UIKit)
import UIKit
#endif
#if canImport(AppKit)
import AppKit
#endif

@Observable
@MainActor
final class ThreadListViewModel {
    var threads: [NmbPost] = []
    var isRefreshing = false
    var isLoadingMore = false
    var errorMessage: String?
    private(set) var loadedPage: Int32 = 0

    private let repository: ForumRepository
    private let bridge: AppleForumBridge
    private let source: CatalogSource
    private var observationTask: Task<Void, Never>?

    init(repository: ForumRepository, bridge: AppleForumBridge, source: CatalogSource) {
        self.repository = repository
        self.bridge = bridge
        self.source = source
    }

    func start() {
        observationTask?.cancel()
        observationTask = Task { [weak self, repository, source] in
            let flow = SkieSwiftFlow(repository.observeCatalog(source: source))
            for await list in flow {
                if Task.isCancelled { break }
                await MainActor.run {
                    self?.threads = list
                }
            }
        }
        Task { [weak self] in await self?.refresh() }
    }

    func stop() {
        observationTask?.cancel()
        observationTask = nil
    }

    func refresh() async {
        isRefreshing = true
        defer { isRefreshing = false }
        do {
            let outcome = try await bridge.refreshCatalog(
                source: source,
                page: Int32(1),
                replaceLoadedPages: true
            )
            if outcome.isSuccess {
                loadedPage = 1
                errorMessage = nil
            } else if let message = outcome.errorMessage {
                print("[ThreadList] refresh failed: \(message) kind=\(outcome.errorKind ?? "?")")
                errorMessage = message
            }
        } catch {
            print("[ThreadList] refresh threw: \(error)")
            errorMessage = error.localizedDescription
        }
    }

    func loadNextPageIfNeeded(currentItem: NmbPost) async {
        guard !threads.isEmpty,
              currentItem.id == threads.last?.id,
              !isLoadingMore else { return }
        isLoadingMore = true
        defer { isLoadingMore = false }
        let nextPage: Int32 = max(1, loadedPage) + 1
        do {
            let outcome = try await bridge.refreshCatalog(
                source: source,
                page: nextPage,
                replaceLoadedPages: false
            )
            if outcome.isSuccess {
                loadedPage = nextPage
            } else if let message = outcome.errorMessage {
                print("[ThreadList] page \(nextPage) failed: \(message)")
                errorMessage = message
            }
        } catch {
            print("[ThreadList] page \(nextPage) threw: \(error)")
            errorMessage = error.localizedDescription
        }
    }
}

struct ThreadListScreen: View {
    @Environment(AppContainer.self) private var container
    let selection: CatalogSelection
    @State private var viewModel: ThreadListViewModel?

    var body: some View {
        Group {
            if let viewModel {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.threads, id: \.id) { post in
                            ThreadListRow(
                                post: post,
                                forumName: post.forumId.flatMap { container.forumNameMap[$0.int64Value] }
                            )
                            .task { await viewModel.loadNextPageIfNeeded(currentItem: post) }
                        }

                        if viewModel.isLoadingMore {
                            ProgressView().padding(.vertical, 8)
                        }

                        if !viewModel.threads.isEmpty, let error = viewModel.errorMessage {
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundStyle(.orange)
                                Text(error).font(.footnote).foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 8)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
                .background(Color.platformGroupedBackground)
                .refreshable { await viewModel.refresh() }
                .overlay {
                    if viewModel.threads.isEmpty {
                        if viewModel.isRefreshing {
                            ProgressView()
                        } else if let error = viewModel.errorMessage {
                            ContentUnavailableView {
                                Label("加载失败", systemImage: "wifi.exclamationmark")
                            } description: {
                                Text(error)
                            } actions: {
                                Button("重试") {
                                    Task { await viewModel.refresh() }
                                }
                                .buttonStyle(.borderedProminent)
                            }
                        }
                    }
                }
            } else {
                ProgressView()
            }
        }
        .navigationTitle(selection.displayName)
#if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
#endif
        .navigationDestination(for: Int64.self) { threadId in
            ThreadDetailScreen(threadId: threadId)
        }
        .task {
            if viewModel == nil {
                let vm = ThreadListViewModel(
                    repository: container.forumRepository,
                    bridge: container.forumBridge,
                    source: selection.toShared()
                )
                viewModel = vm
                vm.start()
            }
        }
        .onDisappear {
            viewModel?.stop()
        }
    }
}

private struct ThreadListRow: View {
    let post: NmbPost
    let forumName: String?

    var body: some View {
        NavigationLink(value: post.threadId) {
            PostCardView(post: post, isThreadHeader: true, forumName: forumName)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(Color.platformSecondaryGroupedBackground)
                )
        }
        .buttonStyle(.plain)
    }
}

private extension Color {
    static var platformGroupedBackground: Color {
    #if canImport(UIKit)
        return Color(UIColor.systemGroupedBackground)
    #elseif canImport(AppKit)
        return Color(NSColor.windowBackgroundColor)
    #else
        return .gray.opacity(0.1)
    #endif
    }
    static var platformSecondaryGroupedBackground: Color {
    #if canImport(UIKit)
        return Color(UIColor.secondarySystemGroupedBackground)
    #elseif canImport(AppKit)
        return Color(NSColor.controlBackgroundColor)
    #else
        return .gray.opacity(0.2)
    #endif
    }
}
