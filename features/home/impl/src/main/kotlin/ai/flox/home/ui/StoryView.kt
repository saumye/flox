package ai.flox.home.ui

import ai.flox.arch.Store
import ai.flox.home.model.HomeAction
import ai.flox.home.model.NewsCategory
import ai.flox.home.model.NewsItem
import ai.flox.state.Action
import ai.flox.state.State
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@Composable
fun StoryDialog(
    category: NewsCategory,
    articles: List<NewsItem>,
    onDismiss: () -> Unit,
    onArticleClick: (NewsItem) -> Unit,
    store: Store<State, Action>
) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        StoryView(
            category = category,
            articles = articles,
            onClose = { onDismiss() },
            onArticleClick = onArticleClick,
            store
        )
    }
}

@Composable
fun StoryView(
    category: NewsCategory,
    articles: List<NewsItem>,
    onClose: () -> Unit,
    onArticleClick: (NewsItem) -> Unit,
    store: Store<State, Action>
) {
    // Track current article for click handling
    var currentArticleIndex by remember { mutableStateOf(0) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Use the Stories library to display articles
        Stories(
            numberOfPages = articles.size,
            indicatorBackgroundColor = Color.Gray.copy(alpha = 0.5f),
            indicatorProgressColor = MaterialTheme.colorScheme.primary,
            slideDurationInSeconds = 15,
            touchToPause = true,
            onEveryStoryChange = { index ->
                currentArticleIndex = index
                articles[currentArticleIndex].description?.let { store.dispatch(HomeAction.StartSpeak(it)) }
            },
            onComplete = {
                onClose()
            }
        ) { index ->
            // Create the story content for each article
            ArticleStoryContent(
                article = articles[index],
                onArticleClick = { onArticleClick(articles[index]) },
                onNextClick = { if(index < articles.size-1) onArticleClick(articles[index+1]) },
                onPreviousClick = { if(index > 0) onArticleClick(articles[index-1]) }
            )
        }
        
        // Top navigation and controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = { onClose() },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            IconButton(
                onClick = { /* Share functionality */ },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White
                )
            }
        }

        // Bottom swipe indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Swipe up for details",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Swipe up for details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    
    // Clean up resources when the story view is dismissed
    DisposableEffect(Unit) {
        onDispose {
            // Any cleanup if needed
        }
    }
}

@Composable
fun ArticleStoryContent(
    article: NewsItem,
    onArticleClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { offset ->
                        velocityTracker.resetTracking()
                    },
                    onDrag = { change: PointerInputChange, dragAmount: Offset ->
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        change.consume()
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity()
                        // If the vertical velocity is significant and upward, trigger the article detail
                        if (velocity.y < -1000) {
                            onArticleClick()
                        }
                    }
                )
                
                // Handle tap gestures
                detectTapGestures { offset ->
                    val screenWidth = size.width
                    val x = offset.x
                    if (x < screenWidth / 2) {
                        onPreviousClick()
                    } else {
                        onNextClick()
                    }
                }
            }
    ) {
        // Article image as background
        article.urlToImage?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        // Gradient overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        
        // Article content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            article.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
} 