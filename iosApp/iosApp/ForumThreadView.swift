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
    @State var isShowAlert: Bool = false
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
                ThreadsListRow(forumId: forumId, forumShowName: forumShowName, threadList: threads, sdk: sdk, viewModel: nil, notice: nil)
                    .navigationTitle(forumShowName)
            )
        case.error(let discription):
            return AnyView(
                VStack {
                    if(discription != "Illegal input"){
                        Text("Oops! 加载失败(´Д` )")
                            .font(.headline)
                    } else {
                        Text("饼干权限不足！(>д<)")
                            .font(.headline)
                    }
                    Text("")
                    HStack {
                        Button("查看错误信息") {
                            self.isShowAlert.toggle()
                        }
                        Button("重试") {
                            viewModel.refreshForumThread(forceReload: true)
                        }
                    }
                    .buttonStyle(.bordered)
                    .alert(isPresented: $isShowAlert) {
                        Alert(title: Text("错误信息"), message: Text(discription), dismissButton: .default(Text("确定")))
                    }
                    .navigationTitle(forumShowName)
                }
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

