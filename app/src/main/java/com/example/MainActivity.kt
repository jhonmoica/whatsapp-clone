package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WhatsAppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WhatsAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase gracefully
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            viewModel.setFirebaseConnected(true)
        } catch (e: Exception) {
            // Graceful fallback if google-services.json is missing
            try {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:abcdef123456")
                    .setApiKey("mock-api-key-for-firebase-initialization-graceful-fallback")
                    .setDatabaseUrl("https://mock-whatsapp-clone.firebaseio.com")
                    .setProjectId("mock-whatsapp-clone")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
                viewModel.setFirebaseConnected(false) // Connected via mock fallback
            } catch (fallbackEx: Exception) {
                android.util.Log.e("MainActivity", "Failed to initialize Firebase fallback: ${fallbackEx.localizedMessage}")
            }
        }

        // Handle Back Buttons elegantly
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewModel.activeCallState.value != null -> {
                        // End or reject call on back
                        viewModel.endCall()
                    }
                    viewModel.viewingStatusList.value != null -> {
                        viewModel.closeStatusViewer()
                    }
                    viewModel.selectedChatId.value != null -> {
                        viewModel.selectChat(null)
                    }
                    else -> {
                        // Default system back action
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    WhatsAppAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun WhatsAppAppContent(viewModel: WhatsAppViewModel) {
    val chats by viewModel.chats.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val statusUpdates by viewModel.statusUpdates.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    
    val selectedChatId by viewModel.selectedChatId.collectAsState()
    val selectedChat by viewModel.selectedChat.collectAsState()
    val currentMessages by viewModel.currentMessages.collectAsState()
    
    val typingStates by viewModel.typingStates.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val quotedMessage by viewModel.quotedMessage.collectAsState()
    val isRecordingAudio by viewModel.isRecordingAudio.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    
    val viewingStatusList by viewModel.viewingStatusList.collectAsState()
    val currentStatusIndex by viewModel.currentStatusIndex.collectAsState()
    
    val activeCallState by viewModel.activeCallState.collectAsState()
    val isFirebaseReal by viewModel.isFirebaseReal.collectAsState()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsState()

    // Main layout switching
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            selectedChatId != null && selectedChat != null -> {
                val chat = selectedChat!!
                val isTyping = typingStates[chat.id] ?: false

                ChatDetailScreen(
                    chat = chat,
                    messages = currentMessages,
                    isTyping = isTyping,
                    isDarkMode = isDarkMode,
                    quotedMessage = quotedMessage,
                    isRecordingAudio = isRecordingAudio,
                    recordingDuration = recordingDuration,
                    onBack = { viewModel.selectChat(null) },
                    onSendMessage = { text -> viewModel.sendMessage(text) },
                    onStartCall = { isVideo ->
                        viewModel.startCall(
                            contactId = chat.contactId,
                            contactName = chat.name,
                            contactAvatar = chat.avatar,
                            isVideo = isVideo
                        )
                    },
                    onDeleteChat = { viewModel.deleteChat(chat.id) },
                    onSetQuotedMessage = { msg -> viewModel.setQuotedMessage(msg) },
                    onClearQuotedMessage = { viewModel.clearQuotedMessage() },
                    onStartRecordingAudio = { viewModel.startRecordingAudio() },
                    onStopRecordingAudio = { send -> viewModel.stopRecordingAudio(send) }
                )
            }
            else -> {
                HomeScreen(
                    chats = chats,
                    contacts = contacts,
                    statusUpdates = statusUpdates,
                    callLogs = callLogs,
                    searchQuery = searchQuery,
                    isDarkMode = isDarkMode,
                    isFirebaseReal = isFirebaseReal,
                    syncStatus = firebaseSyncStatus,
                    onSearchChanged = { q -> viewModel.updateSearchQuery(q) },
                    onChatSelected = { id -> viewModel.selectChat(id) },
                    onStartCall = { contact, isVideo ->
                        viewModel.startCall(
                            contactId = contact.id,
                            contactName = contact.name,
                            contactAvatar = contact.avatar,
                            isVideo = isVideo
                        )
                    },
                    onDeleteChat = { id -> viewModel.deleteChat(id) },
                    onAddNewContact = { name, status, isAi -> 
                        viewModel.createNewContactAndChat(name, status, isAi)
                    },
                    onToggleTheme = { viewModel.toggleTheme() },
                    onOpenStatus = { list, index -> viewModel.openStatusViewer(list, index) },
                    onPostStatus = { text -> viewModel.postStatus(text) },
                    onClearCallHistory = { viewModel.clearAllCallHistory() },
                    onSyncToFirebase = { viewModel.syncToFirebase() },
                    onResetSyncStatus = { viewModel.resetSyncStatus() }
                )
            }
        }

        // Overlay status stories fullscreen when active
        AnimatedVisibility(
            visible = viewingStatusList != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (viewingStatusList != null) {
                StatusViewScreen(
                    statusUpdates = viewingStatusList!!,
                    currentIndex = currentStatusIndex,
                    onNext = { viewModel.nextStatus() },
                    onPrev = { viewModel.prevStatus() },
                    onClose = { viewModel.closeStatusViewer() },
                    onReplySent = { replyText ->
                        // Send status reply to the contact chat thread
                        val update = viewingStatusList!![currentStatusIndex]
                        viewModel.closeStatusViewer()
                        
                        // Find or open chat to send reply
                        val matchedContact = contacts.firstOrNull { it.name == update.contactName }
                        if (matchedContact != null) {
                            viewModel.startNewChat(matchedContact)
                            viewModel.sendMessage("Respondendo ao status: \"${update.textContent}\"\n\n$replyText")
                        }
                    }
                )
            }
        }

        // Overlay active calls fullscreen when active
        AnimatedVisibility(
            visible = activeCallState != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (activeCallState != null) {
                CallActiveScreen(
                    callState = activeCallState!!,
                    onAnswer = { viewModel.answerCall() },
                    onReject = { viewModel.rejectIncomingCall() },
                    onHangUp = { viewModel.endCall() }
                )
            }
        }
    }
}
