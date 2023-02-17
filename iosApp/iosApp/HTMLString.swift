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
    var hexString: String? {
        if let components = self.cgColor.components {
            let r, g, b: CGFloat
            switch components.count {
            case 2:
                r = components[0]
                g = components[0]
                b = components[0]
            case 3:
                r = components[0]
                g = components[1]
                b = components[2]
            case 4:
                r = components[0]
                g = components[1]
                b = components[2]
            default:
                return nil
            }
            return String(format: "#%02X%02X%02X", Int(r * 255), Int(g * 255), Int(b * 255))
        }
        return nil
    }
}
