//
//  HistoryView.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/11.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct HistoryView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    var sdk: XdSDK
    
    var body: some View {
        listView()
            .onAppear(
                perform: {viewModel.loadHistory()}
            )
    }
    
    private func listView() -> AnyView {
        switch viewModel.threads {
        case .loading:
            return AnyView(
                Text("加载中...")
                    .navigationTitle("历史记录")
            )
        case.result(let threads):
            return AnyView(
                ThreadsListRow(forumId: "-1", forumShowName: "历史记录", threadList: threads, sdk: sdk)
                    .navigationTitle("历史记录")
            )
        case.error(let discription):
            return AnyView(
                Text(discription).multilineTextAlignment(.center)
                    .navigationTitle("历史记录")
            )
        }
    }
}

extension HistoryView {
    enum LoadableHistory {
        case loading
        case result([shared.Thread])
        case error(String)
    }
    
    class ViewModel: ObservableObject {
        let sdk: XdSDK
        @Published var threads = LoadableHistory.loading
        
        init(sdk: XdSDK) {
            self.sdk = sdk
            self.loadHistory()
        }
        
        func loadHistory(){
            self.threads = .loading
            sdk.getHistory(completionHandler: { thread, error in
                if let thread = thread {
                    self.threads = .result(thread)
                } else {
                    self.threads =
                        .error(error?.localizedDescription ?? "错误")
                }
            })
        }
    }
}
