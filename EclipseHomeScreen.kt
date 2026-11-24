
package com.example.eclipseandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.comicreader.data.SavedSeries
import com.example.comicreader.data.SortOrder

// Eclipse Obsidian & Violet/Indigo Accent Palette
val EclipseBg = Color(0xFF090A0F)
val EclipseSurface = Color(0xFF13151F)
val EclipseSurfaceElevated = Color(0xFF1C1F2E)
val EclipseAccent = Color(0xFF8B5CF6)
val EclipseTextPrimary = Color(0xFFF4F4F6)
val EclipseTextSecondary = Color(0xFF9CA3AF)

@Composable
fun EclipseAppTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = EclipseAccent,
        background = EclipseBg,
        surface = EclipseSurface,
        surfaceVariant = EclipseSurfaceElevated,
        onBackground = EclipseTextPrimary,
        onSurface = EclipseTextPrimary
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun EclipseHomeScreen(
    continueReading: List<SavedSeries>,
    library: List<SavedSeries>,
    unreadCounts: Map<String, Int>,
    streakDays: Int,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onOpenSeries: (SavedSeries) -> Unit,
    onOpenRepos: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = EclipseBg,
        bottomBar = {
            NavigationBar(
                containerColor = EclipseSurface.copy(alpha = 0.94f),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.CollectionsBookmark, contentDescription = "Library") },
                    label = { Text("Library") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Explore, contentDescription = "Sources") },
                    label = { Text("Sources") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.BarChart, contentDescription = "Stats") },
                    label = { Text("Stats") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeHubTab(
                modifier = Modifier.padding(padding),
                continueReading = continueReading,
                streakDays = streakDays,
                onOpenSeries = onOpenSeries
            )
            1 -> LibraryGridTab(
                modifier = Modifier.padding(padding),
                library = library,
                unreadCounts = unreadCounts,
                sortOrder = sortOrder,
                onSortChange = onSortChange,
                onOpenSeries = onOpenSeries
            )
            2 -> SourcesRepoTab(
                modifier = Modifier.padding(padding),
                onAddRepoClick = onOpenRepos
            )
            3 -> StatsOverviewTab(
                modifier = Modifier.padding(padding),
                totalSeries = library.size,
                streakDays = streakDays
            )
        }
    }
}

@Composable
private fun HomeHubTab(
    modifier: Modifier = Modifier,
    continueReading: List<SavedSeries>,
    streakDays: Int,
    onOpenSeries: (SavedSeries) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Eclipse",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EclipseTextPrimary
                    )
                    Text(
                        "Sync, Download & Track",
                        style = MaterialTheme.typography.bodySmall,
                        color = EclipseTextSecondary
                    )
                }
                // Eclipse-style Reading Streak Pill
                Surface(
                    shape = CircleShape,
                    color = EclipseSurfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "$streakDays day streak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EclipseTextPrimary
                        )
                    }
                }
            }
        }

        // Continue Reading Carousel
        item {
            Text(
                "Continue Reading",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = EclipseTextPrimary
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(continueReading) { series ->
                    ContinueReadingCard(series = series, onClick = { onOpenSeries(series) })
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(series: SavedSeries, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(135.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(EclipseSurfaceElevated)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = series.coverUrl,
            contentDescription = series.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                series.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Resume Chapter",
                style = MaterialTheme.typography.labelSmall,
                color = EclipseAccent
            )
        }
    }
}

@Composable
private fun LibraryGridTab(
    modifier: Modifier = Modifier,
    library: List<SavedSeries>,
    unreadCounts: Map<String, Int>,
    sortOrder: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    onOpenSeries: (SavedSeries) -> Unit
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Library",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = EclipseTextPrimary
            )
            FilterChip(
                selected = true,
                onClick = {
                    val next = when (sortOrder) {
                        SortOrder.TITLE_ASC -> SortOrder.RECENTLY_UPDATED
                        SortOrder.RECENTLY_UPDATED -> SortOrder.UNREAD_DESC
                        SortOrder.UNREAD_DESC -> SortOrder.TITLE_ASC
                    }
                    onSortChange(next)
                },
                label = { Text(sortOrder.name.replace("_", " ")) },
                leadingIcon = { Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(library) { series ->
                val unread = unreadCounts[series.url] ?: 0
                Column(modifier = Modifier.clickable { onOpenSeries(series) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.68f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EclipseSurfaceElevated)
                    ) {
                        AsyncImage(
                            model = series.coverUrl,
                            contentDescription = series.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (unread > 0) {
                            Surface(
                                color = EclipseAccent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                            ) {
                                Text(
                                    "$unread",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        series.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = EclipseTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcesRepoTab(modifier: Modifier = Modifier, onAddRepoClick: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Extensions & Repos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = EclipseTextPrimary
        )
        Text(
            "Add a Mihon/Tachiyomi-compatible index.min.json repository URL or browse installed extension packages.",
            style = MaterialTheme.typography.bodyMedium,
            color = EclipseTextSecondary
        )
        Button(
            onClick = onAddRepoClick,
            colors = ButtonDefaults.buttonColors(containerColor = EclipseAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Repository URL")
        }
    }
}

@Composable
private fun StatsOverviewTab(modifier: Modifier = Modifier, totalSeries: Int, streakDays: Int) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Reading Stats",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = EclipseTextPrimary
        )
        Surface(
            color = EclipseSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalSeries", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EclipseAccent)
                    Text("Series", style = MaterialTheme.typography.labelMedium, color = EclipseTextSecondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$streakDays", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    Text("Day Streak", style = MaterialTheme.typography.labelMedium, color = EclipseTextSecondary)
                }
            }
        }
    }
}
