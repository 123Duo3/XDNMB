//
//  InThreadRow.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/5.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared

struct InThreadRow: View {
    let sdk: XdSDK
    var replyList: shared.Thread
    let forumId: String
    let viewModel: InThreadView.ViewModel
    @State private var htmlText: NSAttributedString?
    
    var body: some View {
        List(){
            Section(header: 
                        VStack(alignment: .leading) {
                HStack{
                    if (forumId == "-1"){
                        Text(sdk.getForumName(forumId: replyList.fid as! Int32))
                        Text("•")
                            .foregroundColor(Color.gray)
                    }
                    Text(replyList.userHash)
                        .foregroundColor(isAdmin(admin: replyList.admin))
                        .font(.system(.footnote, design: .monospaced))
                    Label(String(Int(truncating: replyList.replyCount ?? 0)), systemImage: "bubble.right")
                        .padding(.leading, 8)
                    Label(sdk.formatTime(originalTime: replyList.time, inThread: true), systemImage: "clock")
                        .padding(.leading, 8)
                }
                    .padding(.top, -16)
                    .padding(.bottom, 8)
                    .font(.caption)
                    .textCase(nil)
                
                VStack(alignment: .leading){
                    if(replyList.title != "无标题"){
                        Text(replyList.title)
                            .font(.headline)
                            .foregroundColor(Color(UIColor.label))
                    }
                    if(replyList.name != "无名氏"){
                        Text(replyList.name)
                            .font(.subheadline)
                    }
                } .padding(.bottom, 1.0)
                
                if let htmlText {
                    Text(AttributedString(htmlText))
                        .textCase(nil)
                        .lineSpacing(2.4)
                } else {
                    Text(replyList.content)
                        .font(.callout)
                }
                
                if (replyList.img != "") {
                    AsyncImage(
                        url: URL(string:sdk.imgToUrl(img: replyList.img, ext: replyList.ext, isThumb: true))
                    ) { image in
                        image
                            .resizable()
                            .scaledToFit()
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    } placeholder: {
                        Color.gray.opacity(0.17)
                    }
                    .scaledToFit()
                    .frame(height: 100)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }
                .padding(.bottom, 16)
                .onAppear{
                    DispatchQueue.main.async {
                        htmlText = replyList.content.htmlAttributedString()
                    }
                },
                    footer:
                HStack(alignment: .center){
                Spacer()
                nextPageIndicator()
                    .font(.body)
                    .textCase(nil)
                    .padding(.vertical, 10)
                Spacer()
            }
            ){
                ForEach(replyList.replies!){reply in
                    Reply(poster: replyList.userHash, reply: reply, sdk: sdk)
                        .onAppear(perform: {
                            print("No.", reply.id, "in page", reply.page)
                            viewModel.updateCurrentPage(page: Int(reply.page))
                            if (Int(truncating: replyList.replyCount ?? 0) > 19) {
                                if (reply.id == replyList.replies![replyList.replies!.count - 2].id) {
                                    viewModel.loadNextPage(threadId: Int(replyList.id))
                                }
                            } else {
                                viewModel.nextPage = .success
                            }
                        })
                }
            }
        }
            .listStyle(.grouped)
    }
    
    private func nextPageIndicator() -> AnyView{
        switch viewModel.nextPage{
        case.success: return AnyView(
            Text("到底了")
        )
        case .loading:return AnyView(
            Text("加载中...")
        )
        case .error(let discription):return AnyView(
            Text(discription)
        )}
    }
}

struct Reply: View {
    let poster: String
    let reply: shared.Thread
    let sdk: XdSDK
    @State private var htmlText: NSAttributedString?
    
    var body: some View {
        LazyVStack(alignment:.leading){
                HStack{
                    if(reply.id != 9999999){
                        if(reply.userHash == poster){
                            Text("PO")
                                .foregroundColor(Color.accentColor)
                                .fontWeight(.semibold)
                            Text("•")
                                .foregroundColor(Color.gray)
                            Text("No." + String(reply.id))
                                .font(.system(.footnote, design: .monospaced))
                            Text("•")
                                .foregroundColor(Color.gray)
                            Text(reply.userHash)
                                .foregroundColor(isAdmin(admin: reply.admin))
                                .font(.system(.footnote, design: .monospaced))
                        } else {
                            Text("No." + String(reply.id))
                                .font(.system(.footnote, design: .monospaced))
                            Text("•")
                                .foregroundColor(Color.gray)
                            Text(reply.userHash)
                                .foregroundColor(isAdmin(admin: reply.admin))
                                .font(.system(.footnote, design: .monospaced))
                        }
                        if(reply.sage == 1){
                            Text("•")
                                .foregroundColor(Color.gray)
                            Text("SAGE")
                                .fontWeight(.semibold)
                                .foregroundColor(Color.red)
                        }
                        Spacer()
                        
                        Label(sdk.formatTime(originalTime: reply.time, inThread: true),systemImage: "")
                            .labelStyle(.titleOnly)
                        /* Label("", systemImage: "clock")
                            .labelStyle(.iconOnly)
                            .padding(.bottom, 1.0) */
                    }
                }.font(.caption)
            
            
            VStack(alignment: .leading){
                if(reply.title != "无标题"){
                    Text(reply.title)
                        .font(.headline)
                }
                if(reply.name != "无名氏"){
                    Text(reply.name)
                        .font(.subheadline)
                        .foregroundColor(Color.gray)
                }
            } .padding(.bottom, 1.0)
            
            LazyVStack(alignment: .leading){
                if let htmlText {
                    Text(AttributedString(htmlText))
                        .lineSpacing(2.4)
                } else {
                    Text(reply.content)
                        .font(.callout)
                        .opacity(0)
                }
                
                if (reply.img != "") {
                    AsyncImage(
                        url: URL(string:sdk.imgToUrl(img: reply.img, ext: reply.ext, isThumb: true))
                    ) { image in
                        image
                            .resizable()
                            .scaledToFit()
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    } placeholder: {
                        Color.gray.opacity(0.17)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .scaledToFit()
                    .frame(maxHeight: 100, alignment: .leading)
                }
                
            }.onAppear{
                DispatchQueue.main.async {
                    htmlText = reply.content.htmlAttributedString()
                }
            }
        }
    }
}
