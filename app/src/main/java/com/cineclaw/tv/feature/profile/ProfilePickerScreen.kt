package com.cineclaw.tv.feature.profile

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*
import com.cineclaw.tv.CineClawApp
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.CreateProfileRequest
import com.cineclaw.tv.core.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfilePickerScreen(
    currentActiveProfileId: String = "default",
    fromDrawer: Boolean = false,
    onSelectProfile: (UserProfile) -> Unit,
    onNavigateToAuth: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val app = CineClawApp.instance
    val scope = rememberCoroutineScope()

    var profiles by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val firstCardFocusRequester = remember { FocusRequester() }
    val cardFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    BackHandler {
        onBackClick()
    }

    val fetchProfiles = {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val list = app.apiClient.getApi().getProfiles()
                profiles = list
            } catch (e: Exception) {
                val isAuthErr = e.message?.contains("401") == true || e.localizedMessage?.contains("401") == true
                if (isAuthErr) {
                    app.sessionManager.clearSessionAsync()
                    onNavigateToAuth()
                } else {
                    errorMessage = "Не удалось загрузить список профилей: ${e.localizedMessage}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchProfiles()
    }

    // Auto-focus on active profile card or first card once loaded
    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty()) {
            delay(150)
            val targetId = if (profiles.any { it.id == currentActiveProfileId }) currentActiveProfileId else profiles.first().id
            val req = cardFocusRequesters[targetId] ?: firstCardFocusRequester
            try {
                req.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            // Header: Logo & "Кто смотрит?"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "C", color = ObsidianBackground, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = "CineClaw",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Кто смотрит?",
                    color = TextPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "История просмотров, персональные полки и рекомендации будут сохранены",
                    color = TextMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Profiles Cards Row
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(vertical = 40.dp)
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(44.dp))
                    Text("Загрузка профилей...", color = TextSecondary, fontSize = 14.sp)
                }
            } else if (errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 30.dp)
                ) {
                    Text(errorMessage!!, color = CrimsonText, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { fetchProfiles() },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = ObsidianSurface,
                                focusedContainerColor = EmeraldPrimary
                            ),
                            modifier = Modifier.focusRequester(firstCardFocusRequester)
                        ) {
                            Text("Повторить", color = TextPrimary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                        }

                        Surface(
                            onClick = {
                                app.sessionManager.clearSessionAsync()
                                onNavigateToAuth()
                            },
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = ObsidianSurface,
                                focusedContainerColor = ObsidianSurfaceVariant
                            )
                        ) {
                            Text("Войти снова / Сервер", color = TextPrimary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // Existing profile cards
                    itemsIndexed(profiles, key = { _, p -> p.id }) { index, profile ->
                        val requester = cardFocusRequesters.getOrPut(profile.id) { FocusRequester() }
                        val isKids = profile.isKids
                        val isActive = profile.id == currentActiveProfileId

                        ProfileCard(
                            profile = profile,
                            isKids = isKids,
                            isActive = isActive,
                            focusRequester = requester,
                            onClick = {
                                onSelectProfile(profile)
                            }
                        )
                    }

                    // Add Profile Card
                    item(key = "add_profile_card") {
                        val addRequester = remember { FocusRequester() }
                        AddProfileCard(
                            focusRequester = addRequester,
                            onClick = { showCreateDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, isKids ->
                scope.launch {
                    try {
                        val req = CreateProfileRequest(
                            name = name,
                            isKids = isKids,
                            icon = if (isKids) "kids" else "adult"
                        )
                        val created = app.apiClient.getApi().createProfile(req)
                        showCreateDialog = false
                        // Automatically switch to the newly created profile
                        onSelectProfile(created)
                    } catch (e: Exception) {
                        errorMessage = "Ошибка при создании: ${e.localizedMessage}"
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: UserProfile,
    isKids: Boolean,
    isActive: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val accentColor = if (isKids) AmberUHD else EmeraldPrimary
    val glowColor = if (isKids) AmberGlow else EmeraldGlow

    val avatarBrush = if (isKids) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFB74D),
                Color(0xFFFF7043)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF10B981),
                Color(0xFF047857)
            )
        )
    }

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isActive) accentColor.copy(alpha = 0.5f) else ObsidianBorder),
                shape = RoundedCornerShape(24.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, accentColor),
                shape = RoundedCornerShape(24.dp)
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = glowColor,
                elevation = 18.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = ObsidianSurface,
            focusedContainerColor = ObsidianSurfaceVariant
        ),
        modifier = Modifier
            .width(170.dp)
            .height(230.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar Circle / Rounded Box
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(avatarBrush),
                contentAlignment = Alignment.Center
            ) {
                if (isKids) {
                    Text(
                        text = "🧸",
                        fontSize = 54.sp
                    )
                } else {
                    Text(
                        text = "👤",
                        fontSize = 52.sp
                    )
                }

                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ObsidianBackground)
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Profile Name
            Text(
                text = profile.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Kids badge
            if (isKids) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmberUHD.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Детский",
                        color = AmberUHD,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddProfileCard(
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.5.dp, ObsidianBorderHighlight),
                shape = RoundedCornerShape(24.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, EmeraldPrimary),
                shape = RoundedCornerShape(24.dp)
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevationColor = EmeraldGlow,
                elevation = 18.dp
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = ObsidianSurface.copy(alpha = 0.5f),
            focusedContainerColor = ObsidianSurfaceVariant
        ),
        modifier = Modifier
            .width(170.dp)
            .height(230.dp)
            .focusRequester(focusRequester)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianSurfaceVariant)
                    .border(1.5.dp, ObsidianBorderHighlight, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить профиль",
                    tint = TextSecondary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Добавить",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Новый профиль",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, isKids: Boolean) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var isKidsMode by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            nameFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(520.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
                    .padding(30.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Новый профиль",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Настройте имя и режим просмотра",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isKidsMode) AmberUHD.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isKidsMode) "🧸" else "👤", fontSize = 26.sp)
                        }
                    }

                    // Preset Name Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Быстрый выбор имени:",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val suggestions = listOf("Детский", "Семья", "Гость", "Малыши")
                            suggestions.forEach { suggestion ->
                                Surface(
                                    onClick = {
                                        profileName = suggestion
                                        if (suggestion == "Детский" || suggestion == "Малыши") {
                                            isKidsMode = true
                                        }
                                    },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = ObsidianSurfaceVariant,
                                        focusedContainerColor = EmeraldPrimary
                                    )
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Text Field for Profile Name
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Имя профиля (например: Детский, Алиса, Паша)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = if (isKidsMode) AmberUHD else EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                    )

                    // Kids Mode Toggle Row
                    Surface(
                        onClick = { isKidsMode = !isKidsMode },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = ObsidianSurfaceVariant,
                            focusedContainerColor = ObsidianSurfaceVariant
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, if (isKidsMode) AmberUHD else EmeraldPrimary),
                                shape = RoundedCornerShape(14.dp)
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "🧸 Детский режим",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Только мультфильмы, сказки, Disney/Pixar, безопасные новинки",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Switch(
                                checked = isKidsMode,
                                onCheckedChange = { isKidsMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmberUHD,
                                    checkedTrackColor = AmberUHD.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = ObsidianBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Buttons: Create and Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (profileName.isNotBlank() && !isSubmitting) {
                                    isSubmitting = true
                                    onCreate(profileName.trim(), isKidsMode)
                                }
                            },
                            enabled = profileName.isNotBlank() && !isSubmitting,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(2.dp, if (isKidsMode) AmberUHD else EmeraldPrimary),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            ),
                            glow = ClickableSurfaceDefaults.glow(
                                focusedGlow = Glow(
                                    elevationColor = if (isKidsMode) AmberGlow else EmeraldGlow,
                                    elevation = 12.dp
                                )
                            ),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (isKidsMode) AmberUHD else EmeraldPrimary,
                                focusedContainerColor = if (isKidsMode) AmberUHD else EmeraldPrimary,
                                contentColor = ObsidianBackground,
                                focusedContentColor = ObsidianBackground
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(color = ObsidianBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = "Создать профиль",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onDismiss,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
                            border = ClickableSurfaceDefaults.border(
                                focusedBorder = Border(
                                    border = BorderStroke(2.dp, ObsidianBorderHighlight),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            ),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = ObsidianSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(0.6f)
                                .height(48.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Отмена",
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
