package com.michis.player

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.michis.player.feature.bookdetails.BookDetailsScreen
import com.michis.player.feature.bookmarks.BookmarksScreen
import com.michis.player.feature.library.LibraryRoute
import com.michis.player.feature.player.MiniPlayerRoute
import com.michis.player.feature.player.PlayerPanelHandleRoute
import com.michis.player.feature.player.PlayerRoute
import com.michis.player.feature.settings.SettingsScreen

private const val LIBRARY_ROUTE = "library"
private const val BOOKMARKS_ROUTE = "bookmarks"
private const val SETTINGS_ROUTE = "settings"
private const val BOOK_DETAILS_ROUTE = "book/{bookId}"
private const val PLAYER_ROUTE = "player"
private const val PLAYER_BOOK_ROUTE = "player/{bookId}"

private data class TopLevelDestination(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun MichisPlayerApp() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        PlayerScaffold(isLandscape = maxWidth > maxHeight)
    }
}

@Composable
private fun PlayerScaffold(isLandscape: Boolean) {
    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination("Biblioteca", LIBRARY_ROUTE, Icons.Rounded.Book),
        TopLevelDestination("Marcadores", BOOKMARKS_ROUTE, Icons.Rounded.Bookmark),
        TopLevelDestination("Configuración", SETTINGS_ROUTE, Icons.Rounded.Settings),
    )
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    var panelVisible by rememberSaveable { mutableStateOf(false) }
    var panelBookId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(isLandscape, currentRoute) {
        if (isLandscape && (currentRoute == PLAYER_ROUTE || currentRoute == PLAYER_BOOK_ROUTE)) {
            panelBookId = entry?.arguments?.getString("bookId") ?: panelBookId
            panelVisible = true
            navController.popBackStack()
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                if (!isLandscape && currentRoute != PLAYER_ROUTE && currentRoute != PLAYER_BOOK_ROUTE) {
                    MiniPlayerRoute(onOpenPlayer = { navController.navigate(PLAYER_ROUTE) })
                }
                NavigationBar(modifier = Modifier.height(if (isLandscape) 48.dp else 80.dp)) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.openTopLevel(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = if (isLandscape) null else ({ Text(destination.label) }),
                        )
                    }
                }
            }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val panelWidth = (maxWidth * 0.44f).coerceIn(320.dp, 520.dp)
            val animatedPanelWidth by animateDpAsState(
                targetValue = if (isLandscape && panelVisible) panelWidth else 0.dp,
                label = "playerPanelWidth",
            )
            val panelTranslation = with(LocalDensity.current) { (panelWidth - animatedPanelWidth).toPx() }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                PlayerNavigation(
                    navController = navController,
                    isLandscape = isLandscape,
                    onPlayInPanel = { bookId ->
                        panelBookId = bookId
                        panelVisible = true
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                if (isLandscape) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 4.dp,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        PlayerPanelHandleRoute(
                            expanded = panelVisible,
                            onToggle = { panelVisible = !panelVisible },
                        )
                    }
                    Box(
                        modifier = Modifier.width(animatedPanelWidth).fillMaxHeight().clipToBounds(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Surface(
                            modifier = Modifier
                                .requiredWidth(panelWidth)
                                .fillMaxHeight()
                                .graphicsLayer { translationX = panelTranslation },
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                        ) {
                            PlayerRoute(bookId = panelBookId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerNavigation(
    navController: NavHostController,
    isLandscape: Boolean,
    onPlayInPanel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(navController, startDestination = LIBRARY_ROUTE, modifier = modifier) {
        composable(LIBRARY_ROUTE) {
            LibraryRoute(onOpenBook = { id -> navController.navigate("book/${Uri.encode(id)}") })
        }
        composable(BOOKMARKS_ROUTE) { BookmarksScreen() }
        composable(SETTINGS_ROUTE) { SettingsScreen() }
        composable(
            route = BOOK_DETAILS_ROUTE,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId").orEmpty()
            BookDetailsScreen(
                bookId = bookId,
                onPlay = {
                    if (isLandscape) onPlayInPanel(bookId)
                    else navController.navigate("player/${Uri.encode(bookId)}")
                },
            )
        }
        composable(PLAYER_ROUTE) { PlayerRoute(bookId = null) }
        composable(
            route = PLAYER_BOOK_ROUTE,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { backStackEntry -> PlayerRoute(bookId = backStackEntry.arguments?.getString("bookId")) }
    }
}

private fun NavHostController.openTopLevel(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(LIBRARY_ROUTE) { saveState = true }
        restoreState = true
    }
}
