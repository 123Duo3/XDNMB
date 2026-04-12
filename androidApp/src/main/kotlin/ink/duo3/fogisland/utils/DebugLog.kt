package ink.duo3.fogisland.utils

import android.util.Log
import ink.duo3.fogisland.BuildConfig

internal object DebugLog {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message())
        }
    }
}
