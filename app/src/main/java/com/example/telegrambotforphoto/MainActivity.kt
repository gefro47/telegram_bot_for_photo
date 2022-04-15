package com.example.telegrambotforphoto

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import com.example.telegrambotforphoto.adapters.ClientAdapter
import com.example.telegrambotforphoto.databinding.ActivityMainBinding
import com.example.telegrambotforphoto.model.ChatId
import com.example.telegrambotforphoto.utilits.DataBaseHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class  MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private val listChatId = arrayListOf<ChatId>()
    private val listOfAdded = arrayListOf<Long>()
    private val adapter = ClientAdapter(listChatId, listOfAdded)
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var alertDialog: AlertDialog.Builder
    private val charge = 20
    private val token = MutableLiveData<String>()
    private val booleanDescription = MutableLiveData<Boolean>()

    private val booleanLow = MutableLiveData<Boolean>()
    private val booleanHigh = MutableLiveData<Boolean>()

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageCapture!!.targetRotation = rotation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        supportActionBar?.hide()
        APP_ACTIVITY=this
        shared = getSharedPreferences(namePreferences , Context.MODE_PRIVATE)
        booleanLow.value = true
        booleanHigh.value = true
        token.value = shared.getString("token", "")
        booleanDescription.value = shared.getBoolean("description", false)
        val token = token.value
        alertDialog = AlertDialog.Builder(this)
        if (token == "" || token == null) {
            showCustomDialogToken()
        }

        if (!booleanDescription.value!!) showCustomDialogDescription()

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        preferences_of_bot.setOnClickListener{
            showCustomDialogToken()
        }

        application_info.setOnClickListener {
            showCustomDialogDescription()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(RATIO_16_9)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture
                .Builder()
                .setTargetAspectRatio(RATIO_16_9)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // creates a folder inside internal storage
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    // checks the camera permission
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // If all permissions granted , then start Camera
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // If permissions are not granted,
                // present a toast to notify the user that
                // the permissions were not granted.
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXGFG"
        private const val REQUEST_CODE_PERMISSIONS = 20
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationEventListener.disable()
        cameraExecutor.shutdown()
    }

    val Intent.currentBatteryCharge:Float
        get() {
            val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            return level * 100 / scale.toFloat()
        }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        val bottomSheetBehavior: BottomSheetBehavior<*>?
        val bottomSheet: View = findViewById(R.id.bottom_sheet)
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        alertDialog = AlertDialog.Builder(this)
        listChatId.clear()
        listChatId.addAll(getList())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "token") {
                token.value = shared.getString("token", "")
                battery()
                telegramBot()
            }
        }
        shared.registerOnSharedPreferenceChangeListener(listener)
        val recyclerView = this.findViewById<RecyclerView>(R.id.client_recycler_view)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        recyclerView.layoutParams.width = width
        recyclerView.layoutParams.height = (1.4*width).toInt()
        if (token.value != "" || token.value != null){
            battery()
            telegramBot()
        }

        bottomSheetBehavior = from(bottomSheet)
        bottomSheetBehavior.setBottomSheetCallback(object: BottomSheetCallback(){
            override fun onStateChanged(bottomSheet: View, state: Int) {
                print(state)
                when (state) {
                    STATE_HIDDEN -> {
                        bottomSheetBehavior.state = STATE_COLLAPSED
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
        orientationEventListener.enable()

    }
    private fun telegramBot(){
        val Token = token.value

        if (Token != null && Token != "") {
            val bot = Bot.createPolling("", Token)
            bot.start()
            bot.onCommand("/photo") { msg, _ ->
                val list = getList()
                val listOfChatId = mutableListOf<Long>()
                for (i in list.indices){
                    listOfChatId.add(list[i].chatId)
                }
                if (listOfChatId.contains(msg.chat.id)) {
                    runOnUiThread {
                        try {
                            val photoFile = File(outputDirectory, "keka.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            imageCapture?.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(APP_ACTIVITY),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onError(exc: ImageCaptureException) {
                                        Log.e("Keka", "Photo capture failed: ${exc.message}", exc)
                                    }

                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        bot.sendPhoto(msg.chat.id, photoFile)
                                    }
                                })
                        }catch (e: java.lang.Exception){
                            Log.i("Keka", e.message.toString())
                        }
                    }
                } else {
                    bot.sendMessage(msg.chat.id, "It`s private chat!")
                }
            }
            bot.onCommand("/add") { msg, _ ->
                val list = getList()
                val listOfChatId = mutableListOf<Long>()
                for (i in list.indices){
                    listOfChatId.add(list[i].chatId)
                }
                if (listOfChatId.contains(msg.chat.id)) {
                    bot.sendMessage(msg.chat.id, "You`re already added!")
                } else {
                    runOnUiThread {
                        showCustomDialogAdd(msg, bot)
                    }
                }
            }
        }
    }

    private fun showCustomDialogAdd(msg: Message, bot: Bot) {
        val builder = AlertDialog.Builder(APP_ACTIVITY)
            .create()
        val view = layoutInflater.inflate(R.layout.dialog_for_add_client,null)
        val buttonCancel = view.findViewById<Button>(R.id.btn_cancel)
        val buttonOk = view.findViewById<Button>(R.id.btn_ok)
        val answer = view.findViewById<TextView>(R.id.answer_text_view)
        if (!listOfAdded.contains(msg.chat.id)) {
            builder.apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setView(view)
                setCancelable(false)
            }.show()
        }else{
            return
        }
        listOfAdded.add(msg.chat.id)
        answer.text = "Add client ${msg.chat.first_name} ${msg.chat.last_name} @${msg.chat.username}?"
        builder.setView(view)
        buttonCancel.setOnClickListener {
            showToast("Perhaps you`re right")
            bot.sendMessage(msg.chat.id, "Decline!")
            builder.dismiss()
        }
        buttonOk.setOnClickListener {
            val db = DataBaseHelper(this, null)
            db.addClient(ChatId(msg.chat.id, msg.chat.first_name.toString(),msg.chat.last_name.toString(), "@${msg.chat.username.toString()}"))
            val index = listChatId.lastIndex + 1
            listChatId.add(index, db.getClientByNickname("@${msg.chat.username.toString()}")!!)
            adapter.notifyItemInserted(index)
            bot.sendMessage(msg.chat.id, "You`re added!")
            builder.dismiss()
        }
        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    @SuppressLint("CommitPrefEdits")
    private fun showCustomDialogToken() {
        val builder = alertDialog
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
            finish()
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

    private fun showCustomDialogDescription() {
        val builder = alertDialog
            .create()
        val view = layoutInflater.inflate(R.layout.dialog_for_description,null)
        val buttonOk = view.findViewById<Button>(R.id.btn_ok)
        val text = view.findViewById<TextView>(R.id.text)
        text.setText(R.string.description)
        builder.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setView(view)
            setCancelable(false)
        }.show()
        builder.setView(view)
        buttonOk.setOnClickListener {
            val edit = shared.edit()
            edit.putBoolean("description", true)
            edit.apply()
            builder.dismiss()
        }
        builder.setCanceledOnTouchOutside(false)
        builder.show()
    }

    private fun battery(){
        val token = token.value
        val receiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.apply {
                    val list = getList()
//                    status.text = "Current battery charge\n$currentBatteryCharge%"
                    if (!list.isNullOrEmpty()) {
                        for (i in list) {
                            if (token != null) {
                                if (currentBatteryCharge < charge && booleanLow.value == true) {
                                    booleanHigh.value = true
                                    booleanLow.value = false
                                    val bot = Bot.createPolling("", token)
                                    bot.start()
                                    bot.sendMessage(i.chatId, "Battery charge below 20%")
                                    bot.stop()
                                }
                                if (currentBatteryCharge >= charge && booleanHigh.value == true) {
                                    booleanHigh.value = false
                                    booleanLow.value = true
                                }
                            }
                        }
                    }
                }
            }
        }

        // initialize a new intent filter instance
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        // register the broadcast receiver
        registerReceiver(receiver, filter)
    }

    private fun getList(): ArrayList<ChatId> {
        val db = DataBaseHelper(this, null)
        return db.getAll()
    }
}
