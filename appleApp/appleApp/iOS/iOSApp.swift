#if os(iOS)
import SwiftUI

@main
struct iOSApp: App {
    @State private var container = AppContainer.shared

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environment(container)
        }
    }
}
#endif
