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
    var sdk: XdSDK
    
    var body: some View {
        listView()
            .navigationTitle(Text("时间线"))
            .refreshable(action: {await self.viewModel.refreshTimeline(forceReload: true)})
    }
    
    private func listView() -> AnyView {
        switch viewModel.timeline {
        case .loading:
            return AnyView(
                NavigationView{
                    Text("加载中...")
                }
                    .navigationTitle("时间线")
            )
        case.result(let timeline):
            return AnyView(
                ThreadsListRow(forumId: "-1", forumShowName: "时间线", threadList: timeline, sdk: sdk, viewModel: self.viewModel)
            )
        case.error(let discription):
            return AnyView(Text(discription).multilineTextAlignment(.center))
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
        @Published var timeline = LoadableTimeline.loading
        var threads: [shared.Thread]
        var currentPage: Int32
        
        init(sdk: XdSDK) {
            self.sdk = sdk
            self.threads = []
            self.currentPage = 1
            self.loadTimeline(forceReload: true)
        }
        
        func loadTimeline(forceReload: Bool) {
            self.timeline = .loading
            sdk.getTimeLine(forceReload: forceReload, page: 1, completionHandler: { thread, error in
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
        func refreshTimeline(forceReload: Bool) async{
            DispatchQueue.main.async {
                self.sdk.getTimeLine(forceReload: forceReload, page: 1, completionHandler: { thread, error in
                    if let thread = thread {
                        DispatchQueue.main.async {
                            self.timeline = .result(thread)
                            self.threads = thread
                            self.currentPage = 1
                        }
                    } else {
                        self.timeline = .error(error?.localizedDescription ?? "错误")
                    }
                })
            }
        }
        
        func loadNextPage() {
            self.sdk.getTimeLine(forceReload: true, page: currentPage+1, completionHandler: {threadList, error in
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
    }
}
