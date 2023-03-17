//
//  PostsListRow.swift
//  iosApp
//
//  Created by 123哆3 on 2022/10/29.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct ThreadsListRow: View {
    var forumId: String
    var forumShowName: String
    var threadList: [shared.Thread]
    let sdk: XdSDK
    let viewModel: TimelineView.ViewModel?
    let notice: shared.Notice?
    @State private var isShowingNotice = true
    
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        ScrollView {
            LazyVStack(alignment: .center, spacing: 16) {
                if (notice != nil) {
                    if isShowingNotice {
                        Notice(notice: notice!, dismiss:
                                {
                            viewModel?.dismissNotice()
                            withAnimation(.spring()){
                                isShowingNotice.toggle()
                            }
                        }, sdk: sdk)
                            .transition(.scale(scale: 0.95).combined(with: .opacity))
                    }
                }
                ForEach(threadList) {threads in
                    Threads(threads: threads, forumId: forumId, sdk: sdk)
                        .onAppear(perform: {
                            if (threads.id == threadList[threadList.count - 3].id) {
                                viewModel?.loadNextPage()
                            }
                        })
                }
                Button("Next", action: {
                    viewModel?.loadNextPage()
                })
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight:.infinity)
            .background(Color (UIColor.systemGroupedBackground))
        }
        .background(Color (UIColor.systemGroupedBackground))
    }
}

func isAdmin(admin: Int32)->Color{
    if (admin == 0){
        return Color(UIColor.systemGray)
    } else {
        return Color(UIColor.systemRed)
    }
}

struct Threads: View {
    let threads: shared.Thread
    let forumId: String
    let sdk: XdSDK
    @Environment(\.colorScheme) var colorScheme
    @State private var htmlText: NSAttributedString?
    
    var body: some View {
        NavigationLink {
            InThreadView(viewModel: .init(sdk: sdk, threadId: Int(threads.id), page: 1), sdk: sdk, threadId: String(threads.id), forumId: forumId)
        } label: {
            VStack(alignment: .leading) {
                HStack {
                    Text(threads.userHash)
                        .foregroundColor(isAdmin(admin: threads.admin))
                        .font(.system(.footnote, design: .monospaced))
                    Spacer()
                    if(forumId == "-1" || forumId == "-2" || forumId == "-3") {
                        Text(sdk.getForumName(forumId: Int32(truncating: threads.fid!)))
                        Text("•")
                            .foregroundColor(Color.gray)
                    }
                    Text("No." + String(threads.id))
                        .foregroundColor(Color.gray)
                        .font(.system(.footnote, design: .monospaced))
                    if (threads.sage == 1) {
                        Text("•")
                            .foregroundColor(Color.gray)
                        Text("SAGE")
                            .fontWeight(.semibold)
                            .foregroundColor(Color.red)
                    }
                }
                .padding(.bottom, 1.0)
                .font(.caption)
                .foregroundColor(Color(UIColor.label))
                
                VStack(alignment: .leading) {
                    if (threads.title != "无标题") {
                        Text(threads.title)
                        .font(.headline)
                        .foregroundColor(Color(UIColor.label))
                        .multilineTextAlignment(.leading)
                    }
                    if (threads.name != "无名氏") {
                        Text(threads.name)
                            .font(.subheadline)
                            .foregroundColor(Color.gray)
                            .multilineTextAlignment(.leading)
                    }
                }
                .padding(.bottom, 1.0)
                
                VStack(alignment: .leading) {
                    if let htmlText {
                        Text(AttributedString(htmlText))
                            .multilineTextAlignment(.leading)
                            .lineLimit(16)
                            .lineSpacing(2.4)
                    } else {
                        Text(threads.content)
                            .font(.callout)
                            .opacity(0)
                            .multilineTextAlignment(.leading)
                            .lineLimit(16)
                    }
                    if (threads.img != "") {
                        AsyncImage(
                            url: URL(string:sdk.imgToUrl(img: threads.img, ext: threads.ext, isThumb: true))
                        ) { image in
                            image
                                .resizable()
                                .scaledToFit()
                        } placeholder: {
                            Color.gray.opacity(0.17)
                        }
                        .scaledToFit()
                        .frame(maxHeight: 100)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }.onAppear {
                    DispatchQueue.main.async {
                        htmlText = threads.content.htmlAttributedString()
                    }
                }
                
                HStack {
                    Label("", systemImage: "clock")
                        .labelStyle(.iconOnly)
                    Label(sdk.formatTime(originalTime: threads.time, inThread: false), systemImage: "")
                        .labelStyle(.titleOnly)
                    Spacer()
                    Label(String(Int(truncating: threads.replyCount ?? 0)) + "条回复", systemImage: "")
                        .labelStyle(.titleOnly)
                    Label("", systemImage: "bubble.right")
                        .labelStyle(.iconOnly)
                }
                .padding(.top, 1.0)
                .font(.caption)
                .foregroundColor(Color(UIColor.label))
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 18)
            .frame(maxWidth: .infinity)
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }
}

struct Notice: View {
    let notice: shared.Notice
    let dismiss: () -> Void
    let sdk: XdSDK
    
    
    @Environment(\.colorScheme) var colorScheme
    @State private var htmlText: NSAttributedString?
    
    var body: some View{
        
        VStack(alignment: .leading) {
            Text("公告")
                .font(.title)
                .padding(.bottom, 0.2)
                .padding(.top, 0.1)
            if let htmlText {
                Text(AttributedString(htmlText))
                    .multilineTextAlignment(.leading)
                    .lineSpacing(2.4)
                    .padding(.bottom, 1)
            } else {
                Text(notice.content)
                    .font(.callout)
                    .opacity(0)
                    .multilineTextAlignment(.leading)
                    .padding(.bottom, 1)
            }
            HStack{
                Spacer()
                Text(sdk.formatTime(originalTime: notice.date))
                    .font(.callout)
                    .foregroundColor(Color(UIColor.secondaryLabel))
            }
            Divider()
            HStack(alignment:.center){
                Button(
                    action: dismiss, label:{
                        Text("不再显示")
                    }
                )
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 18)
        .frame(maxWidth: .infinity)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(16)
        .shadow(color:Color(UIColor.label).opacity(0.07), radius: 16, y: 8)
        .onAppear{
            DispatchQueue.main.async {
                htmlText = notice.content.htmlAttributedString()
            }
        }
    }
}
