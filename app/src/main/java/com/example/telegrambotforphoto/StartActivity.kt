package com.example.telegrambotforphoto

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.telegrambotforphoto.model.Token
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity() : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }

    override fun onResume() {
        super.onResume()
        if (readTokenId() != null){
            edit_text.setText(readTokenId())
            checkBox.isChecked = true
        }else{
            showToast("Enter token!")
        }

        button_next.setOnClickListener {
            if (edit_text.text.toString() != ""
            ){
                if (checkBox.isChecked){
                    writeTokenId(Token(edit_text.text.toString()))
                    BOOLEAN = true
                    replaceActivity(MainActivity())
                }else{
                    BOOLEAN = true
                    replaceActivity(MainActivity())
                }
            }else{
                showToast("Enter token!")
            }
        }
    }
}