package com.example.telegrambotforphoto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.telegrambotforphoto.R
import com.example.telegrambotforphoto.model.ChatId
import kotlinx.android.synthetic.main.client_item.view.*

class ClientAdapter : RecyclerView.Adapter<ClientAdapter.MyViewHolder>() {

    private var listChatId = mutableListOf<ChatId>()

    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.client_item, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = listChatId[position]
        holder.itemView.username.text = "${currentItem.firstName} ${currentItem.lastName}"
        holder.itemView.nickname.text = "${currentItem.nickname}"
        holder.itemView.delete_button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {

            }
        })
    }

    override fun getItemCount(): Int {
        return listChatId.size
    }

    fun setData(listChatId: MutableList<ChatId>){
        this.listChatId = listChatId
        notifyDataSetChanged()
    }
}