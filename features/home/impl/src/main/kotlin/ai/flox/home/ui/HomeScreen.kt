package ai.flox.home.ui

import ai.flox.arch.Store
import ai.flox.home.model.HomeAction
import ai.flox.home.model.HomeState
import ai.flox.home.model.NewsCategory
import ai.flox.home.model.NewsItem
import ai.flox.state.Action
import ai.flox.state.State
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun HomeScreen(
    stateFlow: StateFlow<HomeState>,
    store: Store<State, Action>,
) {
    val state: HomeState by stateFlow.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    // State to control Story dialog visibility
    var showStoryDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<NewsCategory?>(null) }
    
    // Load initial data
    LaunchedEffect(Unit) {
        store.dispatch(HomeAction.RecentNewsRendered)
        store.dispatch(HomeAction.LoadTopStories)
        for (category in state.categories) {
            store.dispatch(HomeAction.SelectCategory(category))
        }
    }
    
    // Set up pager state for swiping between categories
    val pagerState = rememberPagerState(
        initialPage = state.categories.indexOf(state.selectedCategory).coerceAtLeast(0)
    )
    
    // Set up scroll behavior for collapsible top app bar
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    // Sync pager with selected category
    LaunchedEffect(state.selectedCategory) {
        val index = state.categories.indexOf(state.selectedCategory)
        if (index != pagerState.currentPage && index >= 0) {
            pagerState.scrollToPage(index)
        }
    }
    
    // Sync selected category with pager
    LaunchedEffect(pagerState.currentPage) {
        val category = state.categories.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (category != state.selectedCategory) {
            store.dispatch(HomeAction.SelectCategory(category))
        }
    }
    
    // Show Story Dialog if a category is selected
    if (showStoryDialog && selectedCategory != null) {
        val articlesForCategory = state.categorizedNews[selectedCategory]?.values?.toList() ?: emptyList()
        if (articlesForCategory.isNotEmpty()) {
            StoryDialog(
                category = selectedCategory!!,
                articles = articlesForCategory,
                onDismiss = { 
                    showStoryDialog = false 
                    selectedCategory = null
                },
                onArticleClick = { article ->
                    // Open full article detail
                    showStoryDialog = false
                    selectedCategory = null
                    store.dispatch(Action.Navigate(DetailRoutes.DETAIL))
                },
                store
            )
        }
    }
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Daily") },
                actions = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Handle chat FAB click */ },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Chat"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Collapsible story circles based on scroll state
            val storyCirclesVisible = scrollBehavior.state.collapsedFraction < 0.5
            AnimatedVisibility(
                visible = storyCirclesVisible,
                enter = expandVertically(animationSpec = tween(durationMillis = 200)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 200))
            ) {
                CategoryStoryCircles(
                    categories = state.categories,
                    onCategorySelected = { category ->
                        // Show story view when a category is clicked
                        selectedCategory = category
                        showStoryDialog = true
                    }
                )
            }
            
            // Category Pills - now scrollable
            CategoryPills(
                state = state,
                pagerState = pagerState,
                onCategorySelected = { category ->
                    coroutineScope.launch {
                        val index = state.categories.indexOf(category)
                        if (index >= 0) {
                            pagerState.scrollToPage(index)
                        }
                        store.dispatch(HomeAction.SelectCategory(category))
                    }
                }
            )
            
            // Swipeable category content
            HorizontalPager(
                count = state.categories.size,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val category = state.categories[page]
                val categoryNews = state.categorizedNews[category]
                
                if (categoryNews != null && categoryNews.isNotEmpty()) {
                    // Pass scroll behavior to the news list to enable coordinated scrolling
                    NewsList(
                        news = categoryNews.values.toList(),
                        onArticleClick = navigateToDetail
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryStoryCircles(
    categories: List<NewsCategory>,
    onCategorySelected: (NewsCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { category ->
                CategoryStoryCircle(category, onCategorySelected)
            }
        }
    }
}

@Composable
fun CategoryStoryCircle(
    category: NewsCategory,
    onCategorySelected: (NewsCategory) -> Unit
) {
    // Define gradient colors for the category circle border
    val gradientColors = when (category) {
        NewsCategory.TOP -> listOf(Color(0xFF9C27B0), Color(0xFF2196F3))
        NewsCategory.BUSINESS -> listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
        NewsCategory.ENTERTAINMENT -> listOf(Color(0xFFFF9800), Color(0xFFFF5722))
        NewsCategory.HEALTH -> listOf(Color(0xFF2196F3), Color(0xFF03A9F4))
        NewsCategory.SCIENCE -> listOf(Color(0xFF9C27B0), Color(0xFFE91E63))
        NewsCategory.SPORTS -> listOf(Color(0xFFFF5722), Color(0xFFF44336))
        NewsCategory.TECHNOLOGY -> listOf(Color(0xFF3F51B5), Color(0xFF2196F3))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(gradientColors),
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onCategorySelected(category) }
        ) {
            // Category icon or first letter of category name
            Text(
                text = category.displayName.first().toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = gradientColors[0],
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun CategoryPills(
    state: HomeState,
    pagerState: com.google.accompanist.pager.PagerState,
    onCategorySelected: (NewsCategory) -> Unit
) {
    val selectedCategoryIndex = pagerState.currentPage
    
    ScrollableTabRow(
        selectedTabIndex = selectedCategoryIndex,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (selectedCategoryIndex < tabPositions.size) {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        state.categories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedCategoryIndex,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = category.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun NewsList(news: List<NewsItem>, onArticleClick: (NewsItem) -> Unit = {}) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp)
    ) {
        items(news) { newsItem ->
            NewsCard(newsItem, onArticleClick)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NewsCard(newsItem: NewsItem, onArticleClick: (NewsItem) -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onArticleClick(newsItem) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // News content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = newsItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = newsItem.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    Text(
                        text = "3h", // Placeholder, should be calculated from publishedAt
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            // News image
            newsItem.urlToImage?.let {
                AsyncImage(
                    model = it,
                    contentDescription = newsItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp)
                )
            }
        }
    }
} 