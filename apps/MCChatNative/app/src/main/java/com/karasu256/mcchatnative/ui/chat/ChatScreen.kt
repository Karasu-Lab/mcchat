package com.karasu256.mcchatnative.ui.chat

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karasu256.mcchatnative.R
import com.karasu256.mcchatnative.model.ChatMessage
import com.karasu256.mcchatnative.model.MessageReaction
import com.karasu256.mcchatnative.model.ReactionData
import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.UserId
import com.karasu256.mcchatnative.ui.PageTransitions
import com.karasu256.mcchatnative.ui.StatusDot
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds


private const val RowEnterMillis = 250
private const val RowExitMillis = 200
private const val AvatarEnterMillis = 250
private const val AvatarExitMillis = 200
private val StartOrigin = TransformOrigin(0f, 0.5f)
private val EndOrigin = TransformOrigin(1f, 0.5f)

private object ChatItemMotion {
    fun enter(origin: TransformOrigin): EnterTransition =
        fadeIn(tween(RowEnterMillis)) +
            scaleIn(tween(RowEnterMillis), initialScale = 0.6f, transformOrigin = origin)

    fun exit(origin: TransformOrigin): ExitTransition =
        fadeOut(tween(RowExitMillis)) +
            scaleOut(tween(RowExitMillis), targetScale = 0.6f, transformOrigin = origin)
}

private data class FlashRequest(val messageId: Long, val nonce: Int)

private const val FlashCount = 2
private const val FlashPeakAlpha = 0.4f
private const val FlashInMillis = 200
private const val FlashOutMillis = 300
private const val ScrollMillis = 400
private const val ReplyPreviewLimit = 20
private val ReplyAvatarSize = 20.dp
private val BubbleMaxWidth = 260.dp
private const val DotCycleMillis = 1_200
private const val DotStagger = 0.15f

