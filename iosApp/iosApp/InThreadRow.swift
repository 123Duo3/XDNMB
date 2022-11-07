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
    var replyList: [shared.Thread]
    
    var body: some View {
        List(){
            ForEach(replyList){reply in
                Section{
                    VStack(alignment:.leading){
                        HStack{
                            if(reply.master == 1){
                                Text("PO")
                                    .fontWeight(.semibold)
                                    .foregroundColor(Color.accentColor)
                                Text("•")
                                    .foregroundColor(Color.gray)
                                Text(reply.userHash)
                                    .fontWeight(.semibold)
                            } else {
                                Text(reply.userHash)
                            }
                            Text("•")
                                .foregroundColor(Color.gray)
                            Text("No." + String(reply.id))
                                .foregroundColor(Color.gray)
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
                        }.font(.caption).padding(.bottom, 1.0)
                        
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
