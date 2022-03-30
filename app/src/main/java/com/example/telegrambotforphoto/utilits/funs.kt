package com.example.telegrambotforphoto

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.telegrambotforphoto.model.ChatId
import com.example.telegrambotforphoto.model.ListChatId
import com.example.telegrambotforphoto.model.Token
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream

@SuppressLint("StaticFieldLeak")
lateinit var APP_ACTIVITY: MainActivity
var BOOLEAN = false

fun AppCompatActivity.replaceActivity(activity: AppCompatActivity){
    val intent = Intent(this, activity::class.java)
    startActivity(intent)
    this.finish()
}

fun writeDataDemoStatus(token: Token) {
    val path = APP_ACTIVITY.filesDir
    val file = File(path, "Token.txt")
    if (file.isFile) {
        val inputAsString = FileInputStream(file).bufferedReader().use {
            it.readText()
        }
        if (inputAsString == "") {
            val jsonString = Gson().toJson(token)
            file.appendText(jsonString)
        } else {
            try {
                val tokenFromFile = Gson().fromJson(inputAsString, Token::class.java)
                if (token.token != tokenFromFile.token) {
                    val newToken = Token(token.token)
                    file.delete()
                    writeDataDemoStatus(newToken)
                }
            } catch (e: Exception) {
                Log.d("e", e.toString())
                file.delete()
                writeDataDemoStatus(token)
            }
        }
    } else {
        val jsonString = Gson().toJson(token)
        file.appendText(jsonString)
    }
}

fun readUserStatus(): String? {
    val path = APP_ACTIVITY.filesDir
    val file = File(path, "Token.txt")
    if (file.isFile) {
        val inputAsString = FileInputStream(file).bufferedReader().use {
            it.readText()
        }
        try {
            val tokenFromFile = Gson().fromJson(inputAsString, Token::class.java)
            return tokenFromFile.token
        } catch (e: Exception) {
            Log.d("e", e.toString())
            return null
        }
    } else {
        return null
    }
}

fun writeDataChatId(ListChatId: ListChatId) {
    val path = APP_ACTIVITY.filesDir
    val file = File(path, "ChatId.txt")
    if (file.isFile) {
        val inputAsString = FileInputStream(file).bufferedReader().use {
            it.readText()
        }
        if (inputAsString == "") {
            val jsonString = Gson().toJson(ListChatId)
            file.appendText(jsonString)
        } else {
            try {
                val ListChatIdFromFile = Gson().fromJson(inputAsString, ListChatId::class.java)
                if (!ListChatIdFromFile.listChatId.contains(ListChatId.listChatId[0])) {
                    ListChatIdFromFile.listChatId.add(ListChatId.listChatId[0])
                    file.delete()
                    writeDataChatId(ListChatIdFromFile)
                }
            } catch (e: Exception) {
                Log.d("e", e.toString())
                file.delete()
                writeDataChatId(ListChatId)
            }
        }
    } else {
        val jsonString = Gson().toJson(ListChatId)
        file.appendText(jsonString)
    }
}

fun readUsersListChatId(): MutableList<ChatId>? {
    val path = APP_ACTIVITY.filesDir
    val file = File(path, "ChatId.txt")
    if (file.isFile) {
        val inputAsString = FileInputStream(file).bufferedReader().use {
            it.readText()
        }
        try {
            val chatIdFromFile = Gson().fromJson(inputAsString, ListChatId::class.java)
            return chatIdFromFile.listChatId
        } catch (e: Exception) {
            Log.d("e", e.toString())
            return null
        }
    } else {
        return null
    }
}

fun showToast(message: String){
    Toast.makeText(APP_ACTIVITY, message, Toast.LENGTH_SHORT).show()
}