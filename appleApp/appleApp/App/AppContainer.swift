import Foundation
import Observation
import Shared

/// Process-wide container holding the shared KMP `ForumRepository` singleton.
/// Resolved once at app launch and injected into the SwiftUI environment.
@Observable
final class AppContainer {
    static let shared = AppContainer()

    let forumRepository: ForumRepository
    let forumBridge: AppleForumBridge

    /// Populated by ForumSidebarViewModel after the forum list loads.
    /// Used by thread list / post cards to resolve forumId → displayName.
    var forumNameMap: [Int64: String] = [:]

    /// Current time-display preferences, kept in sync with the shared DataStore.
    var timeSettings = ForumTimeSettings(
        useUtcPlus8Time: true,
        usePreciseTime: false,
        showSeconds: false
    )

    private init() {
        let provider = RepositoryProvider.shared
        self.forumRepository = provider.forumRepository
        self.forumBridge = provider.forumBridge
        startObservingTimeSettings()
    }

    private func startObservingTimeSettings() {
        Task { @MainActor in
            let flow = SkieSwiftFlow(self.forumBridge.timeSettingsFlow)
            for await settings in flow {
                self.timeSettings = settings
            }
        }
    }
}
