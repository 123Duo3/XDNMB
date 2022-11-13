//
//  FroumThreadView.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/5.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct ForumThreadView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    var sdk: XdSDK
    var forumId: String
    var forumShowName: String
    
    var body: some View {
        listView()
            .refreshable(action: {self.viewModel.refreshForumThread(forceReload: true)})
    }
    
    private func listView() -> AnyView {
        switch viewModel.threads {
        case .loading:
            return AnyView(
                Text("加载中...")
                    .navigationTitle(forumShowName)
            )
        case.result(let threads):
            return AnyView(
                ThreadsListRow(forumId: forumId, forumShowName: forumShowName, threadList: threads, sdk: sdk, loadNextPage: ())
                    .navigationTitle(forumShowName)
            )
        case.error(let discription):
            return AnyView(
                Text(discription).multilineTextAlignment(.center)
                    .navigationTitle(forumShowName)
            )
        }
    }
}

extension ForumThreadView {
    enum LoadableForumThread {
        case loading
        case result([shared.Thread])
        case error(String)
    }
    
    class ViewModel: ObservableObject {
        let sdk: XdSDK
        let forumId: String
        @Published var threads = LoadableForumThread.loading
        
        init(sdk: XdSDK, forumId: String) {
            self.sdk = sdk
            self.forumId = forumId
            self.loadForumThread(forumId: self.forumId,forceReload: true)
        }
        
        func loadForumThread(forumId: String, forceReload: Bool) {
            self.threads = .loading
            sdk.getForumThreads(forumId: Int32(forumId)!, forceReload: forceReload, page: 1, completionHandler: { thread, error in
                if let thread = thread {
                    self.threads = .result(thread)
                } else {
                    self.threads = .error(error?.localizedDescription ?? "错误")
                }
            })
        }
        
        func refreshForumThread(forceReload: Bool) {
            sdk.getForumThreads(forumId: Int32(forumId)!, forceReload: forceReload, page: 1, completionHandler: { thread, error in
                if let thread = thread {
                    self.threads = .result(thread)
                } else {
                    self.threads = .error(error?.localizedDescription ?? "错误")
                }
            })
        }
    }
}

