package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.WhatsAppViewModel.AuthStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authStatus: AuthStatus,
    isFirebaseReal: Boolean,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit,
    onSkipAuth: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Login, 1: Register
    val focusManager = LocalFocusManager.current

    // Form states
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation state
    var localError by remember { mutableStateOf<String?>(null) }

    // Clear local error when switching tabs
    LaunchedEffect(selectedTab) {
        localError = null
        onClearError()
    }

    val brandColor = Color(0xFF00A884)
    val darkBg = Color(0xFF121B22)
    val cardBg = Color(0xFF1F2C34)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(brandColor.copy(alpha = 0.15f), darkBg, darkBg)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // WhatsApp-style Logo Branding
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(brandColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "WhatsApp Logo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "WhatsApp Clone",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sincronize suas conversas e contatos em tempo real usando Firebase Cloud.",
                fontSize = 14.sp,
                color = Color.LightGray.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Firebase Connection Mode Indicator Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFirebaseReal) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE65100).copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isFirebaseReal) Color(0xFF2E7D32) else Color(0xFFEF6C00))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFirebaseReal) "Firebase Cloud Ativo" else "Modo de Simulação Local",
                    color = if (isFirebaseReal) Color(0xFF81C784) else Color(0xFFFFB74D),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }

            // Main Auth Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tab Header (Login vs Register)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = brandColor,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = brandColor
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.testTag("tab_login")
                        ) {
                            Text(
                                text = "Entrar",
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) brandColor else Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.testTag("tab_register")
                        ) {
                            Text(
                                text = "Cadastrar",
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) brandColor else Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Local Error Box
                    val displayError = localError ?: (authStatus as? AuthStatus.Error)?.message
                    AnimatedVisibility(
                        visible = displayError != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        if (displayError != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Erro",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = displayError,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Fields switching based on active tab
                    if (selectedTab == 0) {
                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandColor,
                                focusedLabelColor = brandColor
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Alternar Visibilidade"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (email.isBlank() || password.isBlank()) {
                                        if (!isFirebaseReal) {
                                            onLogin("usuario@whatsapp.com", "123456")
                                        } else {
                                            localError = "Por favor, preencha todos os campos."
                                        }
                                    } else {
                                        onLogin(email.trim(), password.trim())
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandColor,
                                focusedLabelColor = brandColor
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (email.isBlank() || password.isBlank()) {
                                    if (!isFirebaseReal) {
                                        onLogin("usuario@whatsapp.com", "123456")
                                    } else {
                                        localError = "Por favor, preencha todos os campos."
                                    }
                                } else {
                                    onLogin(email.trim(), password.trim())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            if (authStatus is AuthStatus.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    } else {
                        // Register Fields

                        // Name Field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome completo") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandColor,
                                focusedLabelColor = brandColor
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandColor,
                                focusedLabelColor = brandColor
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha (mínimo 6 caracteres)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Alternar Visibilidade"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandColor,
                                focusedLabelColor = brandColor
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password Field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmar Senha") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Alternar Visibilidade"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    when {
                                        name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                                            if (!isFirebaseReal) {
                                                onSignUp(
                                                    if (name.isBlank()) "Novo Usuário" else name.trim(),
                                                    if (email.isBlank()) "usuario@whatsapp.com" else email.trim(),
                                                    if (password.isBlank()) "123456" else password.trim()
                                                )
                                            } else {
                                                localError = "Por favor, preencha todos os campos."
                                            }
                                        }
                                        password.length < 6 -> {
                                            if (!isFirebaseReal) {
                                                onSignUp(name.trim(), email.trim(), password.trim())
                                            } else {
                                                localError = "A senha deve ter pelo menos 6 caracteres."
                                            }
                                        }
                                        password != confirmPassword -> {
                                            if (!isFirebaseReal) {
                                                onSignUp(name.trim(), email.trim(), password.trim())
                                            } else {
                                                localError = "As senhas não conferem."
                                            }
                                        }
                                        else -> {
                                            onSignUp(name.trim(), email.trim(), password.trim())
                                        }
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = brandColor,
                                focusedLabelColor = brandColor
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Register Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                when {
                                    name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                                        if (!isFirebaseReal) {
                                            onSignUp(
                                                if (name.isBlank()) "Novo Usuário" else name.trim(),
                                                if (email.isBlank()) "usuario@whatsapp.com" else email.trim(),
                                                if (password.isBlank()) "123456" else password.trim()
                                            )
                                        } else {
                                            localError = "Por favor, preencha todos os campos."
                                        }
                                    }
                                    password.length < 6 -> {
                                        if (!isFirebaseReal) {
                                            onSignUp(name.trim(), email.trim(), password.trim())
                                        } else {
                                            localError = "A senha deve ter pelo menos 6 caracteres."
                                        }
                                    }
                                    password != confirmPassword -> {
                                        if (!isFirebaseReal) {
                                            onSignUp(name.trim(), email.trim(), password.trim())
                                        } else {
                                            localError = "As senhas não conferem."
                                        }
                                    }
                                    else -> {
                                        onSignUp(name.trim(), email.trim(), password.trim())
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("register_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            if (authStatus is AuthStatus.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Criar Conta", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip / Test Mode Option
            TextButton(
                onClick = onSkipAuth,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("skip_auth_button")
            ) {
                Text(
                    text = "Entrar sem conta (Modo Convidado) →",
                    color = brandColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
