package com.app.zpower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.app.zpower.data.dao.SearchResult
import com.app.zpower.data.repository.DatabaseRepository
import com.app.zpower.ui.navigation.NavigationViewModel
import com.app.zpower.ui.theme.GlassCyan

@Composable
fun SearchResultsOverlay(
    viewModel: NavigationViewModel,
    results: List<SearchResult>,
    onResultClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchPathColorInt by viewModel.searchPathColor.collectAsState()
    val searchPathColor = Color(searchPathColorInt)
    
    Surface(
        modifier = modifier
            .fillMaxSize(),
        color = Color.Black.copy(alpha = 0.9f)
    ) {
        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("NO RESULTS FOUND", style = MaterialTheme.typography.headlineSmall, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(results) { result ->
                    SearchResultItem(
                        result = result, 
                        pathColor = searchPathColor,
                        repository = viewModel.repository,
                        onClick = { onResultClick(result) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    result: SearchResult,
    pathColor: Color,
    repository: DatabaseRepository,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = when (result.type) {
        "thermal_area" -> GlassCyan
        "room" -> Color(0xFF2979FF)
        "panel" -> Color(0xFFFFC400)
        "relay" -> Color(0xFFD500F9)
        else -> Color(0xFF00E676)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        accentColor = accentColor
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (!result.imagePath.isNullOrEmpty()) {
                val file = repository.getLocalImageFile(context, result.imagePath)
                AsyncImage(
                    model = file ?: result.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.type.replace("_", " ").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                if (result.path.isNotEmpty()) {
                    Text(
                        text = result.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = pathColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (result.description.isNotEmpty()) {
                    Text(
                        text = result.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
