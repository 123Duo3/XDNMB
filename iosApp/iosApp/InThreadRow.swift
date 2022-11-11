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
                }
            ){
                ForEach(replyList.replies!){reply in
                    Reply(poster: replyList.userHash, reply: reply, sdk: sdk)
                }
            }
        }.listStyle(.grouped)
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
                        Text("No." + String(reply.id))
                        Text("•")
                            .foregroundColor(Color.gray)
                        if(reply.userHash == poster){
                            Text(reply.userHash)
                                .fontWeight(.semibold)
                                .foregroundColor(isAdmin(admin: reply.admin))
                            Text("•")
                                .foregroundColor(Color.gray)
                            Text("PO")
                                .fontWeight(.semibold)
                                .foregroundColor(Color.accentColor)
                        } else {
                            Text(reply.userHash)
                                .foregroundColor(isAdmin(admin: reply.admin))
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
                        Label("", systemImage: "clock")
                            .labelStyle(.iconOnly)
                            .padding(.bottom, 1.0)
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
