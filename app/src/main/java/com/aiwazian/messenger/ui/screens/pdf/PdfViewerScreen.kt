/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.pdf

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.rememberPdfViewerState
import com.aiwazian.messenger.R
import com.aiwazian.messenger.ui.app.AppDropdownMenu
import com.aiwazian.messenger.ui.app.AppDropdownMenuItem
import com.aiwazian.messenger.ui.app.AppSnackbar
import com.aiwazian.messenger.ui.components.BottomBarScrim
import com.aiwazian.messenger.ui.components.TopBarScrim
import com.aiwazian.messenger.ui.components.navigation.LocalNavBackStack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPdfApi::class)
@Composable
fun PdfViewerScreen(
    chatId: Long,
    messageId: Long,
    fileId: String,
    fileName: String,
    postId: Long?,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(chatId, messageId, fileId, fileName, postId) {
        viewModel.init(chatId, messageId, fileId, fileName, postId)
    }

    val navBackStack = LocalNavBackStack.current
    val context = LocalContext.current
    val loader = remember { SandboxedPdfLoader(context) }
    val documentUri = remember(uiState.filePath) {
        uiState.filePath?.let { path -> Uri.fromFile(File(path)) }
    }
    val documentState by produceState<PdfViewerDocumentState>(
        initialValue = PdfViewerDocumentState.Loading,
        key1 = documentUri
    ) {
        if (documentUri == null) return@produceState

        value = try {
            withTimeout(PdfOpenTimeout) {
                PdfViewerDocumentState.Loaded(loader.openDocument(documentUri))
            }
        } catch (_: TimeoutCancellationException) {
            Log.e(PdfViewerLogTag, "PDF open timed out: $documentUri")
            PdfViewerDocumentState.Failed
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(PdfViewerLogTag, "Failed to open pdf document: $documentUri", error)
            PdfViewerDocumentState.Failed
        }
    }
    val pdfViewerState = rememberPdfViewerState()
    val loadedDocument = (documentState as? PdfViewerDocumentState.Loaded)?.pdfDocument

    DisposableEffect(loadedDocument) {
        onDispose { loadedDocument?.close() }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is PdfViewerUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = effect.message.asString(context),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbar(snackbarHostState) },
        topBar = {
            PdfViewerHeader(
                name = uiState.fileName,
                pageCount = loadedDocument?.pageCount ?: 0,
                currentPage = pdfViewerState.firstVisiblePage + 1,
                onBackClick = { navBackStack.removeLastOrNull() },
                onSaveToDownloads = viewModel::saveToDownloads
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isFailed || documentState is PdfViewerDocumentState.Failed -> Text(
                        text = stringResource(R.string.failed_to_load_file),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    loadedDocument != null -> PdfViewer(
                        pdfDocument = loadedDocument,
                        state = pdfViewerState,
                        isImageSelectionEnabled = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding
                    )

                    else -> CircularWavyProgressIndicator()
                }
            }

            TopBarScrim(height = innerPadding.calculateTopPadding())

            BottomBarScrim(height = innerPadding.calculateBottomPadding())
        }
    }
}

@Composable
private fun PdfViewerHeader(
    name: String,
    pageCount: Int,
    currentPage: Int,
    onBackClick: () -> Unit,
    onSaveToDownloads: () -> Unit
) {
    var showMoreActions by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = CircleShape
            )
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            if (pageCount > 0) {
                Text(
                    text = "$currentPage / $pageCount",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape
                )
        ) {
            IconButton(onClick = { showMoreActions = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.actions)
                )
            }

            AppDropdownMenu(
                expanded = showMoreActions,
                onDismissRequest = { showMoreActions = false }
            ) {
                AppDropdownMenuItem(
                    text = stringResource(R.string.save_to_downloads),
                    onClick = {
                        showMoreActions = false
                        onSaveToDownloads()
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Download, contentDescription = null)
                    }
                )
            }
        }
    }
}

private val PdfOpenTimeout = 60.seconds

private const val PdfViewerLogTag = "PdfViewer"

private sealed interface PdfViewerDocumentState {
    data object Loading : PdfViewerDocumentState
    data object Failed : PdfViewerDocumentState
    data class Loaded(val pdfDocument: PdfDocument) : PdfViewerDocumentState
}
