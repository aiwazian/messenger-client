package com.aiwazian.messenger.ui.settings.chat

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.data.NavigationIcon
import com.aiwazian.messenger.enums.PrimaryColorOption
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.ui.element.PageTopBar
import com.aiwazian.messenger.ui.element.SectionContainer
import com.aiwazian.messenger.ui.element.SectionHeader
import com.aiwazian.messenger.ui.element.SectionItem
import com.aiwazian.messenger.ui.element.SectionToggleItem
import com.aiwazian.messenger.viewModels.NavigationViewModel
import com.aiwazian.messenger.viewModels.SettingsDesignViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsChatScreen() {
    Content()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Content() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    val viewModel = hiltViewModel<SettingsDesignViewModel>()
    
    val primaryColor by viewModel.primaryColor.collectAsState()
    val isDynamicColorEnable by viewModel.dynamicColor.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopBar()
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            val theme = when (viewModel.currentTheme.collectAsState().value) {
                ThemeOption.DARK -> "Включена"
                ThemeOption.LIGHT -> "Отключена"
                else -> "Как в системе"
            }
            
            SectionHeader(title = stringResource(R.string.color_theme))
            
            SectionContainer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SectionToggleItem(
                        text = stringResource(R.string.dynamic_color),
                        isChecked = isDynamicColorEnable,
                        onCheckedChange = {
                            coroutineScope.launch {
                                viewModel.setDynamicColor(!isDynamicColorEnable)
                            }
                        })
                }
                
                AnimatedContent(targetState = isDynamicColorEnable) { enableDynamicColor ->
                    if (!enableDynamicColor) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            PrimaryColorOption.entries.forEach { option ->
                                RadioButton(
                                    enabled = !isDynamicColorEnable,
                                    modifier = Modifier.scale(1.5f),
                                    selected = primaryColor == option,
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.setPrimaryColor(option)
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = option.color,
                                        unselectedColor = option.color,
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            SectionContainer {
                SectionItem(
                    text = stringResource(R.string.dark_theme),
                    primaryText = theme,
                    onClick = {
                        navViewModel.addScreenInStack {
                            SettingsDarkThemeScreen()
                        }
                    })
            }
            
//            var vds by remember { mutableStateOf("") }
//
//            var selected by remember { mutableIntStateOf(0) }
//
//            SectionHeader(title = "Смахивание влево в списке чатов $vds")
//
//            SectionContainer {
//                Row(
//                    modifier = Modifier
//                        .padding(5.dp)
//                        .horizontalScroll(rememberScrollState())
//                ) {
//                    Boxic(
//                        selected = selected == 1,
//                        icon = Icons.Outlined.Archive,
//                        onClick = {
//                            vds = "Архивировать"
//                            selected = 1
//                        })
//
//                    Boxic(
//                        selected = selected == 2,
//                        icon = Icons.Outlined.PushPin,
//                        onClick = {
//                            vds = "Закрепить"
//                            selected = 2
//                        })
//
//                    Boxic(
//                        selected = selected == 5,
//                        icon = Icons.Outlined.ChatBubble,
//                        onClick = {
//                            vds = "Прочитать"
//                            selected = 5
//                        })
//
//                    Boxic(
//                        selected = selected == 6,
//                        icon = Icons.AutoMirrored.Outlined.VolumeOff,
//                        onClick = {
//                            vds = "Выкл. звук"
//                            selected = 6
//                        })
//
//                    Boxic(
//                        selected = selected == 3,
//                        icon = Icons.Outlined.Block,
//                        onClick = {
//                            vds = "Сменить папку"
//                            selected = 3
//                        })
//
//                    Boxic(
//                        selected = selected == 4,
//                        icon = Icons.Outlined.Delete,
//                        onClick = {
//                            vds = "Удалить"
//                            selected = 4
//                        })
//                }
//            }
//
//            SectionDescription(text = "Выбор действия, которое будет выполняться при смахивании влево в списке чатов.")
//
//            SectionHeader("Shape")
//
//            val shapes = listOf(
//                MaterialShapes.Square.toShape(),
//                MaterialShapes.Circle.toShape(),
//                MaterialShapes.Pentagon.toShape(),
//                MaterialShapes.Cookie4Sided.toShape(),
//                MaterialShapes.Cookie6Sided.toShape(),
//                MaterialShapes.Cookie7Sided.toShape(),
//                MaterialShapes.Cookie9Sided.toShape(),
//                MaterialShapes.Cookie12Sided.toShape(),
//                MaterialShapes.Clover4Leaf.toShape(),
//                MaterialShapes.Clover8Leaf.toShape(),
//                MaterialShapes.Flower.toShape(),
//                MaterialShapes.SoftBurst.toShape(),
//                MaterialShapes.Sunny.toShape(),
//                MaterialShapes.VerySunny.toShape()
//            )
//
//            var selectedShapeIndex by remember { mutableIntStateOf(0) }
//
//            SectionContainer {
//                Row(
//                    modifier = Modifier
//                        .horizontalScroll(rememberScrollState())
//                        .padding(10.dp),
//                    horizontalArrangement = Arrangement.spacedBy(10.dp)
//                ) {
//                    shapes.forEachIndexed { index, shape ->
//                        FilledIconButton(
//                            onClick = { selectedShapeIndex = index },
//                            modifier = Modifier.size(50.dp),
//                            shape = shape
//                        ) {
//                            AnimatedVisibility(
//                                visible = selectedShapeIndex == index,
//                                enter = scaleIn(tween(200)),
//                                exit = scaleOut(tween(200))
//                            ) {
//                                Icon(
//                                    Icons.Outlined.Check,
//                                    null
//                                )
//                            }
//                        }
//                    }
//                }
//            }
        }
    }
}

@Composable
private fun Boxic(
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                onClick()
            }) {
        Box {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    val navViewModel = viewModel<NavigationViewModel>()
    
    PageTopBar(
        title = {
            Text(stringResource(R.string.appearance))
        },
        navigationIcon = NavigationIcon(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = navViewModel::removeLastScreenInStack
        )
    )
}
