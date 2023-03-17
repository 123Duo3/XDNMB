//
//  TimelineView.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/4.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct TimelineView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    @State var isShowAlert: Bool = false
    var sdk: XdSDK
    var forumId: String
    var forumShowName: String
    
    var body: some View {
        listView()
            .navigationTitle(forumShowName)
            .refreshable(action: {await self.viewModel.refreshTimeline(forceReload: true)})
    }
    
    private func listView() -> AnyView {
        switch viewModel.timeline {
        case .loading:
            return AnyView(
                Text("加载中...")
                    .navigationTitle(forumShowName)
            )
        case.result(let timeline):
            return AnyView(
                ThreadsListRow(forumId: "-" + forumId, forumShowName: forumShowName, threadList: timeline, sdk: sdk, viewModel: self.viewModel, notice: viewModel.notice)
                    .navigationTitle(forumShowName)
            )
        case.error(let discription):
            return AnyView(
                VStack {
                    Text("Oops! 加载失败(´Д` )")
                        .bold()
                    Text("")
                    HStack {
                        Button("查看错误信息") {
                            self.isShowAlert.toggle()
                        }
                        Button("重试") {
                            Task {
                                await viewModel.refreshTimeline(forceReload: true)
                            }
                        }
                    }
                    .alert(isPresented: $isShowAlert) {
                        Alert(title: Text("错误信息"), message: Text(discription), dismissButton: .default(Text("确定")))
                    }
                }
            )
        }
    }
}

extension TimelineView {
    enum LoadableTimeline {
        case loading
        case result([shared.Thread])
        case error(String)
    }
    
    class ViewModel: ObservableObject {
        let sdk: XdSDK
        let forumId: String
        @Published var timeline = LoadableTimeline.loading
        @Published var notice: shared.Notice? = nil
        var threads: [shared.Thread]
        var currentPage: Int32
        
        init(sdk: XdSDK, forumId: String) {
            self.forumId = forumId
            self.sdk = sdk
            self.threads = []
            self.currentPage = 1
            self.loadTimeline(forumId: forumId, forceReload: true)
        }
        
        func loadTimeline(forumId: String, forceReload: Bool) {
            self.timeline = .loading
            sdk.getCurrentNotice(completionHandler: { notice, error in
                if let notice = notice {
                    DispatchQueue.main.async {
                        self.notice = notice
                        print("getNotice")
                    }
                } else {
                    print(error?.localizedDescription ?? "Error")
                }
            })
            sdk.getTimeLine(forumId: Int32(forumId)!, forceReload: forceReload, page: 1, completionHandler: { thread, error in
                if let thread = thread {
                    DispatchQueue.main.async {
                        self.timeline = .result(thread)
                        self.threads = thread
                        print("getTimeline")
                    }
                } else {
                    DispatchQueue.main.async {
                        self.timeline = .error(error?.localizedDescription ?? "错误")
                    }
                }
            })
        }
        
        @MainActor
        func refreshTimeline(forceReload: Bool) async {
            DispatchQueue.main.async {
                self.sdk.getTimeLine(forumId: Int32(self.forumId)!, forceReload: forceReload, page: 1, completionHandler: { thread, error in
                    if let thread = thread {
                        self.threads = thread
                        self.currentPage = 1
                    } else {
                        self.timeline = .error(error?.localizedDescription ?? "错误")
                    }
                })
                self.timeline = .result(self.threads)
            }
        }
        
        func loadNextPage() {
            self.sdk.getTimeLine(forumId: Int32(forumId)!, forceReload: true, page: currentPage+1, completionHandler: {threadList, error in
                if let threadList = threadList {
                    DispatchQueue.main.async {
                        let timelineSet = NSMutableOrderedSet(array: self.threads)
                        timelineSet.addObjects(from: threadList)
                        self.threads = timelineSet.array as? [shared.Thread] ?? self.threads
                        self.currentPage += 1
                        self.timeline = .result(self.threads)
                        print("LoadNextPage:", self.currentPage)
                    }
                } else {
                    self.timeline = .error(error?.localizedDescription ?? "错误")
                }
            })
        }
        
        func dismissNotice() {
            self.sdk.dismissCurrentNotice(completionHandler: { error in
                if let error = error {
                    print(error.localizedDescription)
                }
            })
        }
    }
}
