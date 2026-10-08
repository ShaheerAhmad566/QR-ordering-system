package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PosViewModel
import com.example.ui.Screen
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppHeader
import com.example.ui.components.AppSidebar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DayCloseScreen
import com.example.ui.screens.ExcelExportScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.LoansScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.StockScreen
import com.example.ui.screens.TenderCheckoutScreen
import com.example.ui.theme.CounterFlowTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CounterFlowTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: PosViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen != Screen.CHECKOUT,
        drawerContent = {
            AppSidebar(
                viewModel = viewModel,
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    viewModel.navigateTo(screen)
                    scope.launch { drawerState.close() }
                },
                onCloseSidebar = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (currentScreen != Screen.DASHBOARD) {
                    AppHeader(
                        currentScreen = currentScreen,
                        onNavigateBack = { viewModel.navigateTo(Screen.DASHBOARD) },
                        onOpenReports = { viewModel.navigateTo(Screen.EXCEL_EXPORT) },
                        onOpenLoans = { viewModel.navigateTo(Screen.LOANS) },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            },
            bottomBar = {
                if (currentScreen != Screen.CHECKOUT) {
                    AppBottomNav(
                        currentScreen = currentScreen,
                        onSelectScreen = { screen -> viewModel.navigateTo(screen) }
                    )
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (slideInHorizontally(tween(280)) { it / 12 } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 12 } + fadeOut(tween(180)))
                },
                label = "screen_transition"
            ) { screen ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (screen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onOpenSidebar = { scope.launch { drawerState.open() } }
                        )
                        Screen.SALE -> PosScreen(
                            viewModel = viewModel,
                            onNavigateToCheckout = { viewModel.navigateTo(Screen.CHECKOUT) }
                        )
                        Screen.CHECKOUT -> TenderCheckoutScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(Screen.SALE) }
                        )
                        Screen.STOCK -> StockScreen(
                            viewModel = viewModel
                        )
                        Screen.EXPENSES -> ExpensesScreen(
                            viewModel = viewModel
                        )
                        Screen.DAY_CLOSE -> DayCloseScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(Screen.DASHBOARD) }
                        )
                        Screen.LOANS -> LoansScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(Screen.DASHBOARD) }
                        )
                        Screen.EXCEL_EXPORT -> ExcelExportScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(Screen.DASHBOARD) }
                        )
                    }
                }
            }
        }
    }
}
