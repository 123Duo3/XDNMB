//
//  CookieRow.swift
//  雾岛
//
//  Created by 123哆3 on 2022/11/15.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI

struct CookieRow: View {
    @State private var fruits = [
        "MP9wcr6",
        "Banana",
        "Papaya",
        "Mango"
    ]
    
    struct FileDetails: Identifiable {
        var id: String { name }
        let name: String
        let fileType: String
    }
    
    @State private var isConfirming = false
    
    var body: some View {
        NavigationView {
            List {
                ForEach(fruits, id: \.self) { fruit in
                    Text(fruit)
                }
                .onDelete { fruits.remove(atOffsets: $0) }
                .onMove { fruits.move(fromOffsets: $0, toOffset: $1) }
                Button("导入饼干") {
                    isConfirming = true
                }
                .confirmationDialog(
                    "Are you sure you want to import this file?",
                    isPresented: $isConfirming
                ) {
                    Button {
                        // Handle import action.
                    } label: {
                        Text("扫描二维码")
                    }
                    Button {
                        // Handle import action.
                    } label: {
                        Text("从相册导入")
                    }
                    Button("取消", role: .cancel) { }
                }
            }
            .navigationTitle("饼干管理")
            .toolbar {
                EditButton()
            }
        }
    }
}

struct CookieRow_Previews: PreviewProvider {
    static var previews: some View {
        CookieRow()
    }
}
