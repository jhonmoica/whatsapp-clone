package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    chat: Chat,
    messages: List<Message>,
    isTyping: Boolean,
    isDarkMode: Boolean,
    quotedMessage: Message?,
    isRecordingAudio: Boolean,
    recordingDuration: Int,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onDeleteChat: () -> Unit,
    onSetQuotedMessage: (Message) -> Unit,
    onClearQuotedMessage: () -> Unit,
    onStartRecordingAudio: () -> Unit,
    onStopRecordingAudio: (send: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var messageToReactTo by remember { mutableStateOf<Message?>(null) }
    val reactionsState = remember { mutableStateMapOf<Int, String>() }

    // Scroll to bottom when new messages arrive or typing status changes
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    // Modern WhatsApp wallpaper pattern drawn procedurally
    val wallpaperModifier = Modifier.drawBehind {
        val spacing = 50.dp.toPx()
        val color = if (isDarkMode) Color(0xFF070B0E) else Color(0xFFEFEAE2)
        drawRect(color)

        // Draw cute little WhatsApp-like doodle crosses and dots softly
        val softDoodleColor = if (isDarkMode) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f)
        for (x in 0..size.width.toInt() step spacing.toInt()) {
            for (y in 0..size.height.toInt() step spacing.toInt()) {
                val alternate = (x + y) % 3 == 0
                if (alternate) {
                    drawCircle(softDoodleColor, 2.dp.toPx(), Offset(x.toFloat(), y.toFloat()))
                } else {
                    drawLine(
                        color = softDoodleColor,
                        start = Offset(x.toFloat() - 4.dp.toPx(), y.toFloat()),
                        end = Offset(x.toFloat() + 4.dp.toPx(), y.toFloat()),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = softDoodleColor,
                        start = Offset(x.toFloat(), y.toFloat() - 4.dp.toPx()),
                        end = Offset(x.toFloat(), y.toFloat() + 4.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) WaDarkSurface else WaGreenDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AvatarView(
                            avatarKey = chat.avatar,
                            name = chat.name,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = chat.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isTyping) "digitando..." else if (chat.isAi) "Online (Gemini)" else "Online",
                                fontSize = 12.sp,
                                color = if (isTyping) Color(0xFF25D366) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onStartCall(true) }) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Chamada de Vídeo")
                    }
                    IconButton(onClick = { onStartCall(false) }) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Chamada de Voz")
                    }
                    
                    var showDropdownMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Mais opções")
                    }
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false },
                        modifier = Modifier.background(if (isDarkMode) WaDarkSurface else Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Limpar Conversa", color = if (isDarkMode) Color.White else Color.Black) },
                            onClick = {
                                onDeleteChat()
                                showDropdownMenu = false
                            }
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(wallpaperModifier)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        val hasReaction = reactionsState[message.id]
                        
                        MessageBubble(
                            message = message,
                            isDarkMode = isDarkMode,
                            reaction = hasReaction,
                            onLongPress = {
                                messageToReactTo = message
                            },
                            onDoubleTap = {
                                reactionsState[message.id] = "❤️"
                            },
                            onQuoteReply = {
                                onSetQuotedMessage(message)
                            }
                        )
                    }

                    if (isTyping) {
                        item {
                            TypingBubble(chatName = chat.name, isDarkMode = isDarkMode)
                        }
                    }
                }

                // Quoted reply bar preview
                if (quotedMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDarkMode) WaDarkSurface else Color.White)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF00A884))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (quotedMessage.isOutgoing) "Você" else chat.name,
                                color = Color(0xFF00A884),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = quotedMessage.text,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onClearQuotedMessage) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar")
                        }
                    }
                }

                // Chat Input Bar / Audio Recording State
                BottomInputBar(
                    text = textState,
                    isDarkMode = isDarkMode,
                    isRecordingAudio = isRecordingAudio,
                    recordingDuration = recordingDuration,
                    onTextChanged = { textState = it },
                    onSend = {
                        onSendMessage(textState)
                        textState = ""
                    },
                    onStartRecording = onStartRecordingAudio,
                    onStopRecording = onStopRecordingAudio,
                    onAttachmentClick = { showAttachmentDialog = true },
                    onEmojiClick = { showEmojiDialog = !showEmojiDialog }
                )
            }

            // Quick Reaction Overlay
            if (messageToReactTo != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { messageToReactTo = null },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) WaDarkSurface else Color.White
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier
                                        .clickable {
                                            messageToReactTo?.let { msg ->
                                                reactionsState[msg.id] = emoji
                                            }
                                            messageToReactTo = null
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Mock Attachments Dialog
            if (showAttachmentDialog) {
                AlertDialog(
                    onDismissRequest = { showAttachmentDialog = false },
                    confirmButton = {},
                    title = { Text("Enviar", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            val attachmentItems = listOf(
                                Pair(Icons.Default.Description, "Documento"),
                                Pair(Icons.Default.CameraAlt, "Câmera"),
                                Pair(Icons.Default.Image, "Galeria"),
                                Pair(Icons.Default.AudioFile, "Áudio"),
                                Pair(Icons.Default.LocationOn, "Localização"),
                                Pair(Icons.Default.ContactPage, "Contato")
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                attachmentItems.take(3).forEach { item ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                showAttachmentDialog = false
                                                onSendMessage("📎 Enviei um arquivo (${item.second})")
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00A884)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = item.first, contentDescription = null, tint = Color.White)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.second, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                attachmentItems.takeLast(3).forEach { item ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                showAttachmentDialog = false
                                                onSendMessage("📎 Enviei um arquivo (${item.second})")
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF075E54)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = item.first, contentDescription = null, tint = Color.White)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.second, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // Mock Emoji Dialog
            if (showEmojiDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showEmojiDialog = false }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) WaDarkSurface else Color.White
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { /* swallow taps */ }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Emojis Recentes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            val emojis = listOf(
                                "😀", "😂", "🥰", "😍", "😘", "😜", "😎", "🤔", "🙄", "🔥",
                                "👍", "🙏", "❤️", "🎉", "💩", "👀", "🚀", "✨", "🤖", "🍕"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                emojis.take(10).forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.clickable {
                                            textState += emoji
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                emojis.takeLast(10).forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.clickable {
                                            textState += emoji
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isDarkMode: Boolean,
    reaction: String?,
    onLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    onQuoteReply: () -> Unit,
) {
    val bubbleColor = when {
        message.isOutgoing -> if (isDarkMode) WaGreenDarkBubble else WaGreenLight
        else -> if (isDarkMode) WaDarkIncomingBubble else Color.White
    }

    val textColor = if (isDarkMode) Color.White else Color.Black
    val timeColor = if (message.isOutgoing && !isDarkMode) Color(0xFF5E6D77) else GrayText

    val alignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isOutgoing) {
        RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)
    } else {
        RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Column(horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start) {
            Surface(
                color = bubbleColor,
                shape = shape,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onLongClick = onLongPress,
                        onDoubleClick = onDoubleTap,
                        onClick = onQuoteReply
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    // Render quoted reply message inside the bubble if present
                    if (message.quotedMessageText != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.08f))
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF00A884))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.quotedMessageText,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (message.isAudio) {
                        AudioBubbleContent(duration = message.audioDurationSec, isDarkMode = isDarkMode)
                    } else {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                        Text(
                            text = timeString,
                            color = timeColor,
                            fontSize = 10.sp
                        )
                        if (message.isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = null,
                                tint = if (message.isRead) BlueCheck else timeColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Mini Reaction Badge overlapping the bubble bottom
            if (reaction != null) {
                Box(
                    modifier = Modifier
                        .offset(y = (-6).dp, x = if (message.isOutgoing) (-12).dp else 12.dp)
                        .background(if (isDarkMode) WaDarkIncomingBubble else Color.White, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = reaction, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AudioBubbleContent(duration: Int, isDarkMode: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    var playProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            playProgress = 0f
            while (playProgress < 1f && isPlaying) {
                delay(100)
                playProgress += 0.1f / duration
            }
            isPlaying = false
            playProgress = 0f
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isDarkMode) Color.White else Color(0xFF075E54)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Simulated audio waveform
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = 3.dp.toPx()
                val gap = 2.dp.toPx()
                val totalBars = (size.width / (barWidth + gap)).toInt()
                
                // Deterministic height values for wave
                val randomHeights = listOf(0.4f, 0.7f, 0.5f, 0.8f, 0.3f, 0.9f, 0.4f, 0.6f, 0.8f, 0.5f, 0.7f, 0.4f, 0.9f, 0.3f, 0.6f, 0.8f, 0.4f, 0.5f, 0.7f, 0.6f)

                for (i in 0 until totalBars) {
                    val index = i % randomHeights.size
                    val barHeightFraction = randomHeights[index]
                    val currentProgressFraction = i.toFloat() / totalBars
                    
                    val color = if (currentProgressFraction <= playProgress) {
                        BlueCheck
                    } else if (isDarkMode) {
                        Color.White.copy(alpha = 0.3f)
                    } else {
                        Color.Black.copy(alpha = 0.25f)
                    }

                    val x = i * (barWidth + gap)
                    val h = size.height * barHeightFraction
                    val y = (size.height - h) / 2

                    drawRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, h)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        AvatarView(
            avatarKey = "carlos", // Micro portrait mock inside voice note
            name = "Voz",
            size = 28.dp
        )
    }
}

@Composable
fun TypingBubble(chatName: String, isDarkMode: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 0), RepeatMode.Reverse), label = "dot1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "dot2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = if (isDarkMode) WaDarkIncomingBubble else Color.White,
            shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 0.dp),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("digitando", color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f), fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(Color.Gray.copy(alpha = dotAlpha1)))
                    Box(Modifier.size(4.dp).clip(CircleShape).background(Color.Gray.copy(alpha = dotAlpha2)))
                    Box(Modifier.size(4.dp).clip(CircleShape).background(Color.Gray.copy(alpha = dotAlpha3)))
                }
            }
        }
    }
}

