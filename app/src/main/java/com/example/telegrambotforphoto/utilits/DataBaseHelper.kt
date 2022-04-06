package com.example.telegrambotforphoto.utilits

import android.content.ClipData.Item
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.telegrambotforphoto.model.ChatId


class DataBaseHelper (context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    // below is the method for creating a database by a sqlite query
    override fun onCreate(db: SQLiteDatabase) {
        // below is a sqlite query, where column names
        // along with their data types is given
        val query = ("CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY, " +
                CHAT_ID_COL + " INTEGER," +
                FIRST_NAME_COl + " TEXT," +
                LAST_NAME_COL + " TEXT," +
                NICKNAME_COL + " TEXT" +")")

        // we are calling sqlite
        // method for executing our query
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
        // this method is to check if table already exists
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(db)
    }

    // This method is for adding data in our database
    fun addClient(chatId: ChatId ){

        // below we are creating
        // a content values variable
        val values = ContentValues()

        // we are inserting our values
        // in the form of key-value pair
        values.put(CHAT_ID_COL, chatId.chatId)
        values.put(FIRST_NAME_COl, chatId.firstName)
        values.put(LAST_NAME_COL, chatId.lastName)
        values.put(NICKNAME_COL, chatId.nickname)

        // here we are creating a
        // writable variable of
        // our database as we want to
        // insert value in our database
        val db = this.writableDatabase

        // all values are inserted into database
        db.insert(TABLE_NAME, null, values)

        // at last we are
        // closing our database
        db.close()
    }

    // below method is to get
    // all data from our database
    fun getClientByNickname(nickname: String): ChatId? {

        val query = "SELECT * FROM $TABLE_NAME WHERE $NICKNAME_COL =  \"$nickname\""

        // here we are creating a readable
        // variable of our database
        // as we want to read value from it
        val db = this.readableDatabase


        val cursor = db.rawQuery(query, null)

        var chatId: ChatId? = null
        if (cursor.moveToFirst()) {
            cursor.moveToFirst()

            val Id = Integer.parseInt(cursor.getString(1)).toLong()
            val firstName = cursor.getString(2)
            val lastName = cursor.getString(3)
            chatId = ChatId(Id, firstName, lastName,nickname)
            cursor.close()
        }

        db.close()
        return chatId

    }

    fun deleteClientByNickname(nickname: String): Boolean {
        try {
            val query = "DELETE FROM $TABLE_NAME WHERE $NICKNAME_COL =  \"$nickname\""
            val db = this.writableDatabase
            val whereClause = "$NICKNAME_COL=?"
            val whereArgs = arrayOf(nickname)
            db.delete("$TABLE_NAME", whereClause, whereArgs)
//            val cursor = db.rawQuery(query, null)
//            cursor.close()
//            db.close()
            return true
        }catch (e: Exception){
            return false
        }
    }
    fun getAll(): ArrayList<ChatId> {

        val query = "SELECT * FROM $TABLE_NAME"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        val arrayList = arrayListOf<ChatId>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast()) {
            val Id = Integer.parseInt(cursor.getString(1)).toLong()
            val firstName = cursor.getString(2)
            val lastName = cursor.getString(3)
            val nickname = cursor.getString(4)
            arrayList.add(ChatId(Id, firstName, lastName,nickname)) //add the item
            cursor.moveToNext()
        }
        cursor.close()
        db.close()
        return arrayList

    }


    companion object{
        // here we have defined variables for our database

        // below is variable for database name
        private val DATABASE_NAME = "TELEGRAM_BOT"

        // below is the variable for database version
        private val DATABASE_VERSION = 1

        // below is the variable for table name
        val TABLE_NAME = "client_table"

        // below is the variable for id column
        val ID_COL = "id"

        // below is the variable for id column
        val CHAT_ID_COL = "chatId"

        // below is the variable for firstName column
        val FIRST_NAME_COl = "firstName"

        // below is the variable for lastName column
        val LAST_NAME_COL = "lastName"

        // below is the variable for lastName column
        val NICKNAME_COL = "nickname"
    }
}