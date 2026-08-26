/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.screens.chat.media

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiwazian.messenger.domain.ChatMediaItem
import com.aiwazian.messenger.domain.ChatMediaPage
import com.aiwazian.messenger.domain.DownloadItem
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.playback.VoicePlayerManager
import com.aiwazian.messenger.playback.VoiceQueueItem
import com.aiwazian.messenger.repository.ChatMediaRepository
import com.aiwazian.messenger.repository.ChatRepository
import com.aiwazian.messenger.repository.UserRepository
import com.aiwazian.messenger.utils.DataStoreManager
import com.aiwazian.messenger.utils.DownloaderManager
import com.aiwazian.messenger.utils.FileHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatMediaViewModel @Inject constructor(
    private val chatMediaRepository: ChatMediaRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val voicePlayerManager: VoicePlayerManager,
    private val downloaderManager: DownloaderManager,
    private val fileHandler: FileHandler,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatMediaUiState())
    val uiState = _uiState.asStateFlow()
    
    private var chatId: Long = 0
    private var isInitialized = false
    
    private var mediaCursor: Int? = null
    private var filesCursor: Int? = null
    private var voicesCursor: Int? = null
    private var hasMoreMedia = true
    private var hasMoreFiles = true
    private var hasMoreVoices = true
    private var isLoadingMoreMedia = false
    private var isLoadingMoreFiles = false
    private var isLoadingMoreVoices = false
    
    /**
     * Файлы, которые автозагрузка уже брала в работу.
     *
     * Сетка сообщает об одних и тех же ячейках при каждом сдвиге, а ссылка на
     * скачивание берётся отдельным запросом — без памяти одна прокрутка
     * устроила бы шквал повторных запросов.
     */
    private val autoRequested = mutableSetOf<String>()
    
    private var trackedDownloads = emptySet<String>()
    
    fun init(chatId: Long) {
        if (isInitialized) {
            return
        }
        
        isInitialized = true
        this.chatId = chatId
        
        voicePlayerManager.connect()
        
        loadMe()
        loadMedia()
        loadFiles()
        loadVoices()
        loadCounts()
        observeDownloads()
        observePlayback()
        observeSettings()
    }
    
    private fun loadMe() {
        viewModelScope.launch {
            val myId = userRepository.getMe().firstOrNull()?.id ?: return@launch
            
            _uiState.update { it.copy(myId = myId) }
        }
    }
    
    /*
     * Кэш показывается сразу, ответ сервера его замещает. Пустой кэш в состояние
     * не пишется: иначе снялся бы флаг загрузки, и вкладка мигнула бы надписью
     * «нет вложений» за мгновение до прихода данных.
     */
    private fun loadMedia() {
        viewModelScope.launch {
            val cached = chatMediaRepository.getCachedMedia(chatId)
            
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(media = cached, isMediaLoading = false) }
            }
            
            chatMediaRepository.getMedia(chatId)
                .onSuccess { page -> _uiState.update { it.applyMedia(page, reset = true) } }
                .onFailure { _uiState.update { it.copy(isMediaLoading = false, hasError = true) } }
            
            mediaCursor = _uiState.value.media.lastOrNull()?.id
        }
    }
    
    private fun loadFiles() {
        viewModelScope.launch {
            val cached = chatMediaRepository.getCachedFiles(chatId)
            
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(files = cached, isFilesLoading = false) }
            }
            
            chatMediaRepository.getFiles(chatId)
                .onSuccess { page -> _uiState.update { it.applyFiles(page, reset = true) } }
                .onFailure { _uiState.update { it.copy(isFilesLoading = false, hasError = true) } }
            
            filesCursor = _uiState.value.files.lastOrNull()?.id
        }
    }
    
    private fun loadVoices() {
        viewModelScope.launch {
            val cached = chatMediaRepository.getCachedVoices(chatId)
            
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(voices = cached, isVoicesLoading = false) }
            }
            
            chatMediaRepository.getVoices(chatId)
                .onSuccess { page -> _uiState.update { it.applyVoices(page, reset = true) } }
                .onFailure {
                    _uiState.update { it.copy(isVoicesLoading = false, hasError = true) }
                }
            
            voicesCursor = _uiState.value.voices.lastOrNull()?.id
        }
    }
    
    /**
     * Счётчики для подписи в шапке.
     *
     * Считаются на сервере, а не по загруженным спискам: в них лежит одна
     * страница, а подпись говорит о всём чате.
     */
    private fun loadCounts() {
        viewModelScope.launch {
            chatMediaRepository.getCachedCounts(chatId)?.let { counts ->
                _uiState.update { it.copy(counts = counts) }
            }
            
            chatMediaRepository.getCounts(chatId).onSuccess { counts ->
                _uiState.update { it.copy(counts = counts) }
            }
        }
    }
    
    fun loadMoreMedia() {
        val cursor = mediaCursor
        
        if (isLoadingMoreMedia || !hasMoreMedia || cursor == null) {
            return
        }
        
        isLoadingMoreMedia = true
        
        viewModelScope.launch {
            chatMediaRepository.getMedia(chatId, cursorId = cursor)
                .onSuccess { page ->
                    _uiState.update { it.applyMedia(page, reset = false) }
                    mediaCursor = page.nextCursorId
                    hasMoreMedia = page.nextCursorId != null
                }
            
            isLoadingMoreMedia = false
        }
    }
    
    fun loadMoreFiles() {
        val cursor = filesCursor
        
        if (isLoadingMoreFiles || !hasMoreFiles || cursor == null) {
            return
        }
        
        isLoadingMoreFiles = true
        
        viewModelScope.launch {
            chatMediaRepository.getFiles(chatId, cursorId = cursor)
                .onSuccess { page ->
                    _uiState.update { it.applyFiles(page, reset = false) }
                    filesCursor = page.nextCursorId
                    hasMoreFiles = page.nextCursorId != null
                }
            
            isLoadingMoreFiles = false
        }
    }
    
    fun loadMoreVoices() {
        val cursor = voicesCursor
        
        if (isLoadingMoreVoices || !hasMoreVoices || cursor == null) {
            return
        }
        
        isLoadingMoreVoices = true
        
        viewModelScope.launch {
            chatMediaRepository.getVoices(chatId, cursorId = cursor)
                .onSuccess { page ->
                    _uiState.update { it.applyVoices(page, reset = false) }
                    voicesCursor = page.nextCursorId
                    hasMoreVoices = page.nextCursorId != null
                }
            
            isLoadingMoreVoices = false
        }
    }
    
    /**
     * Догружает то, что сейчас видно на экране.
     *
     * Скачанное просто показывается, а вручную остановленное так и остаётся
     * на паузе: автозагрузка не вправе отменять решение пользователя.
     */
    fun onMediaVisible(items: List<ChatMediaItem>) {
        autoDownload(items)
    }
    
    /**
     * То же для голосовых.
     *
     * Записи качаются сами: не скачанную нельзя ни проиграть, ни измерить, а
     * без длины вторая строка осталась бы половинчатой.
     */
    fun onVoicesVisible(items: List<ChatMediaItem>) {
        autoDownload(items)
    }
    
    fun onMediaClick(item: ChatMediaItem) {
        val downloaded = _uiState.value.media.filter { it.localUri != null }
        val index = downloaded.indexOfFirst { it.fileId == item.fileId }
        
        /* Нескачанное показывать нечего — по нажатию торопим загрузку. */
        if (index == -1) {
            startDownload(item)
            return
        }
        
        _uiState.update {
            it.copy(initialMediaIndex = index, showFullScreenViewer = true)
        }
    }
    
    /**
     * Нажатие по документу.
     *
     * Скачанный открывается системой, идущий — становится на паузу, остальное
     * качается. Тот же порядок, что у вложения в переписке.
     */
    fun onFileClick(item: ChatMediaItem) {
        val localUri = item.localUri
        
        if (localUri != null) {
            fileHandler.openFile(localUri.toString())
            return
        }
        
        toggleDownload(item)
    }
    
    /**
     * Нажатие по голосовому.
     *
     * По той же записи — пауза и возврат с той же секунды, а не начало с нуля:
     * случайное касание иначе стоило бы прослушанного.
     */
    fun onVoiceClick(item: ChatMediaItem) {
        if (item.localUri == null) {
            toggleDownload(item)
            return
        }
        
        if (voicePlayerManager.state.value.currentFileId == item.fileId) {
            voicePlayerManager.togglePlayPause()
            return
        }
        
        voicePlayerManager.play(queue = voiceQueue(), fileId = item.fileId)
    }
    
    /**
     * Запоминает длину, считанную карточкой из файла.
     *
     * Сервер длину не отдаёт, поэтому посчитанное ложится в кэш: при следующем
     * открытии вторая строка готова сразу, без повторного разбора файла.
     */
    fun onVoiceDurationResolved(item: ChatMediaItem, durationMs: Int) {
        if (durationMs <= 0) {
            return
        }
        
        _uiState.update { state ->
            state.copy(
                voices = state.voices.map { voice ->
                    if (voice.fileId == item.fileId) voice.copy(durationMs = durationMs) else voice
                })
        }
        
        viewModelScope.launch {
            chatMediaRepository.saveVoiceDuration(item.fileId, durationMs)
        }
    }
    
    fun onViewerDismiss() {
        _uiState.update { it.copy(showFullScreenViewer = false) }
    }
    
    fun onVideoLoopingChange(isLooping: Boolean) {
        viewModelScope.launch { dataStoreManager.saveVideoLooping(isLooping) }
    }
    
    fun onVideoPlaybackSpeedChange(speed: Float) {
        viewModelScope.launch { dataStoreManager.saveVideoPlaybackSpeed(speed) }
    }
    
    fun onSaveToGallery(uri: Uri) {
        fileHandler.saveToGallery(uri.toString())
    }
    
    private fun autoDownload(items: List<ChatMediaItem>) {
        items.forEach { item ->
            if (item.localUri != null) {
                return@forEach
            }
            
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                return@forEach
            }
            
            if (!autoRequested.add(item.fileId)) {
                return@forEach
            }
            
            startDownload(item)
        }
    }
    
    private fun toggleDownload(item: ChatMediaItem) {
        when (item.status) {
            DownloadStatus.DOWNLOADING -> viewModelScope.launch {
                downloaderManager.pause(item.fileId)
            }
            
            DownloadStatus.PAUSED -> viewModelScope.launch {
                downloaderManager.resume(item.fileId)
            }
            
            else -> startDownload(item)
        }
    }
    
    private fun startDownload(item: ChatMediaItem) {
        viewModelScope.launch {
            chatRepository.getDownloadUrl(chatId, item.messageId, item.fileId)
                .onSuccess { url -> downloaderManager.download(url, item.name, item.fileId) }
                .onFailure { autoRequested.remove(item.fileId) }
        }
    }
    
    /**
     * Очередь проигрывателя из уже скачанных записей.
     *
     * Нескачанные пропускаются: после конца записи проигрыватель берёт
     * следующую сам, и элемент без файла оборвал бы цепочку.
     */
    private fun voiceQueue(): List<VoiceQueueItem> = _uiState.value.voices.mapNotNull { voice ->
        voice.localUri?.let { uri ->
            VoiceQueueItem(
                uri = uri,
                fileId = voice.fileId,
                title = voice.name,
                artworkUri = null
            )
        }
    }
    
    private fun observeDownloads() {
        viewModelScope.launch {
            downloaderManager.activeDownloads.collect { downloads ->
                val active = downloads.associateBy { it.fileId }
                val finished = trackedDownloads - active.keys
                trackedDownloads = active.keys
                
                _uiState.update { state ->
                    state.copy(
                        media = state.media.applyProgress(active),
                        files = state.files.applyProgress(active),
                        voices = state.voices.applyProgress(active)
                    )
                }
                
                /*
                 * Ушедшее из очереди уже лежит на диске, и путь к нему есть только
                 * в Room. Переспрашиваем только по факту завершения, а не на каждом
                 * проценте прогресса.
                 */
                if (finished.isNotEmpty()) {
                    refreshLocalState()
                }
            }
        }
    }
    
    private fun observePlayback() {
        viewModelScope.launch {
            voicePlayerManager.state.collect { state ->
                _uiState.update {
                    it.copy(
                        playingFileId = state.currentFileId,
                        isVoicePlaying = state.isPlaying
                    )
                }
            }
        }
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            dataStoreManager.getVideoLooping().collect { isLooping ->
                _uiState.update { it.copy(isVideoLooping = isLooping) }
            }
        }
        viewModelScope.launch {
            dataStoreManager.getVideoPlaybackSpeed().collect { speed ->
                _uiState.update { it.copy(videoPlaybackSpeed = speed) }
            }
        }
    }
    
    private suspend fun refreshLocalState() {
        val media = chatMediaRepository.withLocalState(_uiState.value.media)
        val files = chatMediaRepository.withLocalState(_uiState.value.files)
        val voices = chatMediaRepository.withLocalState(_uiState.value.voices)
        
        _uiState.update { it.copy(media = media, files = files, voices = voices) }
        
        /* Доскачанное попадает в очередь, не прерывая текущее воспроизведение. */
        voicePlayerManager.updateQueue(voiceQueue())
    }
    
    private fun List<ChatMediaItem>.applyProgress(active: Map<String, DownloadItem>) = map { item ->
        val download = active[item.fileId] ?: return@map item
        
        item.copy(status = download.status, progress = download.progress)
    }
    
    private fun ChatMediaUiState.applyMedia(page: ChatMediaPage, reset: Boolean) = copy(
        media = if (reset) page.items else media + page.items,
        isMediaLoading = false,
        hasError = false
    )
    
    private fun ChatMediaUiState.applyFiles(page: ChatMediaPage, reset: Boolean) = copy(
        files = if (reset) page.items else files + page.items,
        isFilesLoading = false,
        hasError = false
    )
    
    private fun ChatMediaUiState.applyVoices(page: ChatMediaPage, reset: Boolean) = copy(
        voices = if (reset) page.items else voices + page.items,
        isVoicesLoading = false,
        hasError = false
    )
}
