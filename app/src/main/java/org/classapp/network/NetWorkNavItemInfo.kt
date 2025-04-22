package org.classapp.network

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

data class NetWorkNavItemInfo(
    val label:String = "",
    val icon: ImageVector = Icons.Filled.Star,
    val route:String = "",
) {
    fun getAllNavItems() : List<NetWorkNavItemInfo> {
        return listOf(
            NetWorkNavItemInfo("My Card", Icons.Filled.Face, DestinationScreens.MyCard.route),
            NetWorkNavItemInfo("Discovery", Icons.Filled.Search, DestinationScreens.Discovery.route),
            NetWorkNavItemInfo("Contacts", Icons.Filled.AccountBox, DestinationScreens.Contacts.route),
            NetWorkNavItemInfo("Settings", Icons.Filled.Menu, DestinationScreens.Settings.route)
        )
    }
}