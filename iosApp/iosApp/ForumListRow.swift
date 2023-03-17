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
        VStack {
            List {
                Section {
                    VStack(alignment: .center) {
                        HStack {
                            Spacer()
                            Text("没有收藏")
                                .foregroundColor(.gray)
                            Spacer()
                        }
                        HStack {
                            Spacer()
                            Text("滑动板块来收藏")
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                            Spacer()
                        }
                    }
                } header: {
                    Text("收藏").bold().font(.callout)
                }
                ForEach(forumList) { forumGroup in
                    Section(header: Text(forumGroup.name).bold().font(.callout)) {
                        ForEach(forumGroup.forums) { forum in
                            let forumGroupId = forum.fgroup
                            NavigationLink {
                                if (forumGroupId == "-1") {
                                    TimelineView(viewModel: .init(sdk: sdk, forumId: forum.id), sdk: sdk, forumId: forum.id, forumShowName: forum.name)
                                } else {
                                    ForumThreadView(viewModel: .init(sdk: sdk, forumId: forum.id), sdk: sdk, forumId: forum.id, forumShowName: forum.name)
                                }
                            } label: {
                                Forum(name: forum.name, showName: forum.showName)
                            }
                        }
                    }
                }
            }
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
