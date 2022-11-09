//
//  InThreadRow.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/5.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared
import RichText

struct InThreadRow: View {
    let sdk: XdSDK
    var replyList: shared.Thread
    let forumId: String
    
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
                    Label(sdk.formatTime(originalTime: replyList.time, inThread: true), systemImage: "clock")
                        .padding(.leading, 8)
                    Label(String(Int(truncating: replyList.replyCount ?? 0)), systemImage: "bubble.right")
                        .padding(.leading, 8)
                }
                    .padding(.top, -16)
                    .font(.caption)
                    .textCase(nil)
                
                RichText(html: replyList.content)
                    .padding(-8)
                
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
                    .frame(maxHeight: 100)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }.padding(.bottom, 16))
            {
                ForEach(replyList.replies!){reply in
                    VStack(alignment:.leading){
                            HStack{
                                if(reply.id != 9999999){
                                    Text("No." + String(reply.id))
                                    Text("•")
                                        .foregroundColor(Color.gray)
                                    if(reply.userHash == replyList.userHash){
                                        Text(reply.userHash)
                                            .fontWeight(.semibold)
                                            .foregroundColor(Color.gray)
                                        Text("•")
                                            .foregroundColor(Color.gray)
                                        Text("PO")
                                            .fontWeight(.semibold)
                                            .foregroundColor(Color.accentColor)
                                    } else {
                                        Text(reply.userHash)
                                            .foregroundColor(Color.gray)
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
                        
                        RichText(html: reply.content)
                            .padding(-8)
                        
                        if (reply.img != "") {
                            AsyncImage(
                                url: URL(string:sdk.imgToUrl(img: reply.img, ext: reply.ext, isThumb: true))
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
                    }
                }
            }
        }.listStyle(.grouped)
    }
}
