package com.example.telegrambotforphoto.model

data class ChatId (
    val chatId: Long,
    val firstName: String,
    val lastName: String
)

data class ListChatId(
    val listChatId: MutableList<ChatId>
)