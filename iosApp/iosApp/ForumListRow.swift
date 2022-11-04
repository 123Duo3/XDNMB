//
//  ForumListRow.swift
//  iosApp
//
//  Created by 123哆3 on 2022/10/22.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared
import RichText

struct ForumListRow: View {
    var forumList: [ForumGroup]

    var body: some View {
        NavigationView {
            List() {
                ForEach(forumList) { forumGroup in
                    Section(header: Text(forumGroup.name)) {
                        ForEach(forumGroup.forums) { forum in
                            if ((forum.showName?.isEmpty) == true) {
                                RichText(html: forum.name).padding(-8)
                            } else {
                                RichText(html: forum.showName ?? forum.name).padding(-8)
                            }
                        }
                    }
                }
            }
            .navigationTitle("板块")
        }
    }
}
