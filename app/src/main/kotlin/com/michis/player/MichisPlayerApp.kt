package com.michis.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
import com.michis.player.feature.bookdetails.BookDetailsScreen
import com.michis.player.feature.bookmarks.BookmarksScreen
import com.michis.player.feature.library.LibraryRoute
import com.michis.player.feature.settings.SettingsScreen
private const val LIBRARY_ROUTE = "library"
private const val BOOKMARKS_ROUTE = "bookmarks"
private const val SETTINGS_ROUTE = "settings"
private const val BOOK_DETAILS_ROUTE = "book/{bookId}"

private data class TopLevelDestination(val label: String, val route: String)

@Composable
fun MichisPlayerApp() {
    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination("Biblioteca", LIBRARY_ROUTE),
        TopLevelDestination("Marcadores", BOOKMARKS_ROUTE),
        TopLevelDestination("Configuración", SETTINGS_ROUTE),
    )
    val entry by navController.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = entry?.destination?.route == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigate(destination.route) { launchSingleTop = true; popUpTo(LIBRARY_ROUTE) { saveState = true }; restoreState = true } },
                        icon = { Text(destination.label.take(1)) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = LIBRARY_ROUTE, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(LIBRARY_ROUTE) { LibraryRoute(onOpenBook = { id -> navController.navigate("book/${Uri.encode(id)}") }) }
            composable(BOOKMARKS_ROUTE) { BookmarksScreen() }
            composable(SETTINGS_ROUTE) { SettingsScreen() }
            composable(
                route = BOOK_DETAILS_ROUTE,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { backStackEntry ->
                BookDetailsScreen(bookId = backStackEntry.arguments?.getString("bookId").orEmpty(), onPlay = {})
            }
        }
    }
}
