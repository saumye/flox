package ai.flox.home.ui

import ai.flox.arch.Store
import ai.flox.home.model.HomeAction
import ai.flox.home.model.HomeState
import ai.flox.home.model.NewsItem
import ai.flox.state.Action
import ai.flox.state.State
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NewsListScreen(
    stateFlow: StateFlow<HomeState>,
    store: Store<State, Action>
) {
    val state: HomeState by stateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        store.dispatch(HomeAction.RecentNewsRendered)
    }
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (messages) = createRefs()
        val listState = rememberLazyListState()
        state.recentNewsList?.let {
            LazyColumn(
                userScrollEnabled = true,
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(messages) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.fillToConstraints
                    }
            ) {
                items(it.values.toList()) { item ->
                    item.urlToImage?.let {  NewsArticle(item) }
                }
            }
        } ?:  CircularProgressIndicator(
            modifier = Modifier
                .width(64.dp)
                .constrainAs(messages) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                height = Dimension.fillToConstraints
            },
            color = Color.LightGray,
            trackColor = Color.Magenta
        )

    }
}


@Composable
fun NewsArticle(
    message: NewsItem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Start)
        ) {
            AsyncImage(
                model = message.urlToImage,
                contentDescription = message.title,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier.fillMaxSize().height(72.dp),
                color = Color(0x8C000000)
            ) {
                Text(
                    text = message.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

        }
    }
}