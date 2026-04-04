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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExpandedFullScreenSearchBar
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.ui.components.AnimatedDotsText
import com.aiwazian.messenger.ui.components.ChatCard
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavHost
import com.aiwazian.messenger.ui.screens.main.search.ChatResultsList
import com.aiwazian.messenger.ui.screens.main.search.EmptySearchResultsPlaceholder
import com.aiwazian.messenger.ui.screens.main.search.FileResultsList
import com.aiwazian.messenger.ui.screens.main.search.LoadingPlaceholder
import com.aiwazian.messenger.ui.screens.main.search.SearchViewModel
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
                        Settings.EXTRA_APP_PACKAGE, context.packageName
                    )
                }
                context.startActivity(intent)
            }
            
            else -> {}
        }
    }
    
    if (showPermissionRationale) {
        NotificationBottomModal(enable = {
            viewModel.hidePermissionRationale()
            viewModel.openNotificationSettings()
        }, disable = {
            viewModel.hidePermissionRationale()
        })
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, POST_NOTIFICATIONS
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
                }, viewModel.user.collectAsState().value
            )
        },
    ) {
        Content(
            drawerState, viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationBottomModal(
    enable: () -> Unit, disable: () -> Unit
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
                text = "Открыть настройки", modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun Content(
    drawerState: DrawerState, mainViewModel: MainViewModel
) {
    val navHost = LocalNavHost.current
    
    val chats by mainViewModel.chats.collectAsState()
    
    val hasPasscode by mainViewModel.hasPasscode.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        DefaultTopBar(
            drawerState = drawerState, isLockApp = hasPasscode, onLockClick = {
                scope.launch {
                    mainViewModel.lockApp()
                }
            }, socketState = mainViewModel.socketState.collectAsState().value
        )
    }, floatingActionButton = {
        FloatingButton(onClick = {
            navHost.add(AppRoute.NewMessage)
        })
    }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            if (chats.isEmpty()) {
                EmptyChatPlaceholder(text = "Чтобы начать общение нажмите на поле поиска сверху экрана и найдите пользователя по его @username")
            } else {
                LazyColumn {
                    items(chats) { chat ->
                        ChatCard(chat = chat, onClickChat = {
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
    text: String, animation: String? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
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
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun FloatingButton(onClick: () -> Unit) {
    FloatingActionButton(
        shape = CircleShape, onClick = onClick, containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = Icons.Default.Create, contentDescription = null
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class
)
@Composable
private fun DefaultTopBar(
    drawerState: DrawerState,
    isLockApp: Boolean,
    onLockClick: () -> Unit,
    socketState: ConnectionState,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val searchUiState by searchViewModel.uiState.collectAsState()
    val textFieldState = rememberTextFieldState(searchUiState.query)
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    val navHost = LocalNavHost.current
    
    LaunchedEffect(textFieldState.text) {
        searchViewModel.onQueryChange(textFieldState.text.toString())
    }
    
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = {},
            placeholder = {
                AnimatedContent(
                    targetState = socketState,
                    contentKey = { it },
                    transitionSpec = {
                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                    },
                    label = "connection_state_animation"
                ) { state ->
                    when (state) {
                        ConnectionState.CONNECTED -> Text(stringResource(R.string.search))
                        ConnectionState.DISCONNECTED -> AnimatedDotsText(stringResource(R.string.waiting_for_network))
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> AnimatedDotsText(
                            stringResource(R.string.connecting)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                AnimatedContent(searchBarState.currentValue) {
                    if (it == SearchBarValue.Collapsed) {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                Icons.Rounded.Menu, null
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                searchBarState.animateToCollapsed()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack, null
                            )
                        }
                    }
                }
            },
            trailingIcon = if (isLockApp) {
                {
                    AnimatedContent(searchBarState.currentValue) {
                        if (it == SearchBarValue.Collapsed) {
                            IconButton(onClick = onLockClick) {
                                Icon(
                                    imageVector = Icons.Rounded.LockOpen,
                                    contentDescription = "Lock"
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                scope.launch { searchBarState.animateToCollapsed() }
                                textFieldState.edit {
                                    replace(
                                        0, length, ""
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close search"
                                )
                            }
                        }
                    }
                }
            } else null)
    }
    
    AppBarWithSearch(
        state = searchBarState, inputField = inputField
    )
    
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.colors(dividerColor = Color.Transparent)
    ) {
        var selectedIndex by remember { mutableIntStateOf(searchUiState.activeTab) }
        val pagerState = rememberPagerState(pageCount = { 2 })
        
        LaunchedEffect(selectedIndex) {
            pagerState.animateScrollToPage(selectedIndex)
            searchViewModel.onTabChange(selectedIndex)
        }
        
        LaunchedEffect(pagerState.currentPage) {
            selectedIndex = pagerState.currentPage
        }
        
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            divider = {},
        ) {
            Tab(
                selected = selectedIndex == 0,
                onClick = { selectedIndex = 0 },
                modifier = Modifier.clip(MaterialTheme.shapes.medium),
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = stringResource(R.string.chats), modifier = Modifier.padding(6.dp)
                )
            }
            Tab(
                selected = selectedIndex == 1,
                onClick = { selectedIndex = 1 },
                modifier = Modifier.clip(MaterialTheme.shapes.medium),
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = stringResource(R.string.files), modifier = Modifier.padding(6.dp)
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
        ) { page ->
            when (page) {
                0 -> {
                    if (searchUiState.isChatLoading && searchUiState.chatResults.isEmpty()) {
                        LoadingPlaceholder()
                    } else if (searchUiState.chatResults.isEmpty() && searchUiState.query.isNotBlank()) {
                        EmptySearchResultsPlaceholder()
                    } else {
                        ChatResultsList(
                            results = searchUiState.chatResults,
                            isLoading = searchUiState.isChatLoading,
                            onLoadMore = searchViewModel::loadMore,
                            onChatClick = { chatId ->
                                scope.launch {
                                    searchBarState.animateToCollapsed()
                                    navHost.add(AppRoute.Chat(chatId))
                                }
                            })
                    }
                }
                
                1 -> {
                    if (searchUiState.isFileLoading && searchUiState.fileResults.isEmpty()) {
                        LoadingPlaceholder()
                    } else if (searchUiState.fileResults.isEmpty() && searchUiState.query.isNotBlank()) {
                        EmptySearchResultsPlaceholder()
                    } else {
                        FileResultsList(
                            results = searchUiState.fileResults,
                            state = searchUiState,
                            isLoading = searchUiState.isFileLoading,
                            onLoadMore = searchViewModel::loadMore,
                            onFileClick = searchViewModel::onFileClicked
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    onClose: () -> Unit, user: User, nativeAdViewModel: NativeAdViewModel = hiltViewModel()
) {
    val navHost = LocalNavHost.current
    
    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(
                top = 80.dp, bottom = 40.dp
            )
        ) {
            Text(
                text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                modifier = Modifier.padding(
                    start = 20.dp, end = 20.dp, bottom = 40.dp
                ),
                fontSize = 24.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            
            DrawerItem(
                label = stringResource(R.string.profile), icon = Icons.Rounded.AccountCircle
            ) {
                onClose.invoke()
                navHost.add(AppRoute.Profile(user.id))
            }
            
            DrawerItem(
                label = stringResource(R.string.saved_messages), icon = Icons.Rounded.BookmarkBorder
            ) {
                onClose.invoke()
                navHost.add(AppRoute.Chat(user.id))
            }
            
            DrawerItem(
                label = stringResource(R.string.settings), icon = Icons.Rounded.Settings
            ) {
                onClose.invoke()
                navHost.add(AppRoute.Settings)
            }
        }
        
        Box(Modifier.weight(1f))
        
        val isAdLoaded by nativeAdViewModel.isAdLoaded.collectAsState()
        val nativeAd = nativeAdViewModel.nativeAd
        
        if (isAdLoaded) {
            val textColor = MaterialTheme.colorScheme.onSurface
            
            AndroidView(
                modifier = Modifier.fillMaxWidth(), factory = { context ->
                    LayoutInflater.from(context).inflate(
                        R.layout.fragment_ad_mob, null, false
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
                        
                        val viewBinder =
                            NativeAdViewBinder.Builder(nativeAdView).setTitleView(title)
                                .setDomainView(domain).setWarningView(warning)
                                .setSponsoredView(sponsored).setFeedbackView(feedback)
                                .setMediaView(media).setIconView(appIcon).setPriceView(price)
                                .setFaviconView(favicon).build()
                        
                        nativeAd?.bindNativeAd(viewBinder)
                    }
                })
        }
    }
}

@Composable
private fun DrawerItem(
    label: String, icon: ImageVector, onClick: () -> Unit
) {
    NavigationDrawerItem(
        shape = RectangleShape, label = {
            Text(text = label)
        }, icon = {
            Icon(
                imageVector = icon, contentDescription = null
            )
        }, selected = false, onClick = onClick
    )
}
