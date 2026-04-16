import SwiftUI

/// A plain sidebar row: text on the left, chevron on the right, no icon.
struct SidebarRow: View {
    let title: String

    var body: some View {
        HStack {
            Text(title)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundStyle(.tertiary)
        }
    }
}
