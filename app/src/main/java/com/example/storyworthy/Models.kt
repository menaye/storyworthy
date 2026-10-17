package com.example.storyworthy

//data class Entry(
//    val title: String,
//    val content: String,
//    val time: String,
//    val sentiment: Sentiment,
//    val tags: List<String>,
//    val isStarred: Boolean = false
//)

data class DailyEntries(
    val date: String,
    val entries: List<Entry>
)

data class Quote(
    val text: String,
    val author: String
)