@Composable
fun BottomInputBar(
    text: String,
    isDarkMode: Boolean,
    isRecordingAudio: Boolean,
    recordingDuration: Int,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: (send: Boolean) -> Unit,
    onAttachmentClick: () -> Unit,
    onEmojiClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRecordingAudio) {
            // Audio recording layout active
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDarkMode) WaDarkSurface else Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing red recording dot
                val infiniteTransition = rememberInfiniteTransition(label = "recording")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f, targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "dot"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                val minutes = recordingDuration / 60
                val seconds = recordingDuration % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Arraste para cancelar",
                    color = GrayText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onStopRecording(false) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Deletar", tint = Color.Red)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Green mic click to send
            IconButton(
                onClick = { onStopRecording(true) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00A884))
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Enviar Áudio", tint = Color.White)
            }
        } else {
            // Regular input field layout
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDarkMode) WaDarkSurface else Color.White)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEmojiClick) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emojis",
                        tint = GrayText
                    )
                }

                TextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = { Text("Mensagem", color = GrayText, fontSize = 15.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                        unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onAttachmentClick) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Anexar",
                        tint = GrayText
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val hasText = text.isNotBlank()
            IconButton(
                onClick = {
                    if (hasText) {
                        onSend()
                    } else {
                        onStartRecording()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00A884))
            ) {
                Icon(
                    imageVector = if (hasText) Icons.Default.Send else Icons.Default.Mic,
                    contentDescription = if (hasText) "Enviar" else "Gravar",
                    tint = Color.White
                )
            }
        }
    }
}
