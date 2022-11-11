//
//  ForumListRow.swift
//  iosApp
//
//  Created by 123哆3 on 2022/10/22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct ForumListRow: View {
    var forumList: [ForumGroup]
    let sdk = XdSDK(databaseDriverFactory: DatabaseDriverFactory())
    
    var body: some View {
        NavigationView {
            List() {
                ForEach(forumList) { forumGroup in
                    Section(header: Text(forumGroup.name)) {
                        ForEach(forumGroup.forums) { forum in
                            let forumId = forum.id
                            NavigationLink(destination: {
                                if (forumId == "-1") {
                                    TimelineView(viewModel: .init(sdk: sdk), sdk: sdk)
                                } else {
                                    ForumThreadView(viewModel: .init(sdk: sdk, forumId: forum.id), sdk: sdk, forumId: forum.id, forumShowName: forum.name)
                                }
                            }){
                                Forum(name: forum.name, showName: forum.showName)
                            }
                        }
                    }
                }
            }
            .navigationTitle("板块")
        }
    }
}

struct Forum: View {
    var name: String
    let showName: String?
    @State private var htmlText: NSAttributedString?
    
    var body: some View {
        LazyHStack {
            if (showName ?? "" == ""){
                Text(name)
            } else {
                if let htmlText {
                    Text(AttributedString(htmlText))
                }
            }
        }.onAppear{
            DispatchQueue.main.async {
                if (showName == "") {
                    htmlText = name.htmlAttributedString()
                } else {
                    htmlText = (showName ?? name).htmlAttributedString()
                }
            }
        }
    }
}
