import SwiftUI
import shared

struct ContentView: View {
    let sdk = XdSDK(databaseDriverFactory: DatabaseDriverFactory())
    
	var body: some View {
        TabView {
            ForumView(viewModel: .init(sdk: sdk))
                .tabItem{
                    Label("板块", systemImage: "tray.2.fill")
                }
            SubscribeView()
                .tabItem{
                    Label("订阅", systemImage: "star")
                }
            SelectionView(sdk: sdk)
                .tabItem{
                    Label("选项", systemImage: "gear")
                }
        }
	}
}

extension ForumGroup: Identifiable { }
extension Forum_: Identifiable { }
extension shared.Forum: Identifiable { }
extension shared.Thread: Identifiable { }
