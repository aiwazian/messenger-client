/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.main

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.ui.components.ChatCard
import com.aiwazian.messenger.ui.components.SwipeableChatCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current
    
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            viewModel.showPermissionRationale()
        }
    }
    
    val uiEffect by viewModel.uiEffect.collectAsState(null)
    
    LaunchedEffect(uiEffect) {
        when (uiEffect) {
            is MainUiEffect.ShowPermissionRationale -> {
                showPermissionRationale = true
            }
            
            is MainUiEffect.HidePermissionRationale -> {
                showPermissionRationale = false
            }
            
            is MainUiEffect.OpenNotificationSettings -> {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        context.packageName
                    )
                }
                context.startActivity(intent)
            }
            
            else -> {}
        }
    }
    
    if (showPermissionRationale) {
        NotificationBottomModal(
            enable = {
                viewModel.hidePermissionRationale()
                viewModel.openNotificationSettings()
            },
            disable = {
                viewModel.hidePermissionRationale()
            })
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }
    
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onClose = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                viewModel.user.collectAsState().value
            )
        },
    ) {
        Content(
            drawerState,
            viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationBottomModal(
    enable: () -> Unit,
    disable: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = disable,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Column {
                Text(
                    text = "Включите уведомления",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
                
                Text(
                    text = "Разрешите приложению отправлять Вам уведомления, чтобы не пропустить сообщения от друзей и родных.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Button(
            onClick = enable,
            modifier = Modifier
                .padding(15.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Открыть настройки",
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun Content(
    drawerState: DrawerState,
    mainViewModel: MainViewModel
) {
    val navHost = LocalNavHost.current
    
    val chats by mainViewModel.chats.collectAsState()
    
    val hasPasscode by mainViewModel.hasPasscode.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    val scope = rememberCoroutineScope()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DefaultTopBar(
                drawerState = drawerState,
                isLockApp = hasPasscode,
                onLockClick = {
                    scope.launch {
                        mainViewModel.lockApp()
                    }
                },
                isConnected = true
            )
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(snackbarHostState)
        },
        floatingActionButton = {
            FloatingButton(onClick = {
                navHost.add(AppRoute.NewMessage)
            })
        }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            if (chats.isEmpty()) {
                EmptyChatPlaceholder(text = "Чтобы начать общение нажмите на значок поиска в правом верхнем углу и найдите пользователя по его @username")
            } else {
                LazyColumn {
                    items(chats) { chat ->
                        ChatCard(
                            chat = chat,
                            onClickChat = {
                                navHost.add(AppRoute.Chat(chat.id))
                            })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatPlaceholder(
    text: String,
    animation: String? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (animation != null) {
            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.Asset(animation)
            )
            
            LottieAnimation(
                composition = composition,
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 10.dp),
                iterations = LottieConstants.IterateForever,
                isPlaying = true
            )
        }
        
        Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FloatingButton(onClick: () -> Unit) {
    FloatingActionButton(
        shape = CircleShape,
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = Icons.Default.Create,
            contentDescription = null
        )
    }
}

@Composable
private fun SwipeDismissSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.fillMaxWidth()
    ) { data ->
        var dismissed by remember { mutableStateOf(false) }
        
        if (!dismissed) {
            val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
            
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                backgroundContent = { },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = null,
                            )
                            
                            Text(
                                text = data.visuals.message
                            )
                        }
                        
                        TextButton(
                            onClick = { data.performAction() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Reply,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            data.visuals.actionLabel?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            data.dismiss()
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
private fun DefaultTopBar(
    drawerState: DrawerState,
    isLockApp: Boolean,
    onLockClick: () -> Unit,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf("") }
    val state = rememberSearchBarState()
    
    BackHandler(state.currentValue == SearchBarValue.Expanded) {
        scope.launch {
            state.animateToCollapsed()
        }
    }
    
    val field: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            query = value,
            onSearch = {},
            onQueryChange = {
                value = it
                scope.launch {
                    state.animateToExpanded()
                }
            },
            expanded = state.currentValue == SearchBarValue.Expanded,
            onExpandedChange = {
                scope.launch {
                    if (it) state.animateToExpanded() else state.animateToCollapsed()
                }
            },
            placeholder = { Text(stringResource(R.string.search)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        state.animateToExpanded()
                    }
                },
            leadingIcon = {
                AnimatedContent(state.currentValue) {
                    if (it == SearchBarValue.Collapsed) {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                Icons.Rounded.Menu,
                                null
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                state.animateToCollapsed()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                null
                            )
                        }
                    }
                }
            },
            trailingIcon = if (isLockApp) {
                {
                    IconButton(onClick = onLockClick) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = "Lock"
                        )
                    }
                }
            } else null
        )
    }
    
    AppBarWithSearch(
        state = state,
        inputField = field
    )
    
    ExpandedFullScreenContainedSearchBar(
        state = state,
        inputField = field
    ) { }
}

@Composable
private fun DrawerContent(
    onClose: () -> Unit,
    user: User
) {
    val context = LocalContext.current
    
    val navHost = LocalNavHost.current
    
    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(
                top = 80.dp,
                bottom = 40.dp
            )
        ) {
            Text(
                text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 40.dp
                ),
                fontSize = 24.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            
            DrawerItem(
                label = stringResource(R.string.profile),
                icon = Icons.Rounded.AccountCircle
            ) {
                onClose.invoke()
                navHost.add(AppRoute.Profile(user.id))
            }
            
            DrawerItem(
                label = stringResource(R.string.saved_messages),
                icon = Icons.Rounded.BookmarkBorder
            ) {
                onClose.invoke()
                navHost.add(AppRoute.Chat(user.id))
            }
            
            DrawerItem(
                label = stringResource(R.string.settings),
                icon = Icons.Rounded.Settings
            ) {
                onClose.invoke()
                navHost.add(AppRoute.Settings)
            }
        }
        
        Box(Modifier.weight(1f))
        
        val adViewModel = hiltViewModel<AdViewModel>()
        
        LaunchedEffect(Unit) {
            adViewModel.initialize(context)
        }
        
        val isAdLoaded by adViewModel.isAdLoaded.collectAsState()
        val nativeAd = adViewModel.nativeAd
        
        if (isAdLoaded) {
            val textColor = MaterialTheme.colorScheme.onSurface
            
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    LayoutInflater.from(context).inflate(
                        R.layout.fragment_ad_mob,
                        null,
                        false
                    ).apply {
                        val title = findViewById<TextView>(R.id.title)
                        val domain = findViewById<TextView>(R.id.domain)
                        val warning = findViewById<TextView>(R.id.warning)
                        val sponsored = findViewById<TextView>(R.id.sponsored)
                        val feedback = findViewById<ImageView>(R.id.feedback)
                        val media = findViewById<MediaView>(R.id.media)
                        val favicon = findViewById<ImageView>(R.id.favicon)
                        val price = findViewById<TextView>(R.id.price)
                        val appIcon = findViewById<ImageView>(R.id.app_icon)
                        
                        title.setTextColor(textColor.toArgb())
                        domain.setTextColor(textColor.toArgb())
                        warning.setTextColor(textColor.toArgb())
                        sponsored.setTextColor(textColor.toArgb())
                        price.setTextColor(textColor.toArgb())
                        
                        val nativeAdView = findViewById<NativeAdView>(R.id.native_ad_container)
                        
                        val viewBinder = NativeAdViewBinder.Builder(nativeAdView)
                            .setTitleView(title)
                            .setDomainView(domain)
                            .setWarningView(warning)
                            .setSponsoredView(sponsored)
                            .setFeedbackView(feedback)
                            .setMediaView(media)
                            .setIconView(appIcon)
                            .setPriceView(price)
                            .setFaviconView(favicon)
                            .build()
                        
                        nativeAd?.bindNativeAd(viewBinder)
                    }
                })
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        shape = RectangleShape,
        label = {
            Text(text = label)
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        selected = false,
        onClick = onClick
    )
}
