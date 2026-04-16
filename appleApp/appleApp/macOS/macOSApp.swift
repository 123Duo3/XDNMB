#if os(macOS)
import SwiftUI

@main
struct macOSApp: App {
    @State private var container = AppContainer.shared

    var body: some Scene {
        WindowGroup {
            RootWindowView()
                .environment(container)
        }
        .commands {
            // macOS menu commands go here
        }
    }
}
#endif
