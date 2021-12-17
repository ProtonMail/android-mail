package ch.protonmail.android.logging

import android.util.Log
import io.sentry.Sentry
import timber.log.Timber

class SentryTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        Sentry.captureMessage(message)
    }
}
