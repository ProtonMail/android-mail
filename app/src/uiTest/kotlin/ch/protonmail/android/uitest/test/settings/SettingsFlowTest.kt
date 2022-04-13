package ch.protonmail.android.uitest.test.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFlowTest : BaseTest() {

    private val user = quark.userCreate()
    private val menuRobot = MenuRobot(composeTestRule)

    @Before
    fun setUp() {
        login(user)
    }

    @Test
    @Category(SmokeTest::class)
    fun openAccountSettings() {

        menuRobot
            .settings()
            .openUserAccountSettings()
            .verify { accountSettingsOpened(composeTestRule) }
    }

    @Test
    @Category(SmokeTest::class)
    fun openConversationModeSetting() {
        menuRobot
            .settings()
            .openUserAccountSettings()
            .conversationMode()
            .verify { conversationModeToggleShown(composeTestRule) }
    }

    @Test
    @Category(SmokeTest::class)
    fun openAndChangeThemeSetting() {
        val themeSettingsRobot = menuRobot
            .settings()
            .selectThemeSettings()

        themeSettingsRobot
            .selectSystemDefault()
            .verify { defaultThemeSettingShown(composeTestRule) }
        themeSettingsRobot
            .selectDarkTheme()
            .verify { darkThemeSelected(composeTestRule) }
    }
}
