//
//  PostsListRow.swift
//  iosApp
//
//  Created by 123哆3 on 2022/10/29.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI
import shared
import RichText

struct ThreadsListRow: View {
    var threadList: [shared.Thread]
    let sdk: XdSDK
    @State var size:CGSize = .zero
    
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        NavigationView{
            List{
                ForEach(threadList) {threads in
                    Section{
                        VStack(alignment: .leading) {
                            HStack{
                                if(true){
                                    Text(sdk.getForumName(forumId: threads.fid))
                                    Text("•")
                                        .foregroundColor(Color.gray)
                                }
                                Text("No." + String(threads.id))
                                    .foregroundColor(Color.gray)
                                if(threads.sage == 1){
                                    Text("•")
                                        .foregroundColor(Color.gray)
                                    Text("SAGE")
                                        .fontWeight(.semibold)
                                        .foregroundColor(Color.red)
                                }
                                Spacer()
                                Label(sdk.formatTime(originalTime: threads.time, inThread: false), systemImage: "")
                                    .labelStyle(.titleOnly)
                                Label("", systemImage: "clock")
                                    .labelStyle(.iconOnly)
                            }.padding(.bottom, 1.0).font(.caption)
                            
                            VStack(alignment: .leading){
                                if(threads.title != "无标题"){
                                    Text(threads.title)
                                        .font(.headline)
                                }
                                if(threads.name != "无名氏" && threads.name != "无标题" ){
                                    Text(threads.name)
                                        .font(.subheadline)
                                        .foregroundColor(Color.gray)
                                }
                            } .padding(.bottom, 1.0)
            
                            VStack(alignment: .leading){
                                RichText(html: threads.content)
                                    .lineHeight(140)
                                    .padding(-8)
                                //Text(threads.content).font(.callout)
                                
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
                            }
                            
                            HStack{
                                Text(threads.userHash)
                                    .foregroundColor(isAdmin(admin: threads.admin))
                                Spacer()
                                Label(String(Int(truncating: threads.replyCount ?? 0)) + "条回复", systemImage: "")
                                    .labelStyle(.titleOnly)
                                Label("", systemImage: "bubble.right")
                                    .labelStyle(.iconOnly)
                                
                            }.padding(.top, 1.0).font(.caption)
                        }
                    }
                }
            }
            .navigationTitle(Text("时间线"))
            .listRowInsets(EdgeInsets())
        }
    }
}

func isAdmin(admin: Int32)-> Color{
    if (admin == 0){
        return Color.gray
    } else {
        return Color.red
    }
}
