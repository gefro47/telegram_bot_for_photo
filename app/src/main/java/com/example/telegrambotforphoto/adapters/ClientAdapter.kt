package com.example.telegrambotforphoto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.telegrambotforphoto.APP_ACTIVITY
import com.example.telegrambotforphoto.R
import com.example.telegrambotforphoto.model.ChatId
import com.example.telegrambotforphoto.utilits.DataBaseHelper
import kotlinx.android.synthetic.main.client_item.view.*

class ClientAdapter(private val listChatId: ArrayList<ChatId>) : RecyclerView.Adapter<ClientAdapter.MyViewHolder>() {

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
                deleteItem(listChatId.indexOf(currentItem), currentItem.nickname)
            }
        })
    }

    override fun getItemCount(): Int {
        return listChatId.size
    }

    fun deleteItem(index: Int, nickname: String){
        listChatId.removeAt(index)
        val db = DataBaseHelper(APP_ACTIVITY, null)
        if (db.deleteClientByNickname(nickname)) notifyItemRemoved(index)
    }
}