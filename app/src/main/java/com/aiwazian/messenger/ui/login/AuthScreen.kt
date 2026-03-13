package com.aiwazian.messenger.ui.login

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiwazian.messenger.viewModels.AuthViewModel

object Screen {
    const val LOGIN = "login"
    const val VERIFICATION = "verification"
    const val PASSWORD = "password"
}

@Composable
fun AuthScreen() {
    Content()
}

@Composable
private fun Content() {
    val navController = rememberNavController()
    
    val authViewModel = viewModel<AuthViewModel>()
    
    val transition = tween<IntOffset>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )
    
    NavHost(
        navController = navController,
        startDestination = Screen.LOGIN,
        enterTransition = {
            slideInHorizontally(transition) { it }
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = transition) { -it }
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = transition) { -it }
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = transition) { it }
        }
    ) {
        composable(route = Screen.LOGIN) {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable(route = Screen.VERIFICATION) {
            VerificationCodeScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
        composable(route = Screen.PASSWORD) {
            PasswordScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }
}
