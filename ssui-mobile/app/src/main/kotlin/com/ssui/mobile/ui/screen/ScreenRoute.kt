package com.ssui.mobile.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ssui.mobile.R
import com.ssui.mobile.domain.Screen
import com.ssui.mobile.ui.render.RenderElement
import org.koin.compose.viewmodel.koinViewModel

/** Entry composable: owns the ViewModel, collects state and effects, renders the three states. */
@Composable
fun ScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: ScreenViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Sent once per ViewModel lifetime; the ViewModel ignores repeats (rotation, recomposition).
    LaunchedEffect(viewModel) {
        viewModel.onEvent(ScreenEvent.Load)
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is ScreenEffect.ShowToast ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()

                    is ScreenEffect.OpenUrl -> try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.open_url_failed, effect.url),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SsuiTopBar(
                isLoading = uiState is ScreenUiState.Loading,
                onRefresh = { viewModel.onEvent(ScreenEvent.Refresh) },
            )
        },
    ) { innerPadding ->
        ScreenContent(
            state = uiState,
            innerPadding = innerPadding,
            onEvent = viewModel::onEvent,
        )
    }
}

/** App bar with a refresh action that re-fetches the screen definition from the backend. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SsuiTopBar(
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        actions = {
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier.testTag("refresh"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.refresh_content_description),
                )
            }
        },
    )
}

@Composable
fun ScreenContent(
    state: ScreenUiState,
    innerPadding: PaddingValues,
    onEvent: (ScreenEvent) -> Unit,
) {
    when (state) {
        ScreenUiState.Loading -> LoadingState(Modifier.padding(innerPadding))
        is ScreenUiState.Error -> ErrorState(
            message = state.message,
            onRetry = { onEvent(ScreenEvent.Retry) },
            modifier = Modifier.padding(innerPadding),
        )

        is ScreenUiState.Success -> SuccessState(
            screen = state.screen,
            innerPadding = innerPadding,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().testTag("loading"),
        contentAlignment = Alignment.Center,
    ) {
        val description = stringResource(R.string.loading_content_description)
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp).testTag("error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onRetry, modifier = Modifier.testTag("retry")) {
            Text(stringResource(R.string.retry))
        }
    }
}

/** Root COLUMN lives inside a vertically scrollable container that fills the screen (spec 4.2). */
@Composable
private fun SuccessState(
    screen: Screen,
    innerPadding: PaddingValues,
    onEvent: (ScreenEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .testTag("screen"),
    ) {
        RenderElement(
            element = screen.root,
            onElementClick = { elementId, action -> onEvent(ScreenEvent.ElementClicked(elementId, action)) },
        )
    }
}
