package com.karasu256.mcchatnative.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.karasu256.mcchatnative.data.ConnectionManager
import com.karasu256.mcchatnative.data.ConnectionService
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.karasu256.mcchatnative.R
import com.karasu256.mcchatnative.model.Client
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karasu256.mcchatnative.ui.PageTransitions
import com.karasu256.mcchatnative.ui.StatusDot
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ChatScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.karasu256.mcchatnative.ui.drawer.AppDrawer
import com.karasu256.mcchatnative.ui.drawer.DrawerViewModel
import com.karasu256.mcchatnative.ui.search.SearchUiState
import com.karasu256.mcchatnative.ui.search.SearchViewModel
import com.karasu256.mcchatnative.ui.splash.SplashScreen
import com.karasu256.mcchatnative.ui.theme.MCChatTheme
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    private val runtimePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startConnectionService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ConnectionManager.start(applicationContext)
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= 37) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            startConnectionService()
        } else {
            runtimePermissions.launch(missing.toTypedArray())
        }
        setContent {
            MCChatTheme {
                DestinationsNavHost(navGraph = NavGraphs.root)
            }
        }
    }

    private fun startConnectionService() {
        ContextCompat.startForegroundService(this, Intent(this, ConnectionService::class.java))
    }
}

private const val DrawerShrinkScale = 0.1f
private const val DrawerShrinkShift = 0.1f

@Destination<RootGraph>(start = true, style = PageTransitions::class)
@Composable
fun MainScreen(
    navigator: DestinationsNavigator,
    mainViewModel: MainViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    drawerViewModel: DrawerViewModel = viewModel(),
) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val search by searchViewModel.uiState.collectAsStateWithLifecycle()
    Crossfade(targetState = search.isLoading, label = "splash") { loading ->
        if (loading) {
            SplashScreen()
        } else {
            MainContent(
                state = state,
                search = search,
                drawerViewModel = drawerViewModel,
                onSearch = searchViewModel::search,
                onAddConnection = searchViewModel::addConnection,
                onRemoveConnection = {
                    searchViewModel.removeConnection(it)
                    drawerViewModel.onClientRemoved(it)
                },
                onSearchStart = mainViewModel::onSearchStart,
                onSearchClose = {
                    mainViewModel.onSearchClose()
                    searchViewModel.search("")
                },
                onDrawerOpen = mainViewModel::onDrawerOpen,
                onDrawerClose = mainViewModel::onDrawerClose,
                onDrawerSettled = mainViewModel::onDrawerSettled,
                onBack = mainViewModel::onBack,
                onOpenChat = { navigator.navigate(ChatScreenDestination(clientId = it)) },
                onSettingsClick = {},
            )
        }
    }
}

