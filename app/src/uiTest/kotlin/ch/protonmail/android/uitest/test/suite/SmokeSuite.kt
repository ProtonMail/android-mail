package ch.protonmail.android.uitest.test.suite

import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.test.login.LoginTests
import ch.protonmail.android.uitest.test.settings.SettingsTest
import org.junit.experimental.categories.Categories
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Categories::class)
@Categories.IncludeCategory(SmokeTest::class)
@Suite.SuiteClasses(
    LoginTests::class,
    SettingsTest::class
)
class SmokeSuite
