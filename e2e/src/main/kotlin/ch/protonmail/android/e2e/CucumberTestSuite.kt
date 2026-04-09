package ch.protonmail.android.e2e

import io.cucumber.junit.CucumberOptions

@CucumberOptions(
    features = ["features"],
    glue = ["ch.protonmail.android.e2e.steps"]
)
class CucumberTestSuite
