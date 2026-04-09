package ch.protonmail.android.e2e.runner

import android.os.Bundle
import io.cucumber.android.runner.CucumberAndroidJUnitRunner

class CucumberTestRunner : CucumberAndroidJUnitRunner() {
    override fun onCreate(bundle: Bundle) {
        bundle.putString("plugin", "pretty")
        bundle.putString("features", "features")
        bundle.putString("glue", "ch.protonmail.android.e2e.steps")
        bundle.putString("cucumberOptions", "ch.protonmail.android.e2e.runner")
        super.onCreate(bundle)
    }
}
