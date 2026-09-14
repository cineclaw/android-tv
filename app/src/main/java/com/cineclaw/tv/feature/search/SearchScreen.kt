package com.cineclaw.tv.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.MediaItem

private val QUICK_SUGGESTIONS = listOf(
    "Мэйдэй",
    "Дюна",
    "Сёгун",
    "Одни из нас",
    "Рик и Морти",
    "4K UHD",
    "Пингвин",
    "Медведь",
    "Укрытие",
    "Игра престолов"
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    results: List<MediaItem>,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onVoiceSearchClick: () -> Unit,
    onSelectMedia: (MediaItem) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val chipsFocusRequester = remember { FocusRequester() }
    val micFocusRequester = remember { FocusRequester() }
    var isInputFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Search Bar & Voice Search Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Android TV Text Field (triggers Gboard / Leanback IME)
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isInputFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    chipsFocusRequester.requestFocus()
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (query.isEmpty()) {
                                        micFocusRequester.requestFocus()
                                        true
                                    } else false
                                }
                                else -> false
                            }
                        } else false
                    },
                placeholder = {
                    Text(
                        text = "Поиск фильмов, сериалов, персон...",
                        color = TextMuted,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск",
                        tint = if (isInputFocused) EmeraldPrimary else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Очистить",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        chipsFocusRequester.requestFocus()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurfaceVariant.copy(alpha = 0.6f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = ObsidianBorder,
                    cursorColor = EmeraldPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            // Dedicated Voice Search Button
            Surface(
                onClick = { onVoiceSearchClick() },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp)
                    )
                ),
                glow = ClickableSurfaceDefaults.glow(
                    focusedGlow = Glow(
                        elevationColor = EmeraldGlow,
                        elevation = 14.dp
                    )
                ),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = EmeraldPrimary,
                    focusedContainerColor = EmeraldPrimary
                ),
                modifier = Modifier
                    .size(56.dp)
                    .focusRequester(micFocusRequester)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Голосовой поиск",
                        tint = ObsidianBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            itemsIndexed(QUICK_SUGGESTIONS) { index, suggestion ->
                val isSelected = query.equals(suggestion, ignoreCase = true)
                val chipModifier = if (index == 0) Modifier.focusRequester(chipsFocusRequester) else Modifier
                Surface(
                    onClick = { onQueryChange(suggestion) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(1.5.dp, EmeraldPrimary),
                            shape = RoundedCornerShape(20.dp)
                        )
                    ),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else ObsidianSurface,
                        focusedContainerColor = ObsidianSurfaceVariant
                    ),
                    modifier = chipModifier
                ) {
                    Text(
                        text = suggestion,
                        color = if (isSelected) EmeraldPrimary else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Main Cinema Results Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 3.dp)
                            Text(
                                text = "Поиск в каталоге...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                results.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (query.isBlank()) "Введите название или используйте голосовой поиск" else "По запросу «$query» ничего не найдено",
                                color = TextMuted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (query.isBlank()) {
                                Text(
                                    text = "Поддерживается поиск по фильмам, сериалам и оригинальным названиям",
                                    color = TextMuted.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(results, key = { it.effectiveTconst }) { item ->
                            TvCinemaCard(
                                title = item.displayTitle,
                                posterUrl = item.effectivePoster,
                                year = item.year,
                                rating = item.rating,
                                onClick = { onSelectMedia(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
