package org.classapp.network

sealed class DestinationScreens(val route : String) {
    object MyCard : DestinationScreens("myCard")
    object Discovery : DestinationScreens("discovery")
    object Contacts : DestinationScreens("contacts")
    object Settings : DestinationScreens("settings")
}