@Destination<RootGraph>(style = PageTransitions::class)
@Composable
fun ChatScreen(
    clientId: String,
    navigator: DestinationsNavigator,
) {
    val viewModel: ChatViewModel = viewModel(
        key = clientId,
        factory = viewModelFactory { initializer { ChatViewModel(clientId) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatContent(
        state = state,
        onBack = navigator::navigateUp,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::sendMessage,
        onReact = viewModel::onReact,
        onReplyToggle = viewModel::onReplyToggle,
        onReplyCancel = viewModel::onReplyCancel,
        onMentionToggle = viewModel::onMentionToggle,
        onClearHistory = viewModel::clearHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContent(
    state: ChatUiState,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onReact: (Long, ReactionData) -> Unit,
    onReplyToggle: (Long) -> Unit,
    onReplyCancel: () -> Unit,
    onMentionToggle: () -> Unit,
    onClearHistory: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(state.client?.name.orEmpty())
                        StatusDot(isOnline = state.client?.isOnline == true)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.chat_back),
                        )
                    }
                },
                actions = {
                    ActiveUsersAction(headerUsers = state.headerUsers, allUsers = state.activeUsers)
                    ChatMenuAction(onClearHistory = onClearHistory)
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(modifier = Modifier.padding(padding).imePadding()) {
            MessageList(
                messages = state.messages,
                currentUserId = state.currentUserId,
                typingUsers = state.typingUsers.filter { it.id != state.currentUserId },
                reactions = state.reactions,
                onReact = onReact,
                onReplyToggle = onReplyToggle,
                modifier = Modifier.weight(1f),
            )
            MessageInput(
                draft = state.draft,
                canSend = state.canSend,
                replyTarget = state.replyTarget,
                mentionEnabled = state.mentionEnabled,
                onDraftChange = onDraftChange,
                onSend = onSend,
                onReplyCancel = onReplyCancel,
                onMentionToggle = onMentionToggle,
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    currentUserId: UserId,
    typingUsers: List<User>,
    reactions: List<ReactionData>,
    onReact: (Long, ReactionData) -> Unit,
    onReplyToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty() && typingUsers.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().clearFocusOnTap(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.chat_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val seenIds = remember { HashSet<Long>() }
    val isFirstBatch = remember { booleanArrayOf(true) }
    if (isFirstBatch[0] && messages.isNotEmpty()) {
        messages.forEach { seenIds.add(it.id) }
        isFirstBatch[0] = false
    }
    val messagesById = remember(messages) { messages.associateBy { it.id } }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val heights = remember { mutableStateMapOf<Long, Int>() }
    val tops = remember { mutableStateMapOf<Long, Int>() }
    var flash by remember { mutableStateOf<FlashRequest?>(null) }
    LaunchedEffect(scroll) {
        var previousHeight = 0
        var wasAtBottom = true
        snapshotFlow { viewportHeight to !scroll.canScrollForward }
            .collect { (height, atBottom) ->
                if (previousHeight != 0 && height < previousHeight && wasAtBottom) {
                    scroll.scrollBy((previousHeight - height).toFloat())
                }
                wasAtBottom = atBottom
                previousHeight = height
            }
    }
    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(messages.size, typingUsers.isNotEmpty()) {
        if (initialScrollDone) {
            scroll.animateScrollTo(scroll.maxValue)
        } else {
            snapshotFlow { scroll.maxValue }.first { it > 0 }
            scroll.scrollTo(scroll.maxValue)
            initialScrollDone = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clearFocusOnTap()
            .onSizeChanged { viewportHeight = it.height }
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(0.dp))
        messages.forEach { message ->
            key(message.id) {
                val animateEnter = message.id !in seenIds
                SideEffect { seenIds.add(message.id) }
                ViewportItem(
                    id = message.id,
                    scroll = scroll,
                    viewportHeight = viewportHeight,
                    heights = heights,
                    tops = tops,
                ) {
                    message.world?.let { world ->
                        WorldDivider(world)
                        return@ViewportItem
                    }
                    MessageRow(
                        message = message,
                        currentUserId = currentUserId,
                        replyTarget = message.replyId?.let(messagesById::get),
                        animateEnter = animateEnter,
                        reactions = reactions,
                        onReact = { onReact(message.id, it) },
                        onReply = { onReplyToggle(message.id) },
                        flash = flash?.takeIf { it.messageId == message.id },
                        onReplyClick = {
                            message.replyId?.let { targetId ->
                                tops[targetId]?.let { top ->
                                    scope.launch {
                                        scroll.animateScrollTo(top, tween(ScrollMillis, easing = FastOutSlowInEasing))
                                        flash = FlashRequest(targetId, (flash?.nonce ?: 0) + 1)
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
        ViewportItem(
            id = Long.MIN_VALUE,
            scroll = scroll,
            viewportHeight = viewportHeight,
            heights = heights,
            tops = tops,
        ) {
            TypingRow(typingUsers)
        }
        Spacer(Modifier.height(0.dp))
    }
}

@Composable
private fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(focusManager) { detectTapGestures { focusManager.clearFocus() } }
}

@Composable
private fun ViewportItem(
    id: Long,
    scroll: ScrollState,
    viewportHeight: Int,
    heights: SnapshotStateMap<Long, Int>,
    tops: SnapshotStateMap<Long, Int>,
    content: @Composable () -> Unit,
) {
    val currentViewportHeight by rememberUpdatedState(viewportHeight)
    val visible by remember {
        derivedStateOf {
            val height = heights[id] ?: return@derivedStateOf true
            val top = tops[id] ?: return@derivedStateOf true
            top + height > scroll.value && top < scroll.value + currentViewportHeight
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { tops[id] = it.positionInParent().y.roundToInt() },
    ) {
        if (visible) {
            Box(modifier = Modifier.onSizeChanged { heights[id] = it.height }) {
                content()
            }
        } else {
            Spacer(Modifier.height(with(LocalDensity.current) { (heights[id] ?: 0).toDp() }))
        }
    }
}

@Composable
private fun MessageRow(
    message: ChatMessage,
    currentUserId: UserId,
    replyTarget: ChatMessage?,
    animateEnter: Boolean,
    reactions: List<ReactionData>,
    onReact: (ReactionData) -> Unit,
    onReply: () -> Unit,
    flash: FlashRequest?,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(flash) {
        if (flash != null) {
            repeat(FlashCount) {
                flashAlpha.animateTo(FlashPeakAlpha, tween(FlashInMillis))
                flashAlpha.animateTo(0f, tween(FlashOutMillis))
            }
        }
    }
    val state = remember { MutableTransitionState(!animateEnter) }
    state.targetState = true
    val isOwn = message.isSentBy(currentUserId)
    val isMentioned = message.isMentioning(currentUserId, replyTarget)
    val origin = if (isOwn) EndOrigin else StartOrigin

    AnimatedVisibility(
        visibleState = state,
        modifier = modifier.fillMaxWidth(),
        enter = ChatItemMotion.enter(origin),
        exit = ChatItemMotion.exit(origin),
    ) {
        SwipeToReply(onReply = onReply) {
            MessageRowContent(
                message, isOwn, isMentioned, currentUserId, replyTarget, reactions, onReact, onReplyClick,
                flashAlpha = { flashAlpha.value },
            )
        }
    }
}

@Composable
private fun MessageRowContent(
    message: ChatMessage,
    isOwn: Boolean,
    isMentioned: Boolean,
    currentUserId: UserId,
    replyTarget: ChatMessage?,
    reactions: List<ReactionData>,
    onReact: (ReactionData) -> Unit,
    onReplyClick: () -> Unit,
    flashAlpha: () -> Float,
) {
    val bias = animatedOwnBias(isOwn)
    val inset by animateDpAsState(if (isOwn) 0.dp else AvatarSize + 8.dp, tween(OwnershipMillis))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = BiasAlignment.Horizontal(bias),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (replyTarget != null) {
            ReplyContainer(
                target = replyTarget,
                onClick = onReplyClick,
                modifier = Modifier.padding(start = inset),
            )
        }
        MessageLine(message, isOwn, isMentioned, currentUserId, reactions, onReact, flashAlpha)
        if (message.reactions.isNotEmpty()) {
            ReactionRow(
                reactions = message.reactions,
                currentUserId = currentUserId,
                onReact = onReact,
                modifier = Modifier.padding(start = inset),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReactionRow(
    reactions: List<MessageReaction>,
    currentUserId: UserId,
    onReact: (ReactionData) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { reaction ->
            FilterChip(
                selected = reaction.isReactedBy(currentUserId),
                onClick = { onReact(reaction.data) },
                label = {
                    Text(
                        text = "${reaction.data.emoji} ${reaction.count}",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageLine(
    message: ChatMessage,
    isOwn: Boolean,
    isMentioned: Boolean,
    currentUserId: UserId,
    reactions: List<ReactionData>,
    onReact: (ReactionData) -> Unit,
    flashAlpha: () -> Float,
) {
    val flashColor = MaterialTheme.colorScheme.secondary
    var pickerVisible by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val time = DateFormat.getTimeFormat(LocalContext.current).format(Date(message.timestampMillis))
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        when {
            isMentioned -> colors.secondaryContainer
            isOwn -> colors.primaryContainer
            else -> colors.surfaceContainerHigh
        },
        tween(OwnershipMillis),
    )
    val contentColor by animateColorAsState(
        when {
            isMentioned -> colors.onSecondaryContainer
            isOwn -> colors.onPrimaryContainer
            else -> colors.onSurface
        },
        tween(OwnershipMillis),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.aligned(BiasAlignment.Horizontal(animatedOwnBias(isOwn))),
        verticalAlignment = Alignment.Bottom,
    ) {
        AnimatedVisibility(
            visible = isOwn,
            enter = fadeIn(tween(OwnershipMillis)) + expandHorizontally(tween(OwnershipMillis)),
            exit = fadeOut(tween(OwnershipMillis)) + shrinkHorizontally(tween(OwnershipMillis)),
        ) {
            Row {
                MessageTime(time)
                Spacer(Modifier.width(8.dp))
            }
        }
        AnimatedVisibility(
            visible = !isOwn,
            enter = fadeIn(tween(OwnershipMillis)) + expandHorizontally(tween(OwnershipMillis)),
            exit = fadeOut(tween(OwnershipMillis)) + shrinkHorizontally(tween(OwnershipMillis)),
        ) {
            Row {
                UserAvatar(message.sender)
                Spacer(Modifier.width(8.dp))
            }
        }
        Box {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .widthIn(max = BubbleMaxWidth)
                    .clip(MaterialTheme.shapes.large)
                    .drawWithContent {
                        drawContent()
                        drawRect(flashColor, alpha = flashAlpha())
                    }
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            pickerVisible = true
                        },
                    ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AnimatedVisibility(
                        visible = !isOwn,
                        enter = fadeIn(tween(OwnershipMillis)) + expandVertically(tween(OwnershipMillis)),
                        exit = fadeOut(tween(OwnershipMillis)) + shrinkVertically(tween(OwnershipMillis)),
                    ) {
                        Text(
                            text = message.sender.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(message.body, style = MaterialTheme.typography.bodyLarge)
                }
            }
            ReactionPicker(
                visible = pickerVisible,
                reactions = reactions,
                selected = message.reactions.filter { it.isReactedBy(currentUserId) }.mapTo(HashSet()) { it.data },
                onReact = {
                    onReact(it)
                    pickerVisible = false
                },
                onDismissRequest = { pickerVisible = false },
            )
        }
        AnimatedVisibility(
            visible = !isOwn,
            enter = fadeIn(tween(OwnershipMillis)) + expandHorizontally(tween(OwnershipMillis)),
            exit = fadeOut(tween(OwnershipMillis)) + shrinkHorizontally(tween(OwnershipMillis)),
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                MessageTime(time)
            }
        }
    }
}

private const val OwnershipMillis = 300

@Composable
private fun animatedOwnBias(isOwn: Boolean): Float =
    animateFloatAsState(if (isOwn) 1f else -1f, tween(OwnershipMillis)).value

@Composable
private fun ReplyContainer(target: ChatMessage, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.widthIn(max = BubbleMaxWidth),
    ) {
        ReplyQuote(target, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
private fun ReplyQuote(target: ChatMessage, modifier: Modifier = Modifier) {
    val nameColor = MaterialTheme.colorScheme.primary
    val text = remember(target, nameColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Medium)) {
                append(target.sender.name)
            }
            append(" ")
            append(target.body.takeCodePoints(ReplyPreviewLimit))
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UserAvatar(target.sender, size = ReplyAvatarSize)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            imageVector = ReplyIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun String.takeCodePoints(limit: Int): String {
    if (codePointCount(0, length) <= limit) return this
    return substring(0, offsetByCodePoints(0, limit))
}

@Composable
private fun MessageTime(time: String) {
    Text(
        text = time,
        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private data class TypingEntry(val user: User, val animateEnter: Boolean)

@Composable
private fun TypingRow(users: List<User>, modifier: Modifier = Modifier) {
    var roster by remember { mutableStateOf(users.map { TypingEntry(it, animateEnter = false) }) }
    LaunchedEffect(users) {
        if (users.isEmpty()) {
            delay(RowExitMillis.toLong().milliseconds)
            roster = emptyList()
        } else {
            val known = roster.mapTo(HashSet()) { it.user.id }
            val animate = roster.isNotEmpty()
            roster = roster + users.filter { it.id !in known }.map { TypingEntry(it, animateEnter = animate) }
        }
    }

    Box(modifier = modifier.fillMaxWidth().animateContentSize()) {
        AnimatedVisibility(
            visible = users.isNotEmpty(),
            enter = ChatItemMotion.enter(StartOrigin),
            exit = ChatItemMotion.exit(StartOrigin),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                AvatarStack(modifier = Modifier.animateContentSize()) {
                    roster.forEach { entry ->
                        key(entry.user.id) {
                            TypingAvatar(
                                user = entry.user,
                                visible = users.any { it.id == entry.user.id } || users.isEmpty(),
                                animateEnter = entry.animateEnter,
                                onGone = { roster = roster.filter { it.user.id != entry.user.id } },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    TypingDots(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun TypingAvatar(
    user: User,
    visible: Boolean,
    animateEnter: Boolean,
    onGone: () -> Unit,
) {
    val state = remember { MutableTransitionState(!animateEnter) }
    state.targetState = visible

    AnimatedVisibility(
        visibleState = state,
        enter = slideInHorizontally(tween(AvatarEnterMillis)) { -it } + fadeIn(tween(AvatarEnterMillis)),
        exit = scaleOut(tween(AvatarExitMillis), targetScale = 0.6f) + fadeOut(tween(AvatarExitMillis)),
    ) {
        UserAvatar(user, ringColor = MaterialTheme.colorScheme.surface)
    }

    LaunchedEffect(visible) {
        if (!visible) {
            delay(AvatarExitMillis.toLong().milliseconds)
            onGone()
        }
    }
}

@Composable
private fun TypingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DotCycleMillis, easing = LinearEasing)),
        label = "phase",
    )
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        val t = (phase - index * DotStagger + 1f) % 1f
                        alpha = 0.3f + 0.7f * if (t < 0.5f) t * 2f else (1f - t) * 2f
                    }
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
private fun MessageInput(
    draft: String,
    canSend: Boolean,
    replyTarget: ChatMessage?,
    mentionEnabled: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onReplyCancel: () -> Unit,
    onMentionToggle: () -> Unit,
) {
    var lastTarget by remember { mutableStateOf(replyTarget) }
    SideEffect { if (replyTarget != null) lastTarget = replyTarget }
    val shownTarget = replyTarget ?: lastTarget

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
        ) {
            AnimatedVisibility(
                visible = replyTarget != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                if (shownTarget != null) {
                    ReplyStatus(
                        target = shownTarget,
                        mentionEnabled = mentionEnabled,
                        onMentionToggle = onMentionToggle,
                        onCancel = onReplyCancel,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                    maxLines = 4,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.weight(1f),
                )
                val defaultColors = IconButtonDefaults.filledIconButtonColors()
                val sendContainer by animateColorAsState(
                    if (canSend) defaultColors.containerColor else defaultColors.disabledContainerColor,
                )
                val sendContent by animateColorAsState(
                    if (canSend) defaultColors.contentColor else defaultColors.disabledContentColor,
                )
                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = sendContainer,
                        contentColor = sendContent,
                        disabledContainerColor = sendContainer,
                        disabledContentColor = sendContent,
                    ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplyStatus(
    target: ChatMessage,
    mentionEnabled: Boolean,
    onMentionToggle: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReplyQuote(target, modifier = Modifier.weight(1f))
        IconToggleButton(checked = mentionEnabled, onCheckedChange = { onMentionToggle() }) {
            Icon(
                imageVector = MentionIcon,
                contentDescription = stringResource(R.string.chat_reply_mention),
                tint = if (mentionEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = onCancel) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.chat_reply_cancel),
            )
        }
    }
}
