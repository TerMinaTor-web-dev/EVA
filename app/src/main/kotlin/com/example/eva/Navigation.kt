package com.example.eva

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.eva.data.AppServiceCatalog
import com.example.eva.data.UserSessionManager
import com.example.eva.ui.components.EvaAiSheet
import com.example.eva.ui.screens.AppContainerScreen
import com.example.eva.ui.screens.HomeScreen
import com.example.eva.ui.screens.OnboardingScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val initialRoute = remember {
        if (UserSessionManager.isLoggedIn(context)) HomeRoute else OnboardingRoute
    }
    val backStack = rememberNavBackStack(initialRoute)
    var showHomeAiCopilot by remember { mutableStateOf(false) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<OnboardingRoute> {
                    OnboardingScreen(
                        onCompleteOnboarding = {
                            backStack.removeLastOrNull()
                            backStack.add(HomeRoute)
                        }
                    )
                }
                entry<HomeRoute> {
                    HomeScreen(
                        onSelectService = { service ->
                            backStack.add(AppRoute(service.id))
                        },
                        onOpenAiCopilot = {
                            showHomeAiCopilot = true
                        }
                    )
                }
                entry<AppRoute> { route ->
                    val service = AppServiceCatalog.getById(route.serviceId)
                    AppContainerScreen(
                        initialService = service,
                        onNavigateHome = {
                            while (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        }
                    )
                }
            },
    )

    if (showHomeAiCopilot) {
        EvaAiSheet(
            currentPageTitle = "EVA Home Hub",
            currentUrl = "eva://hub",
            onDismiss = { showHomeAiCopilot = false }
        )
    }
}
