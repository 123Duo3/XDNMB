import Foundation

/// Thrown by `withNetworkTimeout` when the underlying operation doesn't return
/// within the allotted duration. Prevents UI from being stuck on ProgressView
/// when a request hangs (e.g. inaccessible forum, server never closes socket).
struct NetworkTimeoutError: LocalizedError {
    var errorDescription: String? { "请求超时，请检查网络或稍后重试" }
}

/// Races `operation` against a sleeping task; whichever finishes first wins.
/// If the timeout fires, the operation task is cancelled and
/// `NetworkTimeoutError` is thrown.
func withNetworkTimeout<T: Sendable>(
    seconds: Double,
    operation: @Sendable @escaping () async throws -> T
) async throws -> T {
    try await withThrowingTaskGroup(of: T.self) { group in
        group.addTask { try await operation() }
        group.addTask {
            try await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            throw NetworkTimeoutError()
        }
        guard let first = try await group.next() else {
            throw NetworkTimeoutError()
        }
        group.cancelAll()
        return first
    }
}

/// Maps opaque bridged Kotlin/NSError messages to something user-readable.
/// SKIE surfaces Kotlin exceptions as NSError with `localizedDescription`
/// that's often just the class name — fall back to a generic message when
/// we can't make sense of it.
func friendlyMessage(for error: Error) -> String {
    if error is NetworkTimeoutError {
        return (error as! NetworkTimeoutError).errorDescription ?? "请求超时"
    }
    let description = error.localizedDescription
    if description.isEmpty || description.contains("KotlinException") {
        return "加载失败：\(String(describing: type(of: error)))"
    }
    return description
}
