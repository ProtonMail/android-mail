package ch.protonmail.android.initializer

import android.content.Context
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import io.sentry.Sentry

class SentryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        Sentry.init(
            BuildConfig.SENTRY_DSN
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
