import SwiftUI
import Shared
#if canImport(UIKit)
import UIKit
#endif
#if canImport(AppKit)
import AppKit
#endif

struct SettingsScreen: View {
    @Environment(AppContainer.self) private var container

    var body: some View {
        NavigationStack {
            List {
                Section {
                    SettingsRow(icon: .system("clock.fill"), color: .purple, label: "历史记录")
                        .rowInsets()
                    SettingsRow(icon: .system("bubble.right.fill"), color: .blue, label: "发言记录")
                        .rowInsets()
                }

                Section {
                    NavigationLink(destination: ReadingSettingsScreen()) {
                        SettingsRow(icon: .system("text.square.filled"), color: .cyan, label: "阅读设置")
                    }
                    .rowInsets()
                }

                Section {
                    SettingsRow(icon: .custom("cookie"), color: .orange, label: "饼干管理")
                        .rowInsets()
                    SettingsRow(icon: .system("nosign"), color: .red, label: "屏蔽管理")
                        .rowInsets()
                }
                Section {
                    SettingsRow(icon: .custom("frog.island"), color: .indigo, label: "关于雾岛")
                        .rowInsets()
                    SettingsRow(
                        icon: .custom("github"),
                        color: .platformLabel,
                        iconColor: .platformBackground,
                        label: "查看源码"
                    )
                    .rowInsets()
                }
            }
#if os(iOS)
            .listStyle(.insetGrouped)
#else
            .listStyle(.inset)
#endif
            .navigationTitle("设置")
        }
    }
}

// MARK: - Reading Settings (sub-page)

struct ReadingSettingsScreen: View {
    @Environment(AppContainer.self) private var container

    var body: some View {
        List {
            Section("时间显示") {
                TimeToggleRow(
                    label: "使用 UTC+8 时间",
                    description: "关闭后使用设备本地时区。",
                    isOn: container.timeSettings.useUtcPlus8Time
                ) { v in Task { try? await container.forumBridge.updateUseUtcPlus8Time(value: v) } }

                TimeToggleRow(
                    label: "使用精确时间",
                    description: "关闭后显示相对时间。",
                    isOn: container.timeSettings.usePreciseTime
                ) { v in Task { try? await container.forumBridge.updateUsePreciseTime(value: v) } }

                if container.timeSettings.usePreciseTime {
                    TimeToggleRow(
                        label: "显示秒",
                        description: "仅在精确时间下生效。",
                        isOn: container.timeSettings.showSeconds
                    ) { v in Task { try? await container.forumBridge.updateShowSeconds(value: v) } }
                    .transition(.move(edge: .top).combined(with: .opacity))
                }
            }
        }
#if os(iOS)
        .listStyle(.insetGrouped)
#else
        .listStyle(.inset)
#endif
        .animation(.easeInOut, value: container.timeSettings.usePreciseTime)
        .navigationTitle("阅读设置")
#if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
#endif
    }
}

private struct TimeToggleRow: View {
    let label: String
    let description: String
    let isOn: Bool
    let onChange: (Bool) -> Void

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: onChange)) {
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

// MARK: - Row insets helper

private extension View {
    func rowInsets() -> some View {
        listRowInsets(EdgeInsets(top: 7, leading: 16, bottom: 7, trailing: 16))
    }
}

// MARK: - Settings Row

private enum SettingsIconKind {
    case system(String)
    case custom(String)
}

private struct SettingsRow: View {
    let icon: SettingsIconKind
    let color: Color
    var iconColor: Color = .white
    let label: String

    var body: some View {
        HStack {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(color)
                    .frame(width: 30, height: 30)
                iconImage
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(iconColor)
            }
            .padding(.trailing, 8)
            Text(label)
        }
    }

    @ViewBuilder
    private var iconImage: some View {
        switch icon {
        case .system(let name):
            Image(systemName: name)
        case .custom(let name):
            Image(name)
        }
    }
}

private extension Color {
    static var platformLabel: Color {
    #if canImport(UIKit)
        return Color(UIColor.label)
    #elseif canImport(AppKit)
        return Color(NSColor.labelColor)
    #else
        return .primary
    #endif
    }
    static var platformBackground: Color {
    #if canImport(UIKit)
        return Color(UIColor.systemBackground)
    #elseif canImport(AppKit)
        return Color(NSColor.windowBackgroundColor)
    #else
        return .white
    #endif
    }
}
