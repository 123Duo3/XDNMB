//
//  InThreadView.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/5.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct InThreadView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    var sdk: XdSDK
    var threadId: String
    let forumId: String
    
    var body: some View {
        listView()
    }
    
    
    private func listView() -> AnyView{
        switch viewModel.replys {
        case .loading:
            return AnyView(
                Text("加载中...")
                    .navigationTitle("No." + threadId)
            )
        case.result(let threads):
            return AnyView(
                InThreadRow(sdk: sdk, replyList: threads, forumId: forumId)
                    .navigationTitle("No." + threadId)
            )
        case.error(let discription):
            return AnyView(
                Text(discription).multilineTextAlignment(.center)
                    .navigationTitle("No." + threadId)
            )
        }
    }
}

extension InThreadView {
    enum LoadableReply {
        case loading
        case result(shared.Thread)
        case error(String)
    }
    
    class ViewModel: ObservableObject {
        let sdk: XdSDK
        let threadId: Int
        let page: Int
        @Published var replys = LoadableReply.loading
        
        init(sdk: XdSDK, threadId: Int, page: Int) {
            self.sdk = sdk
            self.threadId = threadId
            self.page = page
            self.loadReply(threadId: self.threadId, page: self.page)
        }
        
        func loadReply(threadId: Int, page: Int) {
            self.replys = .loading
            sdk.getReply(threadId: Int32(threadId), page: Int32(page), completionHandler: {thread, error in
                if let thread = thread {
                    self.replys = .result(thread)
                } else {
                    self.replys = .error(error?.localizedDescription ?? "错误")
                }
            })
        }
    }
}

