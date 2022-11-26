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
        switch viewModel.replies {
        case .loading:
            return AnyView(
                Text("加载中...")
                    .navigationTitle("No." + threadId)
            )
        case.result(let threads):
            return AnyView(
                InThreadRow(sdk: sdk, replyList: threads, forumId: forumId, viewModel: self.viewModel)
                    .navigationTitle("No." + threadId)
                    .toolbar{
                        Image(systemName: "arrowshape.turn.up.left")
                            .foregroundColor(Color.accentColor)
                            .padding(.trailing, 9)
                        if (viewModel.currentPage <= 50) {
                            Image(systemName: String(viewModel.currentPage) + ".square")
                                .foregroundColor(Color.accentColor)
                        } else {
                            Image("xd.square")
                                .foregroundColor(Color.accentColor)
                        }
                        Menu(content: {
                            Button(action: {}, label: {Label("只看Po主", systemImage: "p.circle")})
                            Button(action: {}, label: {Label("添加订阅", systemImage: "bookmark")})
                            Button(action: {}, label: {Label("举报", systemImage: "flag")})
                            Button(action: {}, label: {Label("分享", systemImage: "square.and.arrow.up")})
                        }, label: {
                            Image(systemName: "ellipsis.circle")
                        })
                    }
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
    
    enum LoadaleNextPage {
        case loading
        case success
        case error(String)
    }
    
    class ViewModel: ObservableObject {
        let sdk: XdSDK
        let threadId: Int
        var page: Int
        var replyList: [shared.Thread]
        var maxPage = 0
        @Published var replies = LoadableReply.loading
        @Published var currentPage = 1
        @Published var nextPage = LoadaleNextPage.success
        
        init(sdk: XdSDK, threadId: Int, page: Int) {
            self.sdk = sdk
            self.threadId = threadId
            self.page = page
            self.replyList = []
            self.loadReply(threadId: self.threadId)
        }
        
        func loadReply(threadId: Int) {
            self.replies = .loading
            sdk.getReply(threadId: Int32(threadId), page: Int32(page), completionHandler: {thread, error in
                if let thread = thread {
                    self.replies = .result(thread)
                    self.replyList = thread.replies ?? []
                    self.maxPage = Int(ceil(Double(truncating: thread.replyCount!) / 19))
                    print("Max Page:", self.maxPage)
                } else {
                    self.replies = .error(error?.localizedDescription ?? "错误")
                }
            })
        }
        
        func loadNextPage(threadId: Int) {
            if case .success = nextPage {
                if (page + 1 <= maxPage) {
                    self.nextPage = .loading
                    sdk.getReply(threadId: Int32(threadId), page: Int32(page + 1), completionHandler: {thread, error in
                        if let thread = thread {
                            DispatchQueue.main.async {
                                self.replyList += thread.replies ?? []
                                let replies = thread.doCopy(id: thread.id, fid: thread.fid, replyCount: thread.replyCount, img: thread.img, ext: thread.ext, time: thread.time, userHash: thread.userHash, name: thread.name, title: thread.title, content: thread.content, sage: thread.sage, admin: thread.admin, hide: thread.hide, replies: self.replyList, remainReplies: thread.remainReplies, email: thread.email, poster: thread.poster, page: thread.page, forumName: thread.forumName)
                                self.replies = .result(replies)
                                self.page += 1
                                self.maxPage = Int(ceil(Double(truncating: thread.replyCount!) / 19))
                                print("Max Page:", self.maxPage)
                                self.nextPage = .success
                            }
                        } else {
                            self.nextPage = .error(error?.localizedDescription ?? "错误")
                        }
                    })
                }
            }
        }
        
        func updateCurrentPage(page: Int) {
            DispatchQueue.main.async {
                self.currentPage = page
            }
        }
    }
}

