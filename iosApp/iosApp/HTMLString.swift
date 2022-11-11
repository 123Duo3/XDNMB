//
//  HTMLString.swift
//  iosApp
//
//  Created by 123哆3 on 2022/11/11.
//  Copyright © 2022 orgName. All rights reserved.
//

import SwiftUI

extension String {
    func htmlAttributedString(
        fontSize: CGFloat = 16,
        color: UIColor = UIColor.label,
        linkColor: UIColor = UIColor.systemBlue,
        fontFamily: String = "-apple-system"
    ) -> NSAttributedString? {
        let htmlTemplate = """
        <!doctype html>
        <html>
          <head>
            <style>
                body {
                    color: \(color.hexString!);
                    font-family: \(fontFamily);
                    font-size: \(fontSize)px;
                }
                a {
                    color: \(linkColor.hexString!);
                }
            </style>
          </head>
          <body>
            \(self)
          </body>
        </html>
        """

        guard let data = htmlTemplate.data(using: .unicode) else {
            return nil
        }

        guard let attributedString = try? NSAttributedString(
            data: data,
            options: [.documentType: NSAttributedString.DocumentType.html],
            documentAttributes: nil
            ) else {
            return nil
        }

        return attributedString
    }
}

extension UIColor {
    var hexString:String? {
        if let components = self.cgColor.components {
            let r = components[0]
            let g = components[1]
            let b = components[2]
            return  String(format: "#%02x%02x%02x", (Int)(r * 255), (Int)(g * 255), (Int)(b * 255))
        }
        return nil
    }
}
