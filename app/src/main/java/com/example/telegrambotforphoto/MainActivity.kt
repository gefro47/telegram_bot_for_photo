package com.example.telegrambotforphoto

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import com.example.telegrambotforphoto.model.ChatId
import com.example.telegrambotforphoto.model.ListChatId
import com.example.telegrambotforphoto.model.Token
import com.example.telegrambotforphoto.utilits.ClientRecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class  MainActivity(val token: Token? = null) : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private val charge = 20


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        APP_ACTIVITY=this
        val booleanLow = MutableLiveData<Boolean>()
        val booleanHigh = MutableLiveData<Boolean>()
        booleanLow.value = true
        booleanHigh.value = true
        if (readTokenId() == null) {
            replaceActivity(StartActivity())
        }
        val receiver:BroadcastReceiver = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.apply {
                    val Token = readTokenId()?: token?.token
                    val ListChatId = readUsersListChatId()
                    status.text = "Current battery charge\n$currentBatteryCharge%"
                    if (!ListChatId.isNullOrEmpty()) {
                        for (i in ListChatId) {
                            if (Token != null) {
                                if (currentBatteryCharge < charge && booleanLow.value == true) {
                                    booleanHigh.value = true
                                    booleanLow.value = false
                                    val bot = Bot.createPolling("", Token)
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
        registerReceiver(receiver,filter)

        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture
                .Builder()
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

        readUsersListChatId()?.let { ClientRecyclerView(applicationContext).setData(it) }

        val bottomSheetBehavior: BottomSheetBehavior<*>?
        val bottomSheet: View = findViewById(R.id.bottom_sheet)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(bottomSheet: View, state: Int) {
                print(state)
                when (state) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
        val rotationKek = MutableLiveData<Float>()

        val orientationEventListener = object : OrientationEventListener(this as Context) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation: Float = when (orientation) {
                    in 45..134 -> 90F
                    in 135..224 -> 180F
                    in 225..314 -> 270F
                    else -> 0F
                }
                rotationKek.value = rotation
            }
        }
        orientationEventListener.enable()

        val Token = readTokenId()?: token?.token

        if (Token != null) {
            val bot = Bot.createPolling("", Token)
            bot.start()
            bot.onCommand("/photo") { msg, _ ->
                runOnUiThread {
                    val listOfChatId = mutableListOf<Long>()
                    val ListChatId = readUsersListChatId()
                    val texture = rotationKek.value?.let { viewFinder.bitmap!!.rotate(it) }
                    if(ListChatId != null) {
                        for (i in ListChatId.indices){
                            listOfChatId.add(ListChatId[i].chatId)
                        }
                        if (listOfChatId.contains(msg.chat.id)) {
                            bot.sendPhoto(msg.chat.id, bitmapToFile(texture!!, "kek.png")!!)
                        } else {
                            bot.sendMessage(msg.chat.id, "It`s private chat!")
                        }
                    }else {
                        bot.sendMessage(msg.chat.id, "It`s private chat!")
                    }
                }
            }
            bot.onCommand("/add") { msg, _ ->
                runOnUiThread {
                    val listOfChatId = mutableListOf<Long>()
                    val ListChatId = readUsersListChatId()
                    if(ListChatId != null) {
                        for (i in ListChatId.indices){
                            listOfChatId.add(ListChatId[i].chatId)
                        }
                        if (listOfChatId.contains(msg.chat.id)) {
                            bot.sendMessage(msg.chat.id, "You`re already added!")
                        } else {
                            basicAlert(msg, bot)
                        }
                    }else {
                        basicAlert(msg, bot)
                    }
                }
            }
        }
    }


    fun basicAlert(msg: Message, bot: Bot){

        val builder = AlertDialog.Builder(this)

        with(builder)
        {
            setTitle("New client!")
            setMessage("Add client ${msg.from}?")
            setCancelable(false)
            setPositiveButton("Yes") { dialog, id ->
                writeDataChatId(ListChatId(mutableListOf(ChatId(msg.chat.id, msg.chat.first_name.toString(),msg.chat.last_name.toString(), "@${msg.chat.username.toString()}"))))
                readUsersListChatId()?.let { ClientRecyclerView(applicationContext).setData(it) }
                bot.sendMessage(msg.chat.id, "You`re added!")
            }
            setNegativeButton("No") { dialog, id ->
                Toast.makeText(
                    APP_ACTIVITY, "Perhaps you`re right",
                    Toast.LENGTH_LONG
                ).show()
                bot.sendMessage(msg.chat.id, "Decline!")
            }
            show()
        }
    }
}