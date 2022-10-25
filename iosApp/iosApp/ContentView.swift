import SwiftUI
import shared

struct ContentView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    
	var body: some View {
		listView()
	}
    
    private func listView() -> AnyView {
        switch viewModel.forumList {
        case .loading:
            return AnyView(Text("加载中...").multilineTextAlignment(.center))
        case.result(let forumList):
            return AnyView(
                ForumListRow(forumList: forumList)
            )
        case.error(let discription):
            return AnyView(Text(discription).multilineTextAlignment(.center))
        }
    }
}

extension ContentView {
    enum LoadableForumList {
        case loading
        case result([ForumGroup])
        case error(String)
    }
    
    class ViewModel: ObservableObject {
        let sdk: XdSDK
        @Published var forumList = LoadableForumList.loading
        
        init(sdk: XdSDK) {
            self.sdk = sdk
            self.loadForumList(forceReload: true)
        }
        
        func loadForumList(forceReload: Bool) {
            self.forumList = .loading
            sdk.getForumList(forceReload: forceReload, completionHandler: { forumList, error in
                if let forumList = forumList {
                    self.forumList = .result(forumList)
                } else {
                    self.forumList = .error(error?.localizedDescription ?? "错误")
                }
            })
        }
    }
}

extension ForumGroup: Identifiable { }
extension Forum_ :Identifiable { }
