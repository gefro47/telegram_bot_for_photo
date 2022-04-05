package com.example.telegrambotforphoto.activities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.telegrambotforphoto.*
import kotlinx.android.synthetic.main.settings_activity.*

class AppCompatPreferenceActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        shared = getSharedPreferences(namePreferences , Context.MODE_PRIVATE)
        token_pref_view.setOnClickListener {
            showCustomDialogToken(this)
        }
    }

    override fun onBackPressed() {
        replaceActivity(MainActivity())
    }

    @SuppressLint("CommitPrefEdits")
    fun showCustomDialogToken(context: Context) {
        val builder = AlertDialog.Builder(context)
            .create()
        val view = layoutInflater.inflate(R.layout.dialog_for_add_token,null)
        val buttonCancel = view.findViewById<Button>(R.id.btn_cancel)
        val buttonOk = view.findViewById<Button>(R.id.btn_ok)
        val editText = view.findViewById<EditText>(R.id.edit_text)
        val warning = view.findViewById<TextView>(R.id.warning)
        editText.doAfterTextChanged { warning.visibility = View.INVISIBLE }
        val readToken = shared.getString("token", "")
        if (readToken == "" || readToken == null){
            editText.requestFocus()
        }else{
            editText.setText(readToken)
        }
        builder.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setView(view)
            setCancelable(false)
        }.show()
        builder.setView(view)
        buttonCancel.setOnClickListener {
            builder.dismiss()
        }
        buttonOk.setOnClickListener {
            val token = editText.text.toString()
            val edit = shared.edit()
            if (token != ""){
                edit.putString("token" , editText.text.toString())
                edit.apply()
                edit.commit()
                builder.dismiss()
            }else{
                warning.visibility = View.VISIBLE
            }
        }
        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }
}