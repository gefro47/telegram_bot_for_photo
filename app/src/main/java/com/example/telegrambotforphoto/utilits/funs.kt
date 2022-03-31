package com.example.telegrambotforphoto

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.telegrambotforphoto.model.ChatId
import com.example.telegrambotforphoto.model.ListChatId
import com.example.telegrambotforphoto.model.Token
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@SuppressLint("StaticFieldLeak")
lateinit var APP_ACTIVITY: MainActivity
var BOOLEAN = false

fun AppCompatActivity.replaceActivity(activity: AppCompatActivity){
    val intent = Intent(this, activity::class.java)
    startActivity(intent)
    this.finish()
}

fun writeTokenId(token: Token) {
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
                    writeTokenId(newToken)
                }
            } catch (e: Exception) {
                Log.d("e", e.toString())
                file.delete()
                writeTokenId(token)
            }
        }
    } else {
        val jsonString = Gson().toJson(token)
        file.appendText(jsonString)
    }
}

fun readTokenId(): String? {
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


fun Bitmap.rotate(degrees:Float = 180F): Bitmap?{
    val matrix = Matrix()
    matrix.postRotate(degrees)

    return Bitmap.createBitmap(
        this, // source bitmap
        0, // x coordinate of the first pixel in source
        0, // y coordinate of the first pixel in source
        width, // The number of pixels in each row
        height, // The number of rows
        matrix, // Optional matrix to be applied to the pixels
        false // true if the source should be filtered
    )
}

fun bitmapToFile(bitmap: Bitmap, fileNameToSave: String): File? { // File name like "image.png"
    //create a file to write bitmap data
    var file: File? = null
    return try {
        file = File(APP_ACTIVITY.filesDir.toString() + File.separator + fileNameToSave)
        file.createNewFile()

        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos) // YOU can also save it in JPEG
        val bitmapdata = bos.toByteArray()

        //write the bytes in file
        val fos = FileOutputStream(file)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        file // it will return null
    }
}