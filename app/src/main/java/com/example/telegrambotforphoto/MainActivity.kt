package com.example.telegrambotforphoto

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.elbekD.bot.Bot
import com.elbekD.bot.types.Message
import com.example.telegrambotforphoto.model.ChatId
import com.example.telegrambotforphoto.model.ListChatId
import com.example.telegrambotforphoto.model.Token
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class  MainActivity(val token: Token? = null) : AppCompatActivity() {

    val TAG = MainActivity::class.simpleName
    val CAMERA_REQUEST_RESULT = 1

    private lateinit var textureView: TextureView
    private lateinit var cameraId: String
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size
    private lateinit var videoSize: Size
    private var shouldProceedWithOnResume: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        APP_ACTIVITY=this

        if (!BOOLEAN) {
            replaceActivity(StartActivity())
        }
        var boolean = false
        val receiver:BroadcastReceiver = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.apply {
                    val Token = readUserStatus()?: token?.token
                    val ListChatId = readUsersListChatId()
                    status.text = "Current battery charge\n$currentBatteryCharge%"
                    if (!ListChatId.isNullOrEmpty()) {
                        for (i in ListChatId) {
                            if (Token != null) {
                                if (currentBatteryCharge <= 20 && !boolean && BOOLEAN) {
                                    val bot = Bot.createPolling("RaspBotCam", Token)
                                    bot.start()
                                    bot.sendMessage(i, "Low power 20%!")
                                    bot.stop()
                                    boolean = true
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        textureView = findViewById(R.id.texture_view)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager


        if (!wasCameraPermissionWasGiven()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_RESULT)
            }
        }
        startBackgroundThread()
    }

    val Intent.currentBatteryCharge:Float
        get() {
            // integer containing the maximum battery level
            val scale = getIntExtra(
                BatteryManager.EXTRA_SCALE, -1
            )

            //  integer field containing the current battery
            //  level, from 0 to EXTRA_SCALE
            val level = getIntExtra(
                BatteryManager.EXTRA_LEVEL, -1
            )

            // return current battery charge percentage
            return level * 100 / scale.toFloat()
        }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView.isAvailable && shouldProceedWithOnResume) {
            setupCamera()
        } else if (!textureView.isAvailable){
            textureView.surfaceTextureListener = surfaceTextureListener
        }

        val Token = readUserStatus()?: token?.token

        shouldProceedWithOnResume = !shouldProceedWithOnResume

        if (Token != null) {
            val bot = Bot.createPolling("RaspBotCam", Token)
            bot.onCommand("/photo") { msg, _ ->
                runOnUiThread {
                    val listOfChatId = mutableListOf<Long>()
                    val ListChatId = readUsersListChatId()
                    val texture = texture_view.bitmap!!.rotate(270F)
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
                    basicAlert(msg, bot)
                }
            }
            bot.start()
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
                writeDataChatId(ListChatId(mutableListOf(ChatId(msg.chat.id, msg.chat.first_name.toString(),msg.chat.last_name.toString()))))
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

    fun Bitmap.rotate(degrees:Float = 180F):Bitmap?{
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
            file = File(this.filesDir.toString() + File.separator + fileNameToSave)
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

    private fun setupCamera() {
        val cameraIds: Array<String> = cameraManager.cameraIdList

        for (id in cameraIds) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)

            //If we want to choose the rear facing camera instead of the front facing one
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }

            val streamConfigurationMap : StreamConfigurationMap? = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            if (streamConfigurationMap != null) {
                previewSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(
                    ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
                videoSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(MediaRecorder::class.java).maxByOrNull { it.height * it.width }!!
                imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }
            cameraId = id
        }
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        var width = displayMetrics.widthPixels
        var widthForTextureView: Int
        var heightForTextureView: Int
        val k: Float = previewSize.width.toFloat()/previewSize.height.toFloat()
        widthForTextureView = width
        heightForTextureView = (width*k).toInt()
        textureView.layoutParams = FrameLayout.LayoutParams(
            widthForTextureView, heightForTextureView, Gravity.CENTER
        )
    }

    private fun wasCameraPermissionWasGiven() : Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        {
            return true
        }

        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            surfaceTextureListener.onSurfaceTextureAvailable(textureView.surfaceTexture!!, textureView.width, textureView.height)
        } else {
            Toast.makeText(
                this,
                "Camera permission is needed to run this application",
                Toast.LENGTH_LONG
            )
                .show()
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", this.packageName, null)
                startActivity(intent)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun connectCamera() {
        cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
    }



    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        @SuppressLint("MissingPermission")
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            if (wasCameraPermissionWasGiven()) {
                setupCamera()
                connectCamera()
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {

        }
    }

    /**
     * Camera State Callbacks
     */

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            val surfaceTexture : SurfaceTexture? = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface: Surface = Surface(surfaceTexture)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(listOf(previewSurface, imageReader.surface), captureStateCallback, null)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {

        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            val errorMsg = when(error) {
                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                ERROR_CAMERA_DISABLED -> "Device policy"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                else -> "Unknown"
            }
            Log.e(TAG, "Error when trying to connect camera $errorMsg")
        }
    }

    /**
     * Background Thread
     */
    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraVideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    /**
     * Capture State Callback
     */

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {

        }
        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session

            cameraCaptureSession.setRepeatingRequest(
                captureRequestBuilder.build(),
                null,
                backgroundHandler
            )
        }
    }

    /**
     * ImageAvailable Listener
     */
    val onImageAvailableListener = object: ImageReader.OnImageAvailableListener{
        override fun onImageAvailable(reader: ImageReader) {
            Toast.makeText(this@MainActivity, "Photo Taken!", Toast.LENGTH_SHORT).show()
            val image: Image = reader.acquireLatestImage()
            image.close()
        }
    }
}