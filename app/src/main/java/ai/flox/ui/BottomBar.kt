package ai.flox.ui

import ai.flox.BottomTabs
import ai.flox.R
import ai.flox.model.AppAction
import ai.flox.state.Action
import androidx.annotation.DrawableRes
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
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun BottomBar(tabs: List<BottomTabs>, dispatch: (Action) -> Unit) {

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
                    dispatch(AppAction.Navigate(navigationItem.route))
                }
            )
        }
    }
}