package com.example.telegrambotforphoto

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.widget.Toast


@SuppressLint("StaticFieldLeak")
lateinit var APP_ACTIVITY: MainActivity
lateinit var shared : SharedPreferences
const val namePreferences = "TelegramBot"

fun showToast(message: String){
    Toast.makeText(APP_ACTIVITY, message, Toast.LENGTH_SHORT).show()
}