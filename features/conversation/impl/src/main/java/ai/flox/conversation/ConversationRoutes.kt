package ai.flox.conversation

import ai.flox.Navigable
import ai.flox.conversation.ui.ConversationListScreen
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

object ConversationRoutes : Navigable {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController,
        modifier: Modifier
    ) {
        navGraphBuilder.composable("conversations") {
            ConversationListScreen(modifier = modifier, state = conversationState, dispatchEvent = dispatchEvent)
        }
    }
}