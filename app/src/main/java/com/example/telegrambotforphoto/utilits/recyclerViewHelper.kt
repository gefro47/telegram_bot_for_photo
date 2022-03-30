package com.example.telegrambotforphoto.utilits

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.telegrambotforphoto.*
import com.example.telegrambotforphoto.R
import com.example.telegrambotforphoto.adapters.ClientAdapter
import com.example.telegrambotforphoto.model.ChatId
import com.example.telegrambotforphoto.model.ListChatId

class ClientRecyclerView(private val requireContext: Context){
    private val adapter = ClientAdapter()

    fun setData(listChatId: MutableList<ChatId>){
        adapter.setData(listChatId)
        val recyclerView = APP_ACTIVITY.findViewById<RecyclerView>(R.id.client_recycler_view)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext)
    }
}