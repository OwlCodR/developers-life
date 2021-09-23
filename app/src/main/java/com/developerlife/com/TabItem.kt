package com.developerlife.com

sealed class TabItem(
        val title: String,
        val section: String,
        var posts: MutableSet<Post>,
        var currentPostIndex: Int
    ) {

    object Latest : TabItem("Свежее", "latest", mutableSetOf(), 0)
    object Top : TabItem("Лучшее", "top", mutableSetOf(), 0)
    object Hot : TabItem("Горячее", "hot", mutableSetOf(), 0)
}