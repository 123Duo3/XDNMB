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
    @State var isShowAlert: Bool = false
    
    var body: some View {
        NavigationView {
            listView()
                .navigationTitle("板块")
        }
    }
    
    private func listView() -> AnyView {
        switch viewModel.forumList {
        case .loading:
            return AnyView(
                Text("加载中...(　ﾟ 3ﾟ)")
            )
        case.result(let forumList):
            return AnyView(
                ForumListRow(forumList: forumList)
            )
        case.error(let discription):
            return AnyView(
                VStack {
                    Text("Oops! 加载失败(;´Д`)")
                        .font(.headline)
                    Text("")
                    HStack {
                        Button("查看错误信息") {
                            self.isShowAlert.toggle()
                        }
                        Button("重试") {
                            viewModel.loadForumList(forceReload: true)
                        }
                    }
                    .buttonStyle(.bordered)
                    .alert(isPresented: $isShowAlert) {
                        Alert(title: Text("错误信息"), message: Text(discription), dismissButton: .default(Text("确定")))
                    }
                }
            )
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
