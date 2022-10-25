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

    var body: some View {
        NavigationView {
            List() {
                ForEach(forumList) { forumGroup in
                    Section(header: Text(forumGroup.name)) {
                        ForEach(forumGroup.forums) { forum in
                            if ((forum.showName?.isEmpty) != nil) {
                                Text(forum.name)
                            } else {
                                Text(forum.showName!)
                            }
                        }
                    }
                }
            }
            .navigationTitle("板块")
        }
    }
}
