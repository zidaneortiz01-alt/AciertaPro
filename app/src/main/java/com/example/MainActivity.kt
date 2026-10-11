package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AttemptResultsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticScreen
import com.example.ui.screens.FeedbackScreen
import com.example.ui.screens.InitialConfigScreen
import com.example.ui.screens.ModulesListScreen
import com.example.ui.screens.PracticeQuestionScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SimulationConfigScreen
import com.example.ui.screens.SimulationScreen
import com.example.ui.screens.StudyPlanScreen
import com.example.ui.screens.TopicsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.WrittenCommunicationScreen
import com.example.ui.theme.SaberProTheme
import com.example.ui.viewmodel.SaberProViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SaberProViewModel = viewModel(
                factory = SaberProViewModel.provideFactory(application)
            )
            val uiState by viewModel.uiState.collectAsState()

            SaberProTheme(darkTheme = uiState.isDarkMode) {
                // Adaptive layout: mobile-first (360-430dp), responsive on wider screens (up to 600dp centered)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp)
                    ) {
                        AppNavigation(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: SaberProViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // BackHandler para navegación natural en Android
    if (uiState.currentScreen != Screen.WELCOME && uiState.currentScreen != Screen.DASHBOARD) {
        BackHandler {
            when (uiState.currentScreen) {
                Screen.AUTH -> viewModel.navigateTo(Screen.WELCOME)
                Screen.INITIAL_CONFIG -> viewModel.navigateTo(Screen.AUTH)
                Screen.TOPICS_LIST -> viewModel.navigateTo(Screen.MODULES_LIST)
                Screen.FEEDBACK -> viewModel.navigateTo(Screen.PRACTICE_QUESTION)
                Screen.ATTEMPT_RESULTS -> viewModel.navigateTo(Screen.DASHBOARD)
                Screen.PRACTICE_QUESTION,
                Screen.SIMULATION_IN_PROGRESS -> {
                    // Manually confirm or navigate to avoid accidental data loss
                    viewModel.navigateTo(Screen.DASHBOARD)
                }
                else -> viewModel.navigateTo(Screen.DASHBOARD)
            }
        }
    }

    when (uiState.currentScreen) {
        Screen.WELCOME -> WelcomeScreen(viewModel)
        Screen.AUTH -> AuthScreen(viewModel)
        Screen.INITIAL_CONFIG -> InitialConfigScreen(viewModel)
        Screen.DASHBOARD -> DashboardScreen(viewModel)
        Screen.DIAGNOSTIC -> DiagnosticScreen(viewModel)
        Screen.MODULES_LIST -> ModulesListScreen(viewModel)
        Screen.TOPICS_LIST -> TopicsScreen(viewModel)
        Screen.PRACTICE_QUESTION -> PracticeQuestionScreen(viewModel)
        Screen.FEEDBACK -> FeedbackScreen(viewModel)
        Screen.SIMULATION_CONFIG -> SimulationConfigScreen(viewModel)
        Screen.SIMULATION_IN_PROGRESS -> SimulationScreen(viewModel)
        Screen.ATTEMPT_RESULTS -> AttemptResultsScreen(viewModel)
        Screen.PROGRESS_HISTORY -> ProgressScreen(viewModel)
        Screen.STUDY_PLAN -> StudyPlanScreen(viewModel)
        Screen.PROFILE_SETTINGS -> ProfileScreen(viewModel)
        Screen.WRITTEN_COMMUNICATION -> WrittenCommunicationScreen(viewModel)
    }
}
