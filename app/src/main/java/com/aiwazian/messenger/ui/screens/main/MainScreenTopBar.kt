package com.aiwazian.messenger.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.MarkChatRead
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aiwazian.messenger.R
import com.aiwazian.messenger.enums.ConnectionState
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.components.AnimatedDotsText
import com.aiwazian.messenger.ui.components.navigation.AppRoute
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import com.aiwazian.messenger.ui.screens.main.search.ChatResultsList
import com.aiwazian.messenger.ui.screens.main.search.EmptySearchResultsPlaceholder
import com.aiwazian.messenger.ui.screens.main.search.LoadingPlaceholder
import com.aiwazian.messenger.ui.screens.main.search.SearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultTopBar(
    drawerState: DrawerState,
    passcodeEnabled: Boolean,
    onLockClick: () -> Unit,
    socketState: ConnectionState,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    val searchUiState by searchViewModel.uiState.collectAsState()
    val textFieldState = rememberTextFieldState(searchUiState.query)
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    
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
                        ConnectionState.DISCONNECTED -> AnimatedDotsText(
                            stringResource(R.string.waiting_for_network)
                        )
                        
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
                            textFieldState.edit {
                                replace(0, length, "")
                            }
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
            trailingIcon = {
                AnimatedContent(searchBarState.currentValue) {
                    if (it == SearchBarValue.Collapsed) {
                        if (passcodeEnabled) {
                            IconButton(onClick = onLockClick) {
                                Icon(
                                    imageVector = Icons.Rounded.LockOpen,
                                    contentDescription = "Lock"
                                )
                            }
                        }
                    } else if (textFieldState.text.isNotEmpty()) {
                        IconButton(onClick = {
                            textFieldState.edit {
                                replace(0, length, "")
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close search"
                            )
                        }
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
    
    AppBarWithSearch(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.appBarWithSearchColors(appBarContainerColor = Color.Transparent)
    )
    
    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            dividerColor = Color.Transparent,
            inputFieldColors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    ) {
        if (searchUiState.isChatLoading && searchUiState.chatResults.isEmpty()) {
            LoadingPlaceholder()
        } else if (searchUiState.chatResults.isEmpty() && searchUiState.query.isNotBlank()) {
            EmptySearchResultsPlaceholder()
        } else {
            ChatResultsList(
                results = searchUiState.chatResults,
                query = searchUiState.query,
                isLoading = searchUiState.isChatLoading,
                onLoadMore = searchViewModel::loadMore,
                onChatClick = { chatId, chatName ->
                    scope.launch {
                        navBackStack.add(AppRoute.Chat(chatId, chatName))
                    }
                })
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onPinClick: () -> Unit,
    onUnpinClick: () -> Unit,
    hasUnpinnedChats: Boolean,
    onMarkReadClick: () -> Unit,
    onMarkUnreadClick: () -> Unit,
    hasUnreadChats: Boolean
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Rounded.Close, null)
                }
                AnimatedContent(
                    modifier = Modifier.padding(start = 10.dp, end = 18.dp),
                    targetState = selectedCount,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { it } + fadeIn() + scaleIn() togetherWith slideOutVertically { -it } + fadeOut() + scaleOut()
                        } else {
                            slideInVertically { -it } + fadeIn() + scaleIn() togetherWith slideOutVertically { it } + fadeOut() + scaleOut()
                        }
                    }) { count ->
                    Text(text = "$count")
                }
            }
        }, navigationIcon = {}, actions = {
            var expand by remember { mutableStateOf(false) }
            
            IconButton(
                onClick = { expand = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Icon(Icons.Rounded.MoreVert, null)
            }
            
            AppDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                AppDropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.PushPin,
                            null,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(45f)
                        )
                    },
                    text = stringResource(if (hasUnpinnedChats) R.string.pin else R.string.unpin),
                    onClick = {
                        if (hasUnpinnedChats) {
                            onPinClick()
                        } else {
                            onUnpinClick()
                        }
                    })
                
                AppDropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            if (hasUnreadChats) Icons.Outlined.MarkChatRead
                            else Icons.Outlined.MarkChatUnread,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = stringResource(
                        if (hasUnreadChats) R.string.mark_as_read
                        else R.string.mark_as_unread
                    ),
                    onClick = {
                        expand = false
                        if (hasUnreadChats) {
                            onMarkReadClick()
                        } else {
                            onMarkUnreadClick()
                        }
                    })
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}
