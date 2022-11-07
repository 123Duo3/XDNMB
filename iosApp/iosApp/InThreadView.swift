//
//  InThreadView.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/5.
//  Copyright © 2022 orgName. All rights reserved.
//

/*
import SwiftUI
import shared

struct InThreadView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    var sdk: XdSDK
    var threadId: String
    
    var body: some View {
        listView()
    }
    
    
    private func listView() -> AnyView{
        
    }
}

extension InThreadView {
    enum LoadableReply {
        case loading
        case result([shared.Thread])
        case error(String)
    }
    
    class ViewModel: ObservedObject {
        let sdk: XdSDK
        let threadId: Int
        @Published var replys = LoadableReply.loading
        
        init(sdk: XdSDK, threadId: Int) {
            self.sdk = sdk
            self.threadId = threadId
            self.loadReply(threadId: self.threadId, forceReload: false)
        }
        
        func loadReply(threadId: Int, forceReload: Bool) {
            self.replys = .loading
            sdk.
        }
    }
}
*/
