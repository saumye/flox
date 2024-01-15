package ai.flox.ui

import ai.flox.BottomTab
import ai.flox.model.AppAction
import ai.flox.state.Action
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController

@Composable
fun BottomBar(tabs: List<BottomTab>, navController: NavController, dispatch: (Action) -> Unit) {

    var navigationSelectedItem by remember { mutableIntStateOf(0) }
    NavigationBar {
        tabs.forEachIndexed { index, navigationItem ->
            NavigationBarItem(
                selected = index == navigationSelectedItem,
                label = {
                    Text(navigationItem.title)
                },
                icon = {
                    Icon(
                        painter = painterResource(id = navigationItem.icon),
                        contentDescription = navigationItem.title
                    )
                },
                onClick = {
                    navigationSelectedItem = index
                    dispatch(AppAction.BottomBarClicked(BottomTab.entries[index], navController))
                }
            )
        }
    }
}