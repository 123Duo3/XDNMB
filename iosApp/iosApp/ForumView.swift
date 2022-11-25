//
//  ForumView.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/3.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct ForumView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    
    var body: some View {
        listView()
    }
    
    private func listView() -> AnyView {
        switch viewModel.forumList {
        case .loading:
            return AnyView(
                NavigationView{
                    Text("加载中...")
                }
                    .navigationTitle("板块")
            )
        case.result(let forumList):
            return AnyView(
                ForumListRow(forumList: forumList)
            )
        case.error(let discription):
            return AnyView(Text(discription).multilineTextAlignment(.center))
        }
    }
}

extension ForumView {
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
            self.loadForumList(forceReload: false)
        }
        
        func loadForumList(forceReload: Bool) {
            self.forumList = .loading
            sdk.getForumList(forceReload: forceReload, completionHandler: { forumList, error in
                if let forumList = forumList {
                    DispatchQueue.main.async {
                        self.forumList = .result(forumList)
                    }
                } else {
                    DispatchQueue.main.async {
                        self.forumList = .error(error?.localizedDescription ?? "错误")
                    }
                }
            })
        }
    }
}
