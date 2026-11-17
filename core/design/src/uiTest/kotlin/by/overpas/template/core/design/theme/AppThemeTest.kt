package by.overpas.template.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import by.overpas.template.core.ui.test.ComposeUiTestBase
import com.atiurin.ultron.core.compose.runUltronUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppThemeTest : ComposeUiTestBase() {

    @Test
    fun darkThemeProvidesTheDarkSurfaceColor() =
        runUltronUiTest {
            val surfaces = mutableListOf<Color>()

            setContent {
                AppTheme(darkTheme = true) {
                    surfaces += MaterialTheme.colorScheme.surface
                }
            }

            assertEquals(darkColorScheme().surface, surfaces.last())
        }

    @Test
    fun lightThemeProvidesTheLightSurfaceColor() =
        runUltronUiTest {
            val surfaces = mutableListOf<Color>()

            setContent {
                AppTheme(darkTheme = false) {
                    surfaces += MaterialTheme.colorScheme.surface
                }
            }

            assertEquals(lightColorScheme().surface, surfaces.last())
        }
}
