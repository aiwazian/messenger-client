package com.aiwazian.messenger.ui.screens.main

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.MainActivity
import com.aiwazian.messenger.R
import com.aiwazian.messenger.domain.User
import com.aiwazian.messenger.enums.ThemeOption
import com.aiwazian.messenger.ui.app.AppBottomSheet
import com.aiwazian.messenger.ui.components.ChatAvatar
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.yandex.mobile.ads.common.AdTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun DrawerContent(
    drawerState: DrawerState,
    user: User,
    theme: ThemeOption,
    showAccountSheet: Boolean,
    onShowAccountSheet: () -> Unit,
    onHideAccountSheet: () -> Unit,
    accountSwitcherViewModel: AccountSwitcherViewModel = hiltViewModel(),
    adBannerViewModel: AdBannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val navBackStack = LocalNavBackStack.current
    val scope = rememberCoroutineScope()
    val screenHeight = LocalWindowInfo.current.containerDpSize.height
    val accountSwitcherState by accountSwitcherViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        accountSwitcherViewModel.sideEffect.collectLatest { sideEffect ->
            when (sideEffect) {
                is AccountSwitcherSideEffect.AccountSwitched -> {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                }
            }
        }
    }
    
    val verticalPadding = if (screenHeight < 400.dp) {
        20.dp
    } else {
        80.dp
    }
    
    val maxAdHeight = if (screenHeight < 400.dp) {
        100.dp
    } else {
        300.dp
    }
    
    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        windowInsets = WindowInsets()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = verticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(onClick = onShowAccountSheet) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert, contentDescription = null
                    )
                }
            }
            
            DrawerItem(
                label = stringResource(R.string.profile), icon = Icons.Outlined.AccountCircle
            ) {
                scope.launch {
                    drawerState.close()
                }
                navBackStack.add(
                    AppRoute.Profile(
                        profileId = user.id,
                        profileName = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                        avatarUri = user.avatars.firstOrNull()?.uri.toString()
                    )
                )
            }
            
            val savedMessagesText = stringResource(R.string.saved_messages)
            
            DrawerItem(
                label = savedMessagesText, icon = Icons.Rounded.BookmarkBorder
            ) {
                scope.launch {
                    drawerState.close()
                }
                navBackStack.add(
                    AppRoute.Chat(
                        chatId = user.id,
                        chatName = savedMessagesText,
                        avatarUri = user.avatars.firstOrNull()?.uri?.toString()
                    )
                )
            }
            
            DrawerItem(
                label = stringResource(R.string.settings), icon = Icons.Outlined.Settings
            ) {
                scope.launch {
                    drawerState.close()
                }
                navBackStack.add(AppRoute.Settings)
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        val adTheme =
            if (theme == ThemeOption.DARK || theme == ThemeOption.SYSTEM && isSystemInDarkTheme()) {
                AdTheme.DARK
            } else {
                AdTheme.LIGHT
            }
        
        LaunchedEffect(Unit) {
            adBannerViewModel.ensureInitialLoad(adTheme)
        }
        
        AndroidView(
            factory = { adBannerViewModel.bannerView },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
    
    if (showAccountSheet) {
        AccountSwitcherBottomSheet(
            currentUser = accountSwitcherState.currentUser,
            otherAccounts = accountSwitcherState.otherAccounts,
            onAccountClick = accountSwitcherViewModel::switchAccount,
            onAddAccount = {
                onHideAccountSheet()
                scope.launch {
                    drawerState.close()
                }
                navBackStack.add(AppRoute.Login)
            },
            onDismissRequest = onHideAccountSheet
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSwitcherBottomSheet(
    currentUser: User?,
    otherAccounts: List<User>,
    onAccountClick: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    
    AppBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        currentUser?.let { user ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ChatAvatar(
                    id = user.id,
                    chatName = user.firstName,
                    avatarUri = user.avatars.firstOrNull()?.uri,
                    size = 64.dp,
                    sharedTransition = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        otherAccounts.forEach { account ->
            AccountRow(
                user = account,
                onClick = { onAccountClick(account.id) }
            )
        }
        
        AddAccountRow(onClick = onAddAccount)
    }
}

@Composable
private fun AccountRow(
    user: User, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChatAvatar(
            id = user.id,
            chatName = user.firstName,
            avatarUri = user.avatars.firstOrNull()?.uri,
            size = 40.dp,
            sharedTransition = false
        )
        Text(
            text = "${user.firstName} ${user.lastName.orEmpty()}".trim(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddAccountRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = stringResource(R.string.add_account),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
