package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.WhatsAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalCoroutinesApi::class)
class WhatsAppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WhatsAppRepository
    
    // UI Theme Preferences
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Screen State
    val contacts: StateFlow<List<Contact>>
    val chats: StateFlow<List<Chat>>
    val statusUpdates: StateFlow<List<StatusUpdate>>
    val callLogs: StateFlow<List<CallLog>>

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected Chat
    private val _selectedChatId = MutableStateFlow<Int?>(null)
    val selectedChatId: StateFlow<Int?> = _selectedChatId.asStateFlow()

    private val _selectedChat = MutableStateFlow<Chat?>(null)
    val selectedChat: StateFlow<Chat?> = _selectedChat.asStateFlow()

    // Dynamic message loading based on selectedChatId
    val currentMessages: StateFlow<List<Message>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId != null) {
                repository.getMessagesForChat(chatId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Typings states (chatId -> Boolean)
    private val _typingStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val typingStates: StateFlow<Map<Int, Boolean>> = _typingStates.asStateFlow()

    // Audio recording state
    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    // Quoted Message
    private val _quotedMessage = MutableStateFlow<Message?>(null)
    val quotedMessage: StateFlow<Message?> = _quotedMessage.asStateFlow()

    // Active Status viewing
    private val _viewingStatusList = MutableStateFlow<List<StatusUpdate>?>(null)
    val viewingStatusList: StateFlow<List<StatusUpdate>?> = _viewingStatusList.asStateFlow()
    
    private val _currentStatusIndex = MutableStateFlow(0)
    val currentStatusIndex: StateFlow<Int> = _currentStatusIndex.asStateFlow()

    // Calling State
    private val _activeCallState = MutableStateFlow<CallState?>(null)
    val activeCallState: StateFlow<CallState?> = _activeCallState.asStateFlow()

    data class CallState(
        val contactId: Int,
        val name: String,
        val avatar: String,
        val isVideo: Boolean,
        val isIncoming: Boolean,
        val isConnected: Boolean = false,
        val durationSec: Int = 0
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WhatsAppRepository(database.whatsAppDao())

        contacts = repository.allContacts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        chats = repository.allChats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        statusUpdates = repository.allStatusUpdates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        callLogs = repository.allCallLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Seed initial data if database is empty
        viewModelScope.launch {
            repository.allContacts.first().let { list ->
                if (list.isEmpty()) {
                    seedDatabase()
                }
            }
        }

        // Keep _selectedChat in sync with _selectedChatId
        viewModelScope.launch {
            _selectedChatId.collect { id ->
                if (id != null) {
                    _selectedChat.value = repository.getChatById(id)
                } else {
                    _selectedChat.value = null
                }
            }
        }
    }

    private suspend fun seedDatabase() {
        // 1. Create Contacts
        val geminiContactId = repository.insertContact(Contact(
            name = "Gemini AI Bot",
            avatar = "gemini_bot",
            statusMessage = "Disponível - Respostas por Inteligência Artificial",
            isGroup = false,
            isAi = true
        )).toInt()

        val maeContactId = repository.insertContact(Contact(
            name = "Mãe ❤️",
            avatar = "mother",
            statusMessage = "A pressa é a inimiga da perfeição.",
            isGroup = false,
            isAi = false
        )).toInt()

        val carlosContactId = repository.insertContact(Contact(
            name = "Carlos Santos",
            avatar = "carlos",
            statusMessage = "No trabalho. Envie apenas mensagens importantes.",
            isGroup = false,
            isAi = false
        )).toInt()

        val groupContactId = repository.insertContact(Contact(
            name = "Família Silva 👨‍👩‍👧‍👦",
            avatar = "group_fam",
            statusMessage = "Grupo da Família",
            isGroup = true,
            isAi = false
        )).toInt()

        val systemTime = System.currentTimeMillis()

        // 2. Create Chats
        val geminiChatId = repository.insertChat(Chat(
            contactId = geminiContactId,
            name = "Gemini AI Bot",
            avatar = "gemini_bot",
            lastMessage = "Olá! Eu sou o Gemini AI Bot. Como posso te ajudar hoje?",
            lastTimestamp = systemTime - 60000 * 5,
            isGroup = false,
            unreadCount = 1,
            isAi = true
        )).toInt()

        val maeChatId = repository.insertChat(Chat(
            contactId = maeContactId,
            name = "Mãe ❤️",
            avatar = "mother",
            lastMessage = "Filho, compra o pão integral por favor!",
            lastTimestamp = systemTime - 60000 * 15,
            isGroup = false,
            unreadCount = 0,
            isAi = false
        )).toInt()

        val carlosChatId = repository.insertChat(Chat(
            contactId = carlosContactId,
            name = "Carlos Santos",
            avatar = "carlos",
            lastMessage = "Fechado! Te vejo lá às 20h.",
            lastTimestamp = systemTime - 3600000 * 2,
            isGroup = false,
            unreadCount = 0,
            isAi = false
        )).toInt()

        val groupChatId = repository.insertChat(Chat(
            contactId = groupContactId,
            name = "Família Silva 👨‍👩‍👧‍👦",
            avatar = "group_fam",
            lastMessage = "Tio João: Bom dia a todos!",
            lastTimestamp = systemTime - 3600000 * 4,
            isGroup = true,
            unreadCount = 0,
            isAi = false
        )).toInt()

        // 3. Create Seed Messages
        // Gemini
        repository.insertMessage(Message(
            chatId = geminiChatId,
            senderId = geminiContactId,
            senderName = "Gemini AI Bot",
            text = "Olá! Eu sou o Gemini AI Bot integrado no seu WhatsApp. Sinta-se à vontade para me fazer perguntas de qualquer assunto! 🤖✨",
            timestamp = systemTime - 60000 * 5,
            isOutgoing = false
        ))

        // Mãe
        repository.insertMessage(Message(
            chatId = maeChatId,
            senderId = -1,
            senderName = "Você",
            text = "Oi mãe, tudo bem? Precisa de alguma coisa do mercado?",
            timestamp = systemTime - 60000 * 20,
            isOutgoing = true,
            isRead = true
        ))
        repository.insertMessage(Message(
            chatId = maeChatId,
            senderId = maeContactId,
            senderName = "Mãe ❤️",
            text = "Oi querido! Tudo bem sim. Filho, compra o pão integral por favor!",
            timestamp = systemTime - 60000 * 15,
            isOutgoing = false
        ))

        // Carlos
        repository.insertMessage(Message(
            chatId = carlosChatId,
            senderId = carlosContactId,
            senderName = "Carlos Santos",
            text = "Ei cara, vamos jogar bola hoje à noite?",
            timestamp = systemTime - 3600000 * 3,
            isOutgoing = false
        ))
        repository.insertMessage(Message(
            chatId = carlosChatId,
            senderId = -1,
            senderName = "Você",
            text = "Opa! Vamos sim. Que horas vai ser?",
            timestamp = systemTime - 3600000 * 2 - 300000,
            isOutgoing = true,
            isRead = true
        ))
        repository.insertMessage(Message(
            chatId = carlosChatId,
            senderId = carlosContactId,
            senderName = "Carlos Santos",
            text = "Fechado! Te vejo lá às 20h.",
            timestamp = systemTime - 3600000 * 2,
            isOutgoing = false
        ))

        // Family Group
        repository.insertMessage(Message(
            chatId = groupChatId,
            senderId = maeContactId,
            senderName = "Mãe ❤️",
            text = "Alguém viu os meus óculos de leitura?",
            timestamp = systemTime - 3600000 * 5,
            isOutgoing = false
        ))
        repository.insertMessage(Message(
            chatId = groupChatId,
            senderId = carlosContactId, // using as uncle for seed
            senderName = "Tio João",
            text = "Tio João: Bom dia a todos!",
            timestamp = systemTime - 3600000 * 4,
            isOutgoing = false
        ))

        // 4. Create Seed Status
        repository.insertStatusUpdate(StatusUpdate(
            contactId = maeContactId,
            contactName = "Mãe ❤️",
            contactAvatar = "mother",
            timestamp = systemTime - 3600000 * 1,
            textContent = "Cultive o amor nas pequenas coisas! 🌸🌻 Tenham um lindo dia!",
            isViewed = false
        ))

        repository.insertStatusUpdate(StatusUpdate(
            contactId = carlosContactId,
            contactName = "Carlos Santos",
            contactAvatar = "carlos",
            timestamp = systemTime - 3600000 * 3,
            textContent = "Café para aguentar o expediente de hoje ☕💻 #Work",
            isViewed = false
        ))

        // 5. Create Seed Calls
        repository.insertCallLog(CallLog(
            contactId = maeContactId,
            contactName = "Mãe ❤️",
            contactAvatar = "mother",
            timestamp = systemTime - 3600000 * 24,
            isVideo = false,
            isIncoming = true,
            isMissed = true
        ))

        repository.insertCallLog(CallLog(
            contactId = carlosContactId,
            contactName = "Carlos Santos",
            contactAvatar = "carlos",
            timestamp = systemTime - 3600000 * 48,
            isVideo = true,
            isIncoming = false,
            isMissed = false
        ))
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectChat(chatId: Int?) {
        _selectedChatId.value = chatId
        if (chatId != null) {
            // Clear unread counts
            viewModelScope.launch {
                val chatObj = repository.getChatById(chatId)
                if (chatObj != null && chatObj.unreadCount > 0) {
                    repository.updateChat(chatObj.copy(unreadCount = 0))
                }
            }
        }
    }

    fun clearQuotedMessage() {
        _quotedMessage.value = null
    }

    fun setQuotedMessage(message: Message) {
        _quotedMessage.value = message
    }

    // Messaging Logic
    fun sendMessage(text: String, isAudio: Boolean = false, audioDuration: Int = 0) {
        val chatId = _selectedChatId.value ?: return
        val currentChat = _selectedChat.value ?: return
        val systemTime = System.currentTimeMillis()

        viewModelScope.launch {
            // Create outgoing message
            val msg = Message(
                chatId = chatId,
                senderId = -1,
                senderName = "Você",
                text = if (isAudio) "🎤 Áudio ($audioDuration s)" else text,
                timestamp = systemTime,
                isOutgoing = true,
                isRead = false,
                isAudio = isAudio,
                audioDurationSec = audioDuration,
                quotedMessageText = _quotedMessage.value?.text
            )

            repository.insertMessage(msg)
            clearQuotedMessage()

            // Update Chat Last Message
            repository.updateChat(currentChat.copy(
                lastMessage = if (isAudio) "🎤 Mensagem de voz" else text,
                lastTimestamp = systemTime
            ))

            // Check if AI or regular auto-reply
            if (currentChat.isAi) {
                triggerAiReply(chatId, text)
            } else {
                triggerAutoReply(chatId, currentChat, text)
            }
        }
    }

    private fun triggerAiReply(chatId: Int, promptText: String) {
        viewModelScope.launch {
            _typingStates.value = _typingStates.value.toMutableMap().apply { put(chatId, true) }
            
            // Gather last message history for contextual memory
            val historyFlow = repository.getMessagesForChat(chatId).first()
            val chatHistory = historyFlow.map { Pair(it.text, it.isOutgoing) }

            // Call Gemini
            val replyText = GeminiClient.generateResponse(promptText, chatHistory)
            
            _typingStates.value = _typingStates.value.toMutableMap().apply { put(chatId, false) }

            val systemTime = System.currentTimeMillis()
            val replyMsg = Message(
                chatId = chatId,
                senderId = currentChatContactId(chatId),
                senderName = "Gemini AI Bot",
                text = replyText,
                timestamp = systemTime,
                isOutgoing = false
            )
            repository.insertMessage(replyMsg)

            val chatObj = repository.getChatById(chatId)
            if (chatObj != null) {
                repository.updateChat(chatObj.copy(
                    lastMessage = replyText,
                    lastTimestamp = systemTime,
                    unreadCount = if (_selectedChatId.value == chatId) 0 else chatObj.unreadCount + 1
                ))
            }
        }
    }

    private fun triggerAutoReply(chatId: Int, chat: Chat, userMsgText: String) {
        viewModelScope.launch {
            // Simulate reading delay
            delay(1000)
            _typingStates.value = _typingStates.value.toMutableMap().apply { put(chatId, true) }
            delay(1500)
            _typingStates.value = _typingStates.value.toMutableMap().apply { put(chatId, false) }

            val replyText = when {
                chat.isGroup -> {
                    val groupReplies = listOf(
                        "Mãe ❤️: Que legal, filho!",
                        "Mãe ❤️: Não esqueçam da reunião de família domingo!",
                        "Tio João: 👍 Parabéns pessoal!",
                        "Tio João: kkkkkkk muito bom!"
                    )
                    groupReplies.random()
                }
                chat.name.contains("Mãe") -> {
                    when {
                        userMsgText.contains("pão", ignoreCase = true) -> "Obrigada meu bem! Compra aquele integral de fatias."
                        userMsgText.contains("oi", ignoreCase = true) || userMsgText.contains("tudo bem", ignoreCase = true) -> "Oi querido! Tudo bem por aqui, e com você? Se agasalha que vai esfriar!"
                        else -> "Que ótimo querido! Nos vemos em casa. Beijos, se cuida."
                    }
                }
                else -> {
                    val defaultReplies = listOf(
                        "Opa, massa!",
                        "Estou no meio de uma tarefa aqui, já te retorno!",
                        "Show! Combinado então.",
                        "Não consegui ouvir o áudio direito agora, depois ouço 👍",
                        "Me passa os detalhes por favor."
                    )
                    defaultReplies.random()
                }
            }

            val systemTime = System.currentTimeMillis()
            val senderId = if (chat.isGroup) 0 else chat.contactId
            val senderName = if (chat.isGroup) replyText.substringBefore(":") else chat.name
            val textContent = if (chat.isGroup) replyText.substringAfter(": ") else replyText

            val replyMsg = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                text = textContent,
                timestamp = systemTime,
                isOutgoing = false
            )
            repository.insertMessage(replyMsg)

            val chatObj = repository.getChatById(chatId)
            if (chatObj != null) {
                repository.updateChat(chatObj.copy(
                    lastMessage = if (chat.isGroup) "$senderName: $textContent" else textContent,
                    lastTimestamp = systemTime,
                    unreadCount = if (_selectedChatId.value == chatId) 0 else chatObj.unreadCount + 1
                ))
            }
        }
    }

    private suspend fun currentChatContactId(chatId: Int): Int {
        return repository.getChatById(chatId)?.contactId ?: 0
    }

    // Audio Recording Simulations
    fun startRecordingAudio() {
        _isRecordingAudio.value = true
        _recordingDuration.value = 0
        viewModelScope.launch {
            while (_isRecordingAudio.value) {
                delay(1000)
                if (_isRecordingAudio.value) {
                    _recordingDuration.value += 1
                }
            }
        }
    }

    fun stopRecordingAudio(send: Boolean) {
        _isRecordingAudio.value = false
        val duration = _recordingDuration.value
        if (send && duration > 0) {
            sendMessage("", isAudio = true, audioDuration = duration)
        }
        _recordingDuration.value = 0
    }

    // Status Updates Management
    fun postStatus(text: String) {
        viewModelScope.launch {
            val systemTime = System.currentTimeMillis()
            val status = StatusUpdate(
                contactId = -1,
                contactName = "Meu Status",
                contactAvatar = "user_avatar",
                timestamp = systemTime,
                textContent = text,
                isViewed = true
            )
            repository.insertStatusUpdate(status)
        }
    }

    fun openStatusViewer(updates: List<StatusUpdate>, startIndex: Int) {
        _viewingStatusList.value = updates
        _currentStatusIndex.value = startIndex
        
        // Mark update as viewed
        viewModelScope.launch {
            val currentUpdate = updates[startIndex]
            if (!currentUpdate.isViewed && currentUpdate.id != 0) {
                repository.updateStatusUpdate(currentUpdate.copy(isViewed = true))
            }
        }
    }

    fun nextStatus() {
        val list = _viewingStatusList.value ?: return
        val nextIdx = _currentStatusIndex.value + 1
        if (nextIdx < list.size) {
            _currentStatusIndex.value = nextIdx
            // Mark as viewed
            viewModelScope.launch {
                val currentUpdate = list[nextIdx]
                if (!currentUpdate.isViewed) {
                    repository.updateStatusUpdate(currentUpdate.copy(isViewed = true))
                }
            }
        } else {
            closeStatusViewer()
        }
    }

    fun prevStatus() {
        val prevIdx = _currentStatusIndex.value - 1
        if (prevIdx >= 0) {
            _currentStatusIndex.value = prevIdx
        }
    }

    fun closeStatusViewer() {
        _viewingStatusList.value = null
        _currentStatusIndex.value = 0
    }

    // Calling Simulators
    fun startCall(contactId: Int, contactName: String, contactAvatar: String, isVideo: Boolean) {
        val state = CallState(
            contactId = contactId,
            name = contactName,
            avatar = contactAvatar,
            isVideo = isVideo,
            isIncoming = false,
            isConnected = false
        )
        _activeCallState.value = state

        // Simulated connection sequence
        viewModelScope.launch {
            // Ringing for 2.5 seconds
            delay(2500)
            val currentState = _activeCallState.value
            if (currentState != null && currentState.contactId == contactId) {
                // Connect call
                _activeCallState.value = currentState.copy(isConnected = true)
                
                // Track duration
                while (_activeCallState.value?.isConnected == true) {
                    delay(1000)
                    val active = _activeCallState.value
                    if (active != null) {
                        _activeCallState.value = active.copy(durationSec = active.durationSec + 1)
                    }
                }
            }
        }
    }

    fun endCall() {
        val state = _activeCallState.value ?: return
        viewModelScope.launch {
            // Add call log entry
            repository.insertCallLog(CallLog(
                contactId = state.contactId,
                contactName = state.name,
                contactAvatar = state.avatar,
                timestamp = System.currentTimeMillis(),
                isVideo = state.isVideo,
                isIncoming = state.isIncoming,
                isMissed = false
            ))
            _activeCallState.value = null
        }
    }

    fun rejectIncomingCall() {
        val state = _activeCallState.value ?: return
        viewModelScope.launch {
            repository.insertCallLog(CallLog(
                contactId = state.contactId,
                contactName = state.name,
                contactAvatar = state.avatar,
                timestamp = System.currentTimeMillis(),
                isVideo = state.isVideo,
                isIncoming = true,
                isMissed = true
            ))
            _activeCallState.value = null
        }
    }

    fun answerCall() {
        val state = _activeCallState.value ?: return
        _activeCallState.value = state.copy(isConnected = true)
        
        viewModelScope.launch {
            while (_activeCallState.value?.isConnected == true) {
                delay(1000)
                val active = _activeCallState.value
                if (active != null) {
                    _activeCallState.value = active.copy(durationSec = active.durationSec + 1)
                }
            }
        }
    }

    fun deleteChat(chatId: Int) {
        viewModelScope.launch {
            repository.deleteChatById(chatId)
            repository.deleteMessagesByChatId(chatId)
            if (_selectedChatId.value == chatId) {
                selectChat(null)
            }
        }
    }

    fun startNewChat(contact: Contact) {
        viewModelScope.launch {
            // Check if chat already exists
            val existingChat = repository.getChatByContactId(contact.id)
            if (existingChat != null) {
                selectChat(existingChat.id)
            } else {
                val newChatId = repository.insertChat(Chat(
                    contactId = contact.id,
                    name = contact.name,
                    avatar = contact.avatar,
                    lastMessage = "Iniciei uma nova conversa!",
                    lastTimestamp = System.currentTimeMillis(),
                    isGroup = contact.isGroup,
                    unreadCount = 0,
                    isAi = contact.isAi
                )).toInt()
                selectChat(newChatId)
            }
        }
    }

    fun createNewContactAndChat(name: String, status: String, isAi: Boolean) {
        viewModelScope.launch {
            val contactId = repository.insertContact(Contact(
                name = name,
                avatar = "user_placeholder",
                statusMessage = status,
                isGroup = false,
                isAi = isAi
            )).toInt()

            val chatId = repository.insertChat(Chat(
                contactId = contactId,
                name = name,
                avatar = "user_placeholder",
                lastMessage = "Olá! Vamos conversar.",
                lastTimestamp = System.currentTimeMillis(),
                isGroup = false,
                unreadCount = 0,
                isAi = isAi
            )).toInt()

            repository.insertMessage(Message(
                chatId = chatId,
                senderId = contactId,
                senderName = name,
                text = "Olá! Adicionei você no meu WhatsApp. Vamos conversar! 😊",
                timestamp = System.currentTimeMillis(),
                isOutgoing = false
            ))

            selectChat(chatId)
        }
    }

    fun clearAllCallHistory() {
        viewModelScope.launch {
            repository.clearCallLogs()
        }
    }

    // --- Firebase Synchronization Integration ---

    private val _isFirebaseReal = MutableStateFlow(false)
    val isFirebaseReal: StateFlow<Boolean> = _isFirebaseReal.asStateFlow()

    private val _firebaseSyncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val firebaseSyncStatus: StateFlow<SyncStatus> = _firebaseSyncStatus.asStateFlow()

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        data class Success(val count: Int, val isReal: Boolean) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    fun setFirebaseConnected(isReal: Boolean) {
        _isFirebaseReal.value = isReal
    }

    fun resetSyncStatus() {
        _firebaseSyncStatus.value = SyncStatus.Idle
    }

    fun syncToFirebase() {
        viewModelScope.launch {
            _firebaseSyncStatus.value = SyncStatus.Syncing
            delay(1500) // Beautiful visual feedback

            if (!_isFirebaseReal.value) {
                // Mock Simulation for Graceful fallback
                try {
                    val contactCount = contacts.value.size
                    val chatCount = chats.value.size
                    var messageCount = 0
                    for (chat in chats.value) {
                        messageCount += repository.getMessagesForChatOneShot(chat.id).size
                    }
                    _firebaseSyncStatus.value = SyncStatus.Success(
                        count = contactCount + chatCount + messageCount,
                        isReal = false
                    )
                } catch (e: Exception) {
                    _firebaseSyncStatus.value = SyncStatus.Error("Erro ao processar dados locais: ${e.localizedMessage}")
                }
            } else {
                // Real Firebase Cloud Sync using Firestore & Auth
                try {
                    val db = FirebaseFirestore.getInstance()
                    val auth = FirebaseAuth.getInstance()
                    
                    // Sign in anonymously if no user is authenticated
                    var userId = auth.currentUser?.uid
                    if (userId == null) {
                        val authResult = withContext(Dispatchers.IO) {
                            com.google.android.gms.tasks.Tasks.await(auth.signInAnonymously())
                        }
                        userId = authResult.user?.uid
                    }

                    if (userId == null) {
                        _firebaseSyncStatus.value = SyncStatus.Error("Não foi possível autenticar anonimamente com o Firebase.")
                        return@launch
                    }

                    val batch = db.batch()
                    var totalOperations = 0

                    // Sync Contacts
                    for (contact in contacts.value) {
                        val contactRef = db.collection("users").document(userId).collection("contacts").document(contact.id.toString())
                        val data = hashMapOf(
                            "id" to contact.id,
                            "name" to contact.name,
                            "avatar" to contact.avatar,
                            "statusMessage" to contact.statusMessage,
                            "isGroup" to contact.isGroup,
                            "isAi" to contact.isAi
                        )
                        batch.set(contactRef, data)
                        totalOperations++
                    }

                    // Sync Chats & Messages
                    for (chat in chats.value) {
                        val chatRef = db.collection("users").document(userId).collection("chats").document(chat.id.toString())
                        val data = hashMapOf(
                            "id" to chat.id,
                            "contactId" to chat.contactId,
                            "name" to chat.name,
                            "avatar" to chat.avatar,
                            "lastMessage" to chat.lastMessage,
                            "lastTimestamp" to chat.lastTimestamp,
                            "isGroup" to chat.isGroup,
                            "unreadCount" to chat.unreadCount,
                            "isAi" to chat.isAi
                        )
                        batch.set(chatRef, data)
                        totalOperations++

                        val chatMessages = repository.getMessagesForChatOneShot(chat.id)
                        for (msg in chatMessages) {
                            val msgRef = db.collection("users").document(userId).collection("chats").document(chat.id.toString())
                                .collection("messages").document(msg.id.toString())
                            val msgData = hashMapOf(
                                "id" to msg.id,
                                "chatId" to msg.chatId,
                                "senderId" to msg.senderId,
                                "senderName" to msg.senderName,
                                "text" to msg.text,
                                "timestamp" to msg.timestamp,
                                "isOutgoing" to msg.isOutgoing
                            )
                            batch.set(msgRef, msgData)
                            totalOperations++
                        }
                    }

                    withContext(Dispatchers.IO) {
                        com.google.android.gms.tasks.Tasks.await(batch.commit())
                    }

                    _firebaseSyncStatus.value = SyncStatus.Success(count = totalOperations, isReal = true)
                } catch (e: Exception) {
                    _firebaseSyncStatus.value = SyncStatus.Error("Erro na sincronização do Firebase: ${e.localizedMessage}")
                }
            }
        }
    }
}
