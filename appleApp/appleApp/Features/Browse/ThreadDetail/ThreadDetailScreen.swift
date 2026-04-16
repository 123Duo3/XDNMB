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
final class ThreadDetailViewModel {
    var detail: ThreadDetail?
    var isLoading = false
    var errorMessage: String?

    private let repository: ForumRepository
    private let bridge: AppleForumBridge
    private let threadId: Int64
    private var observationTask: Task<Void, Never>?

    init(repository: ForumRepository, bridge: AppleForumBridge, threadId: Int64) {
        self.repository = repository
        self.bridge = bridge
        self.threadId = threadId
    }

    func start() {
        observationTask?.cancel()
        observationTask = Task { [weak self, repository, threadId] in
            let flow = SkieSwiftFlow(repository.observeThreadDetail(threadId: threadId))
            for await detail in flow {
                if Task.isCancelled { break }
                await MainActor.run {
                    self?.detail = detail
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
        isLoading = true
        defer { isLoading = false }
        do {
            let outcome = try await bridge.refreshThread(threadId: threadId)
            if outcome.isSuccess {
                errorMessage = nil
            } else if let message = outcome.errorMessage {
                print("[ThreadDetail] refresh failed: \(message) kind=\(outcome.errorKind ?? "?")")
                errorMessage = message
            }
        } catch {
            print("[ThreadDetail] refresh threw: \(error)")
            errorMessage = error.localizedDescription
        }
    }
}

// MARK: - Screen

struct ThreadDetailScreen: View {
    @Environment(AppContainer.self) private var container
    let threadId: Int64
    @State private var viewModel: ThreadDetailViewModel?

    var body: some View {
        Group {
            if let viewModel {
                List {
                    if let thread = viewModel.detail?.thread {
                        Section {
                            ForEach(viewModel.detail?.posts ?? [], id: \.id) { post in
                                ReplyRow(post: post, isPoster: post.isPoster)
                            }
                        } header: {
                            ThreadOpHeader(post: thread)
                        } footer: {
                            HStack {
                                Spacer()
                                if viewModel.isLoading {
                                    ProgressView()
                                } else if let error = viewModel.errorMessage {
                                    HStack(spacing: 6) {
                                        Image(systemName: "exclamationmark.triangle.fill")
                                            .foregroundStyle(.orange)
                                        Text(error)
                                            .foregroundStyle(.secondary)
                                    }
                                } else {
                                    Text("到底了")
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                            }
                            .font(.footnote)
                            .padding(.vertical, 10)
                            .textCase(nil)
                        }
                    }
                }
#if os(iOS)
                .listStyle(.grouped)
#else
                .listStyle(.inset)
#endif
                .refreshable { await viewModel.refresh() }
                .overlay {
                    if viewModel.detail?.thread == nil {
                        if viewModel.isLoading {
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
        .navigationTitle("No.\(String(threadId))")
#if os(iOS)
        .navigationBarTitleDisplayMode(.large)
#endif
        .task {
            if viewModel == nil {
                let vm = ThreadDetailViewModel(
                    repository: container.forumRepository,
                    bridge: container.forumBridge,
                    threadId: threadId
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

// MARK: - Thread OP Section Header

private struct ThreadOpHeader: View {
    @Environment(AppContainer.self) private var container
    let post: NmbPost

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Hash · reply count · time
            HStack(alignment: .center) {
                Text(post.userHash)
                    .font(.system(.footnote, design: .monospaced))
                    .foregroundColor(post.admin ? .red : .platformSecondaryLabel)
                if let count = post.replyCount?.intValue {
                    Label(String(count), systemImage: "bubble.right")
                        .padding(.leading, 8)
                }
                Label(container.forumBridge.formatTime(
                    epochMillis: post.postedAtEpochMillis,
                    settings: container.timeSettings
                ), systemImage: "clock")
                .padding(.leading, 8)
            }
            .font(.caption)
            .padding(.top, -16)
            .padding(.bottom, 8)

            // Title / name
            VStack(alignment: .leading, spacing: 1) {
                if let title = container.forumBridge.normalizeTitle(title: post.title) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(.platformLabel)
                }
                if let name = container.forumBridge.normalizeName(name: post.name) {
                    Text(name)
                        .font(.subheadline)
                        .foregroundColor(.platformLabel)
                }
            }
            .padding(.bottom, 1)

            // Body
            NmbRichTextView(html: post.contentHtml, plainText: post.contentText)
                .font(.callout)
                .foregroundColor(.platformLabel)
                .lineSpacing(2.4)
                .textCase(nil)

            // Image
            if let thumbUrlString = container.forumBridge.buildThumbUrl(image: post.image, ext: post.ext),
               let url = URL(string: thumbUrlString) {
                NmbThreadImagePreview(url: url) {
                    let fallback = try? await container.forumBridge.buildFallbackThumbUrl(
                        image: post.image,
                        ext: post.ext,
                        currentUrl: thumbUrlString
                    )
                    return fallback.flatMap { URL(string: $0) }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 4)
            }
        }
        .padding(.bottom, 16)
        .textCase(nil)
    }
}

// MARK: - Reply Row

private struct ReplyRow: View {
    @Environment(AppContainer.self) private var container
    let post: NmbPost
    let isPoster: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header: PO · No.id · hash  |  SAGE  |  time
            HStack {
                if isPoster {
                    Text("PO")
                        .foregroundColor(.accentColor)
                        .fontWeight(.semibold)
                    Text("•").foregroundColor(.gray)
                    Text("No.\(String(post.remoteId))")
                        .font(.system(.caption, design: .monospaced))
                    Text("•").foregroundColor(.gray)
                    Text(post.userHash)
                        .font(.system(.caption, design: .monospaced))
                        .foregroundColor(post.admin ? .red : .primary)
                } else {
                    Text("No.\(String(post.remoteId))")
                        .font(.system(.caption, design: .monospaced))
                    Text("•").foregroundColor(.gray)
                    Text(post.userHash)
                        .font(.system(.caption, design: .monospaced))
                        .foregroundColor(post.admin ? .red : .primary)
                }
                if post.sage {
                    Text("•").foregroundColor(.gray)
                    Text("SAGE")
                        .fontWeight(.semibold)
                        .foregroundColor(.red)
                }
                Spacer()
                Text(container.forumBridge.formatTime(
                    epochMillis: post.postedAtEpochMillis,
                    settings: container.timeSettings
                ))
            }
            .font(.caption)
            .padding(.bottom, 4)

            // Title / name
            VStack(alignment: .leading, spacing: 1) {
                if let title = container.forumBridge.normalizeTitle(title: post.title) {
                    Text(title).font(.headline)
                }
                if let name = container.forumBridge.normalizeName(name: post.name) {
                    Text(name).font(.subheadline).foregroundColor(.gray)
                }
            }
            .padding(.bottom, 1)

            // Body
            NmbRichTextView(html: post.contentHtml, plainText: post.contentText)
                .font(.callout)
                .lineSpacing(2.4)

            // Image
            if let thumbUrlString = container.forumBridge.buildThumbUrl(image: post.image, ext: post.ext),
               let url = URL(string: thumbUrlString) {
                NmbThreadImagePreview(url: url) {
                    let fallback = try? await container.forumBridge.buildFallbackThumbUrl(
                        image: post.image,
                        ext: post.ext,
                        currentUrl: thumbUrlString
                    )
                    return fallback.flatMap { URL(string: $0) }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 4)
            }
        }
    }
}

private extension Color {
    static var platformLabel: Color {
    #if canImport(UIKit)
        return Color(UIColor.label)
    #elseif canImport(AppKit)
        return Color(NSColor.labelColor)
    #else
        return .primary
    #endif
    }
    static var platformSecondaryLabel: Color {
    #if canImport(UIKit)
        return Color(UIColor.secondaryLabel)
    #elseif canImport(AppKit)
        return Color(NSColor.secondaryLabelColor)
    #else
        return .secondary
    #endif
    }
}
