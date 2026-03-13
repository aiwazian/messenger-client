package com.aiwazian.messenger.ui.settings.security

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.ui.element.AnimatedIntroScreen
import com.aiwazian.messenger.ui.element.CodeBlocks
import com.aiwazian.messenger.ui.element.CustomDialog
import com.aiwazian.messenger.ui.element.CustomNumberBoard
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.utils.LottieAnimation
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.aiwazian.messenger.viewModels.PasscodeLockViewModel
import kotlinx.coroutines.launch

private object PasscodeScreens {
    const val MAIN = "PasscodeMain"
    const val SETTINGS = "PasscodeSettings"
    const val CREATE = "CreatePasscode"
    const val CHANGE = "ChangePasscode"
}

@Composable
fun SettingsPasscodeScreen(enablePasscode: Boolean = false) {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val navController = rememberNavController()
    
    val transition = tween<IntOffset>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )
    
    val startDestination = if (enablePasscode) {
        PasscodeScreens.SETTINGS
    } else {
        PasscodeScreens.MAIN
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(animationSpec = transition) { it }
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = transition) { -it }
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = transition) { -it }
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = transition) { it }
        }) {
        composable(route = PasscodeScreens.MAIN) {
            PasscodeLockMainScreen(
                navController,
                navViewModel
            )
        }
        composable(route = PasscodeScreens.CREATE) {
            CreatePasscodeScreen(
                navController,
                navViewModel
            )
        }
        composable(route = PasscodeScreens.SETTINGS) {
            SettingsPasscodeLockScreen(
                navController,
                navViewModel
            )
        }
        composable(route = PasscodeScreens.CHANGE) {
            SettingsChangePasscodeLockScreen(navController)
        }
    }
}

@Composable
private fun CreatePasscodeScreen(
    navController: NavHostController,
    navViewModel: NavigationViewModel
) {
    val passcodeLockViewModel = hiltViewModel<PasscodeLockViewModel>()
    
    passcodeLockViewModel.onSaveNewPasscode = {
        navController.navigate(route = PasscodeScreens.SETTINGS) {
            popUpTo(PasscodeScreens.MAIN) {
                inclusive = true
            }
            popUpTo(PasscodeScreens.CREATE) {
                inclusive = true
            }
        }
    }
    
    val passcode by passcodeLockViewModel.passcode.collectAsState()
    
    Scaffold(
        topBar = {
            TopBar(navViewModel)
        },
        modifier = Modifier.fillMaxSize(),
        
        ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    
                    Text(
                        text = "Создание код-пароля",
                        fontSize = 24.sp
                    )
                    
                    Text(
                        text = "Введите 4 цифры, которые хотите использовать для разблокировки приложения.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        lineHeight = 16.sp
                    )
                }
                
                CodeBlocks(
                    count = PasscodeLockViewModel.MAX_LENGTH_PASSCODE,
                    showInput = false,
                    code = passcode
                )
            }
            
            val boardButtons = listOf(
                listOf(
                    "1",
                    "2",
                    "3"
                ),
                listOf(
                    "4",
                    "5",
                    "6"
                ),
                listOf(
                    "7",
                    "8",
                    "9"
                ),
                listOf(
                    null,
                    "0",
                    Icons.AutoMirrored.Outlined.Backspace
                ),
            )
            
            CustomNumberBoard(
                value = passcode,
                buttons = boardButtons,
                onChange = passcodeLockViewModel::onPasscodeChanged
            )
        }
    }
}

@Composable
private fun SettingsChangePasscodeLockScreen(navController: NavHostController) {
    val passcodeLockViewModel = hiltViewModel<PasscodeLockViewModel>()
    
    passcodeLockViewModel.onSaveNewPasscode = {
        navController.navigate(route = PasscodeScreens.SETTINGS) {
            popUpTo(PasscodeScreens.MAIN) {
                inclusive = true
            }
            popUpTo(PasscodeScreens.CREATE) {
                inclusive = true
            }
        }
    }
    
    val passcode by passcodeLockViewModel.passcode.collectAsState()
    
    Scaffold(
        topBar = {
            PageTopBar(
                navigationIcon = NavigationIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onClick = navController::popBackStack
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    
                    Text(
                        text = "Введите новый код-пароль",
                        fontSize = 24.sp
                    )
                    
                    Text(
                        text = "Введите 4 цифры, которые хотите использовать для разблокировки приложения.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        lineHeight = 16.sp
                    )
                }
                
                CodeBlocks(
                    count = PasscodeLockViewModel.MAX_LENGTH_PASSCODE,
                    code = passcode
                )
            }
            
            val boardButtons = listOf(
                listOf(
                    "1",
                    "2",
                    "3"
                ),
                listOf(
                    "4",
                    "5",
                    "6"
                ),
                listOf(
                    "7",
                    "8",
                    "9"
                ),
                listOf(
                    null,
                    "0",
                    Icons.AutoMirrored.Outlined.Backspace
                ),
            )
            
            CustomNumberBoard(
                value = passcode,
                buttons = boardButtons,
                onChange = passcodeLockViewModel::onPasscodeChanged
            )
        }
    }
}

@Composable
private fun PasscodeLockMainScreen(
    navController: NavHostController,
    navViewModel: NavigationViewModel
) {
    Scaffold(
        topBar = {
            TopBarMain(navViewModel)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            AnimatedIntroScreen(
                animation = LottieAnimation.KEY_LOCK,
                title = stringResource(R.string.passcode_lock),
                description = stringResource(R.string.passcode_lock_description),
                buttonText = stringResource(R.string.enable_passcode),
                buttonClick = {
                    navController.navigate(route = PasscodeScreens.CREATE) {
                        popUpTo(PasscodeScreens.MAIN) {
                            inclusive = true
                        }
                    }
                })
        }
    }
}

@Composable
private fun TopBar(navViewModel: NavigationViewModel) {
    PageTopBar(
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}

@Composable
private fun SettingsPasscodeLockScreen(
    navController: NavHostController,
    navViewModel: NavigationViewModel
) {
    val passcodeLockViewModel = hiltViewModel<PasscodeLockViewModel>()
    
    val disablePasscodeDialog = passcodeLockViewModel.disablePasscodeDialog
    
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopBar(navViewModel)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val composition by rememberLottieComposition(
                    spec = LottieCompositionSpec.Asset(LottieAnimation.KEY_LOCK)
                )
                
                LottieAnimation(
                    composition = composition,
                    modifier = Modifier.size(100.dp),
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )
                
                Text(
                    text = "Для блокировки и разблокировки приложения нажмите на значок замка над списком чатов.",
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
            }
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.change_passcode),
                    onClick = {
                        navController.navigate(PasscodeScreens.CREATE)
                    })
            }
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.turn_passcode_off),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = disablePasscodeDialog::show
                )
            }
            
            val scope = rememberCoroutineScope()
            
            if (disablePasscodeDialog.isVisible) {
                DisablePasscodeDialog(
                    onDismiss = disablePasscodeDialog::hide,
                    onConfirm = {
                        scope.launch {
                            passcodeLockViewModel.disablePasscode()
                            disablePasscodeDialog.hide()
                            navViewModel.removeLastScreenInStack()
                        }
                    })
            }
        }
    }
}

@Composable
private fun DisablePasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.turn_passcode_off),
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = "Вы точно хотите отключить пароль?",
            )
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.turn_off))
            }
        })
}

@Composable
private fun TopBarMain(navViewModel: NavigationViewModel) {
    PageTopBar(
        title = { Text(stringResource(R.string.passcode_lock)) },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}