@Composable
private fun MainContent(
    state: MainUiState,
    search: SearchUiState,
    drawerViewModel: DrawerViewModel,
    onSearch: (String) -> Unit,
    onAddConnection: suspend (String) -> Boolean,
    onRemoveConnection: (String) -> Unit,
    onSearchStart: () -> Unit,
    onSearchClose: () -> Unit,
    onDrawerOpen: () -> Unit,
    onDrawerClose: () -> Unit,
    onDrawerSettled: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerUiState by drawerViewModel.uiState.collectAsStateWithLifecycle()
    var backProgress by remember { mutableFloatStateOf(0f) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<Client?>(null) }

    if (showAddDialog) {
        AddConnectionDialog(
            onConfirm = onAddConnection,
            onDismiss = { showAddDialog = false },
        )
    }
    pendingRemoval?.let { client ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.connection_remove)) },
            text = { Text(stringResource(R.string.connection_remove_message, client.address)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveConnection(client.id)
                        pendingRemoval = null
                    },
                ) { Text(stringResource(R.string.connection_remove_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text(stringResource(R.string.connection_cancel))
                }
            },
        )
    }

    LaunchedEffect(state.isDrawerOpen) {
        if (state.isDrawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }.collect { onDrawerSettled(it == DrawerValue.Open) }
    }
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed) backProgress = 0f
    }

    val backLayer = state.topBackLayer

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = state.isDrawerOpen,
        drawerContent = {
            AppDrawer(
                state = drawerUiState,
                onClose = onDrawerClose,
                onThreadClick = {
                    drawerViewModel.onThreadSelected(it.id)
                    onDrawerClose()
                    onOpenChat(it.clientId)
                },
                onPinToggle = drawerViewModel::onPinToggle,
                onSettingsClick = onSettingsClick,
                modifier = Modifier.graphicsLayer {
                    val offset = drawerState.currentOffset
                    val closedness = if (offset.isNaN()) 0f else (-offset / size.width).coerceIn(0f, 1f)
                    val shrink = maxOf(backProgress, closedness)
                    scaleX = 1f - DrawerShrinkScale * shrink
                    translationX = -size.width * DrawerShrinkShift * shrink
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
            )
        },
    ) {
        PredictiveBackHandler(enabled = backLayer != null) { progress ->
            try {
                progress.collect { if (backLayer == BackLayer.Drawer) backProgress = it.progress }
                onBack()
            } catch (e: CancellationException) {
                backProgress = 0f
                throw e
            }
        }
        Scaffold(
            topBar = {
                MainTopBar(
                    state = state,
                    query = search.query,
                    onMenuClick = onDrawerOpen,
                    onQueryChange = onSearch,
                    onSearchStart = onSearchStart,
                    onSearchClose = onSearchClose,
                    onSettingsClick = onSettingsClick,
                )
            },
        ) { padding ->
            ClientList(
                search = search,
                onClientClick = { onOpenChat(it.id) },
                onAddClick = { showAddDialog = true },
                onRemoveRequest = { pendingRemoval = it },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    state: MainUiState,
    query: String,
    onMenuClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchStart: () -> Unit,
    onSearchClose: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    TopAppBar(
        title = {
            if (state.isSearching) {
                SearchField(query = query, onQueryChange = onQueryChange)
            } else {
                Text(stringResource(R.string.clients_title))
            }
        },
        navigationIcon = {
            if (state.isSearching) {
                IconButton(onClick = onSearchClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.search_close),
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_drawer))
                }
            }
        },
        actions = {
            if (!state.isSearching) {
                IconButton(onClick = onSearchStart) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
            }
        },
    )
}

@Composable
private fun AddConnectionDialog(onConfirm: suspend (String) -> Boolean, onDismiss: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!checking) onDismiss() },
        title = { Text(stringResource(R.string.connection_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it
                        failed = false
                    },
                    enabled = !checking,
                    isError = failed,
                    singleLine = true,
                    label = { Text(stringResource(R.string.connection_host)) },
                    placeholder = { Text(stringResource(R.string.connection_host_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                when {
                    checking -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.connection_checking), style = MaterialTheme.typography.bodySmall)
                    }
                    failed -> Text(
                        stringResource(R.string.connection_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    checking = true
                    scope.launch {
                        if (onConfirm(host)) {
                            onDismiss()
                        } else {
                            failed = true
                            checking = false
                        }
                    }
                },
                enabled = host.isNotBlank() && !checking,
            ) {
                Text(stringResource(R.string.connection_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.connection_cancel)) }
        },
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(contentAlignment = Alignment.CenterStart) {
        if (query.isEmpty()) {
            Text(
                text = stringResource(R.string.search),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun ClientList(
    search: SearchUiState,
    onClientClick: (Client) -> Unit,
    onAddClick: () -> Unit,
    onRemoveRequest: (Client) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clients = search.results
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.connection_add))
        }
        if (clients.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(
                        if (search.query.isBlank()) R.string.clients_empty else R.string.clients_no_match,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(clients, key = { it.id }) { client ->
                    SwipeToRemove(onRemoveRequest = { onRemoveRequest(client) }) {
                        ClientRow(client = client, onClick = { onClientClick(client) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToRemove(onRemoveRequest: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemoveRequest()
            dismissState.reset()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.connection_remove),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, content = content)
    }
}

@Composable
private fun ClientRow(client: Client, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(client.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                text = client.address,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(client.name.take(1), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        trailingContent = { StatusDot(isOnline = client.isOnline) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
