import SwiftUI
import Shared

@Observable
@MainActor
final class ThreadDetailViewModel {
    var detail: ThreadDetail?
    var isLoading = false
    var errorMessage: String?

    private let repository: ForumRepository
    private let bridge: IosForumBridge
    private let threadId: Int64
    private var observationTask: Task<Void, Never>?

    init(repository: ForumRepository, bridge: IosForumBridge, threadId: Int64) {
        self.repository = repository
        self.bridge = bridge
        self.threadId = threadId
    }

    func start() {
        observationTask?.cancel()
        observationTask = Task { [repository, threadId] in
            let flow = SkieSwiftFlow(repository.observeThreadDetail(threadId: threadId))
            for await detail in flow {
                self.detail = detail
            }
        }
        Task { await refresh() }
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
                .listStyle(.grouped)
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
        .navigationBarTitleDisplayMode(.large)
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
                    .foregroundColor(post.admin ? .red : Color(UIColor.secondaryLabel))
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
                        .foregroundColor(Color(UIColor.label))
                }
                if let name = container.forumBridge.normalizeName(name: post.name) {
                    Text(name)
                        .font(.subheadline)
                        .foregroundColor(Color(UIColor.label))
                }
            }
            .padding(.bottom, 1)

            // Body
            NmbRichTextView(html: post.contentHtml, plainText: post.contentText)
                .font(.callout)
                .foregroundColor(Color(UIColor.label))
                .lineSpacing(2.4)
                .textCase(nil)

            // Image
            if let url = container.forumBridge.buildThumbUrl(image: post.image, ext: post.ext)
                .flatMap({ URL(string: $0) }) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFit()
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    case .failure:
                        Color.gray.opacity(0.17)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    default:
                        Color.gray.opacity(0.17)
                            .overlay(ProgressView())
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
                .scaledToFit()
                .frame(height: 100)
                .clipShape(RoundedRectangle(cornerRadius: 8))
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
            if let url = container.forumBridge.buildThumbUrl(image: post.image, ext: post.ext)
                .flatMap({ URL(string: $0) }) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFit()
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    case .failure:
                        Color.gray.opacity(0.17)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    default:
                        Color.gray.opacity(0.17)
                            .overlay(ProgressView())
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
                .scaledToFit()
                .frame(maxHeight: 100, alignment: .leading)
                .padding(.top, 4)
            }
        }
    }
}

