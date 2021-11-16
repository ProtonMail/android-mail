package ch.protonmail.android.initializer

import android.content.Context
import android.os.StrictMode
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig

class StrictModeInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun enableStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyFlashScreen()
            .penaltyLog()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}
