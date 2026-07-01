package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.WhatsAppViewModel.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    chats: List<Chat>,
    contacts: List<Contact>,
    statusUpdates: List<StatusUpdate>,
    callLogs: List<CallLog>,
    searchQuery: String,
    isDarkMode: Boolean,
    isFirebaseReal: Boolean,
    syncStatus: SyncStatus,
    userEmail: String?,
    onLogout: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onChatSelected: (Int) -> Unit,
    onStartCall: (Contact, isVideo: Boolean) -> Unit,
    onDeleteChat: (Int) -> Unit,
    onAddNewContact: (name: String, status: String, isAi: Boolean) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenStatus: (updates: List<StatusUpdate>, index: Int) -> Unit,
    onPostStatus: (String) -> Unit,
    onClearCallHistory: () -> Unit,
    onSyncToFirebase: () -> Unit,
    onResetSyncStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Chats, 1: Status, 2: Calls
    var isSearching by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showContactsSheet by remember { mutableStateOf(false) }
    var showPostStatusDialog by remember { mutableStateOf(false) }
    var showFirebaseSyncDialog by remember { mutableStateOf(false) }

    // Filter lists based on search queries
    val filteredChats = chats.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    val filteredContacts = contacts.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(if (isDarkMode) WaDarkSurface else WaGreenDark)
            ) {
                // Top header bar
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchChanged,
                                placeholder = { Text("Pesquisar...", color = Color.White.copy(alpha = 0.6f)) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearching = !isSearching
                            if (!isSearching) onSearchChanged("")
                        }) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Pesquisar"
                            )
                        }

                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Alternar Tema"
                            )
                        }

                        IconButton(onClick = { showAddContactDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Adicionar Contato"
                            )
                        }

                        IconButton(onClick = { showFirebaseSyncDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Sincronizar Cloud"
                            )
                        }

                        var showMenu by remember { mutableStateOf(false) }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Mais opções"
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (userEmail != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "Conectado como:",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = userEmail,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {},
                                        enabled = false
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Sair da Conta") },
                                    onClick = {
                                        showMenu = false
                                        onLogout()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                )

                // Navigation Tabs Custom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("CONVERSAS", "STATUS", "CHAMADAS")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = activeTab == index
                        val borderCol = if (isSelected) Color(0xFF00A884) else Color.Transparent
                        val textCol = if (isSelected) Color(0xFF00A884) else Color.White.copy(alpha = 0.7f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = index }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = title,
                                color = textCol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(3.dp)
                                    .background(borderCol, RoundedCornerShape(100))
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (activeTab) {
                        0 -> showContactsSheet = true
                        1 -> showPostStatusDialog = true
                        2 -> showContactsSheet = true
                    }
                },
                containerColor = Color(0xFF00A884),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                val icon = when (activeTab) {
                    0 -> Icons.Default.Chat
                    1 -> Icons.Default.Edit
                    else -> Icons.Default.AddIcCall
                }
                Icon(imageVector = icon, contentDescription = "Ação FAB")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> ChatsTab(
                    chats = filteredChats,
                    isDarkMode = isDarkMode,
                    onChatSelected = onChatSelected,
                    onDeleteChat = onDeleteChat
                )
                1 -> StatusTab(
                    statusUpdates = statusUpdates,
                    onOpenStatus = onOpenStatus,
                    onPostStatusClick = { showPostStatusDialog = true }
                )
                2 -> CallsTab(
                    callLogs = callLogs,
                    isDarkMode = isDarkMode,
                    onClearHistory = onClearCallHistory,
                    onCallClick = { log ->
                        val contact = contacts.firstOrNull { it.id == log.contactId }
                        if (contact != null) {
                            onStartCall(contact, log.isVideo)
                        }
                    }
                )
            }
        }

        // Dialog for adding new custom contact
        if (showAddContactDialog) {
            var newName by remember { mutableStateOf("") }
            var newStatus by remember { mutableStateOf("") }
            var isAiBot by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddContactDialog = false },
                title = { Text("Novo Contato / Bot IA") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Nome do contato") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newStatus,
                            onValueChange = { newStatus = it },
                            label = { Text("Status (Recado)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = isAiBot, onCheckedChange = { isAiBot = it })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Integrar com Inteligência Artificial (Gemini)")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onAddNewContact(
                                    newName,
                                    if (newStatus.isBlank()) "Olá, estou usando o WhatsApp Clone!" else newStatus,
                                    isAiBot
                                )
                                showAddContactDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                    ) {
                        Text("Criar e Conversar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddContactDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        // Dialog for Firebase Cloud Synchronization
        if (showFirebaseSyncDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (syncStatus != SyncStatus.Syncing) {
                        showFirebaseSyncDialog = false
                        onResetSyncStatus()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Cloud Icon",
                        tint = Color(0xFF00A884),
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = "Sincronização Firebase",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (syncStatus) {
                            is SyncStatus.Idle -> {
                                // Connection Mode Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isFirebaseReal) Color(0xFF1B5E20).copy(alpha = 0.15f)
                                            else Color(0xFFE65100).copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isFirebaseReal) Color(0xFF2E7D32) else Color(0xFFEF6C00))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isFirebaseReal) "Firebase Conectado (Nuvem)" else "Modo de Teste / Simulação",
                                        color = if (isFirebaseReal) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = if (isFirebaseReal) {
                                        "Deseja fazer o backup das suas conversas e contatos locais na nuvem do Firebase Firestore de forma segura?"
                                    } else {
                                        "Você está no modo de simulação local pois não há arquivo 'google-services.json' configurado. Você pode testar e simular o fluxo de sincronização agora."
                                    },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                // Data Summary
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Dados locais a enviar:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text("👥 ${contacts.size} Contatos", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        Text("💬 ${chats.size} Conversas", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    }
                                }
                            }
                            is SyncStatus.Syncing -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF00A884))
                                        Text(
                                            "Salvando dados no Firebase...",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            is SyncStatus.Success -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "Backup Realizado!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = if (syncStatus.isReal) {
                                                "Sincronização real concluída! ${syncStatus.count} registros salvos de forma segura no Firebase Firestore."
                                            } else {
                                                "Sincronização simulada executada com sucesso! Para habilitar o salvamento em tempo real em servidores, adicione o arquivo google-services.json."
                                            },
                                            fontSize = 13.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            is SyncStatus.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "Falha na Sincronização",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = syncStatus.message,
                                            fontSize = 13.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    when (syncStatus) {
                        is SyncStatus.Idle -> {
                            Button(
                                onClick = onSyncToFirebase,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                            ) {
                                Text("Sincronizar")
                            }
                        }
                        is SyncStatus.Syncing -> {
                            // No buttons during sync
                        }
                        is SyncStatus.Success, is SyncStatus.Error -> {
                            Button(
                                onClick = {
                                    showFirebaseSyncDialog = false
                                    onResetSyncStatus()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                            ) {
                                Text("Fechar")
                            }
                        }
                    }
                },
                dismissButton = {
                    if (syncStatus is SyncStatus.Idle) {
                        TextButton(
                            onClick = {
                                showFirebaseSyncDialog = false
                                onResetSyncStatus()
                            }
                        ) {
                            Text("Cancelar", color = Color.Gray)
                        }
                    }
                }
            )
        }

        // Bottomsheet Dialog for Contacts List Selector
        if (showContactsSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showContactsSheet = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .clickable { /* swallow click */ },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Selecione um contato", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            IconButton(onClick = { showContactsSheet = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredContacts) { contact ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showContactsSheet = false
                                            if (activeTab == 2) {
                                                onStartCall(contact, false)
                                            } else {
                                                // Initiate chat
                                                onChatSelected(contact.id)
                                            }
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    AvatarView(
                                        avatarKey = contact.avatar,
                                        name = contact.name,
                                        size = 44.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(contact.statusMessage, color = GrayText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (activeTab == 2) {
                                        Row {
                                            IconButton(onClick = {
                                                showContactsSheet = false
                                                onStartCall(contact, false)
                                            }) {
                                                Icon(imageVector = Icons.Default.Call, contentDescription = "Voz", tint = Color(0xFF00A884))
                                            }
                                            IconButton(onClick = {
                                                showContactsSheet = false
                                                onStartCall(contact, true)
                                            }) {
                                                Icon(imageVector = Icons.Default.Videocam, contentDescription = "Vídeo", tint = Color(0xFF00A884))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog to type a text status
        if (showPostStatusDialog) {
            var statusText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showPostStatusDialog = false },
                title = { Text("Meu Novo Status") },
                text = {
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { statusText = it },
                        placeholder = { Text("No que você está pensando hoje?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (statusText.isNotBlank()) {
                                onPostStatus(statusText)
                                showPostStatusDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884))
                    ) {
                        Text("Postar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPostStatusDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun ChatsTab(
    chats: List<Chat>,
    isDarkMode: Boolean,
    onChatSelected: (Int) -> Unit,
    onDeleteChat: (Int) -> Unit
) {
    if (chats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = GrayText
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Nenhuma conversa ativa", fontWeight = FontWeight.Bold, color = GrayText)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Toque no botão verde abaixo para iniciar!", color = GrayText, fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(chats) { chat ->
                var showDeleteConfirm by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChatSelected(chat.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    AvatarView(
                        avatarKey = chat.avatar,
                        name = chat.name,
                        size = 52.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chat.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = chat.lastMessage,
                            color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onBackground else GrayText,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.lastTimestamp))
                        Text(
                            text = timeString,
                            color = if (chat.unreadCount > 0) Color(0xFF00A884) else GrayText,
                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (chat.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00A884)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Deletar conversa",
                                    tint = GrayText.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Deletar Conversa?") },
                        text = { Text("Isso apagará permanentemente todo o histórico de mensagens com ${chat.name}.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    onDeleteChat(chat.id)
                                    showDeleteConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Apagar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("Cancelar", color = Color.Gray)
                            }
                        }
                    )
                }

                Divider(
                    color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                    modifier = Modifier.padding(start = 84.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
fun StatusTab(
    statusUpdates: List<StatusUpdate>,
    onOpenStatus: (List<StatusUpdate>, Int) -> Unit,
    onPostStatusClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // "Meu Status" card at the top
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPostStatusClick() }
                .padding(16.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AvatarView(
                    avatarKey = "user_avatar",
                    name = "Meu Status",
                    size = 50.dp
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00A884))
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Meu Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Toque para adicionar uma atualização de texto", color = GrayText, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Atualizações recentes", color = Color(0xFF00A884), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        if (statusUpdates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum status recente", color = GrayText, fontSize = 14.sp)
            }
        } else {
            // Horizontal row for status circles (beautiful layout as per design rules)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(statusUpdates.size) { index ->
                    val update = statusUpdates[index]
                    val ringColor = if (update.isViewed) GrayText else Color(0xFF00A884)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onOpenStatus(statusUpdates, index)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(2.5.dp, ringColor, CircleShape)
                                .padding(4.dp)
                        ) {
                            AvatarView(
                                avatarKey = update.contactAvatar,
                                name = update.contactName,
                                size = 48.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = update.contactName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }
            }

            Divider()

            // Vertical list of updates for easier reading
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(statusUpdates.size) { index ->
                    val update = statusUpdates[index]
                    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(update.timestamp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenStatus(statusUpdates, index) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        AvatarView(
                            avatarKey = update.contactAvatar,
                            name = update.contactName,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(update.contactName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Hoje • $timeString", color = GrayText, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallsTab(
    callLogs: List<CallLog>,
    isDarkMode: Boolean,
    onClearHistory: () -> Unit,
    onCallClick: (CallLog) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Histórico de Ligações", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GrayText)
            if (callLogs.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Limpar tudo", color = Color.Red, fontSize = 14.sp)
                }
            }
        }

        if (callLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(56.dp), tint = GrayText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nenhuma ligação recente", color = GrayText, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(callLogs) { log ->
                    val timeString = SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCallClick(log) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        AvatarView(
                            avatarKey = log.contactAvatar,
                            name = log.contactName,
                            size = 48.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.contactName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val arrowIcon = when {
                                    log.isIncoming && log.isMissed -> Icons.Default.CallMissed
                                    log.isIncoming -> Icons.Default.CallReceived
                                    else -> Icons.Default.CallMade
                                }
                                val arrowColor = when {
                                    log.isIncoming && log.isMissed -> RedMissedCall
                                    log.isIncoming -> Color(0xFF00A884)
                                    else -> BlueCheck
                                }
                                Icon(
                                    imageVector = arrowIcon,
                                    contentDescription = null,
                                    tint = arrowColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(timeString, color = GrayText, fontSize = 13.sp)
                            }
                        }

                        IconButton(onClick = { onCallClick(log) }) {
                            Icon(
                                imageVector = if (log.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Ligar",
                                tint = Color(0xFF00A884)
                            )
                        }
                    }

                    Divider(
                        color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                        modifier = Modifier.padding(start = 80.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}
