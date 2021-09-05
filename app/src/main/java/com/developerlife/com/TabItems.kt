package com.developerlife.com

import androidx.compose.runtime.Composable

sealed class TabItem(
        val title: String,
        val section: String,
        var posts: MutableList<Post>,
        var lastPostIndex: Int  // Index of the post in MutableList<Post>
    ) {

    object Latest : TabItem("Свежее", "latest", mutableListOf(), 0)
    object Top : TabItem("Лучшее", "top", mutableListOf(), 0)
    object Hot : TabItem("Горячее", "hot", mutableListOf(), 0)
}