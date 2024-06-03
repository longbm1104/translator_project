package com.example.translator_project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.translator_project.databinding.ActivityMainBinding
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var photoBtn: ImageButton
    private lateinit var micBtn: ImageButton
    private lateinit var cameraBtn: ImageButton
    lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner

    private lateinit var translatorHelper: TranslatorHelper
    private lateinit var textRecognitionHelper: TextRecognitionHelper

    private var delay: Long = 1000 // 1 seconds after user stops typing
    private var lastTextEdit: Long = 0
    private var handler = Handler()

    private lateinit var currentPhotoPath: String
    var fromIdx: Int = -1
    var toIdx: Int = -1
    var languages: ArrayList<String> = arrayListOf("Select Language", "English", "French", "Vietnamese", "Spanish", "German", "Portuguese", "Italian")
    var languageCodes: ArrayList<String> = arrayListOf("detect", "en", "fr", "vi", "es", "de", "pt", "it")

    private var activityResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bitmap = uriToBitmap(uri)
            if (bitmap != null) {
                textRecognitionHelper.runTextRecognition(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        Utils().hideSystemUI(this)

        translatorHelper = TranslatorHelper(this)
        textRecognitionHelper = TextRecognitionHelper(this)

        photoBtn = binding.uploadImage
        micBtn = binding.mic
        cameraBtn = binding.camera
        fromSpinner = binding.fromLanguageChoices
        toSpinner = binding.toLanguageChoices

        val adapter = ArrayAdapter(this, R.layout.custom_spinner_item, languages)
        fromSpinner.adapter = adapter
        toSpinner.adapter = adapter

        fromSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Your code when an item is selected
                fromIdx = position
                if (position != 0) {
                    val item = parent!!.getItemAtPosition(position).toString()
//                    Toast.makeText(this@MainActivity, "Selected Item:" + item, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Your code when nothing is selected
            }
        }

        fromSpinner.isEnabled = false

        toSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Your code when an item is selected
                toIdx = position
                if (position != 0) {
                    val item = parent!!.getItemAtPosition(position).toString()
//                    Toast.makeText(this@MainActivity, "Selected Item:" + item, Toast.LENGTH_SHORT).show()
                    if (binding.inputText.text.toString() != "") {
                        translatorHelper.detectAndTranslate(binding.inputText.text.toString().trim())
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Optional: Your code when nothing is selected
            }
        }

        photoBtn.setOnClickListener {
            toggleBtn(photoBtn, micBtn, cameraBtn, binding.photoText, binding.micText, binding.cameraText)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Request camera permission if not granted
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PHOTO_LIB_REQUEST_CODE)
            } else {
                selectImage()
            }
        }

        micBtn.setOnClickListener {
            Log.d("Line 116 Log", Locale.getAvailableLocales().toString())
            toggleBtn(micBtn, photoBtn, cameraBtn, binding.micText, binding.photoText, binding.cameraText)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_CODE)
            } else {
                askSpeechInput()
            }
        }

        cameraBtn.setOnClickListener {
            toggleBtn(cameraBtn, micBtn, photoBtn, binding.cameraText, binding.micText, binding.photoText)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            } else {
                openCamera()
            }
        }

        binding.inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This method is called to notify you that the characters within start and start + before are about to be replaced with new text with length after.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called to notify you that somewhere within s, the text has been replaced with new text with length count
                handler.removeCallbacks(inputFinishChecker)
            }

            override fun afterTextChanged(s: Editable?) {
                // This method is called to notify you that somewhere within s, the text has been changed.
                if (s.toString().trim() != "") {
                    lastTextEdit = System.currentTimeMillis()
                    handler.postDelayed(inputFinishChecker, delay)
                } else {
                    binding.translateText.setText("")
                }
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
//        Utils().hideSystemUI(this)
    }

    private fun toggleBtn(btn1: ImageButton, btn2: ImageButton, btn3: ImageButton,
                          txt1: TextView, txt2: TextView, txt3: TextView) {
        btn1.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#304674"))
        txt1.setTextColor(ColorStateList.valueOf(Color.parseColor("#304674")))
        btn2.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B2CBDE"))
        txt2.setTextColor(ColorStateList.valueOf(Color.parseColor("#B2CBDE")))
        btn3.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B2CBDE"))
        txt3.setTextColor(ColorStateList.valueOf(Color.parseColor("#B2CBDE")))
    }

    private fun selectImage() {
        activityResultLauncher.launch("image/*")
    }

    private fun openCamera() {
        try {
            val photoFile = createPhotoFile()
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.example.translator_project.fileprovider",
                photoFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            val packageManager = this.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, IMAGE_CAPTURE_CODE)
                //The startActivityForResult() method is deprecated in favor of the Activity Result API, which provides a more modern and flexible approach for handling the result returned by an activity.
            }
        } catch (ex: Exception) {
            Utils().displayToastMessage(this, "Error occurred while creating the File")
        }
    }


    private fun rotateImageIfRequired(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f) // Rotate by 90 degrees clockwise

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun createPhotoFile(): File {
        val fileName = "photo" + SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(fileName, ".jpg", storageDirectory).apply {
            currentPhotoPath = absolutePath
        }
//        Log.d("image file created:", "$imageFile")
        currentPhotoPath = imageFile.absolutePath

        return imageFile
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission was granted, open the camera
            askSpeechInput()
        }
        if (requestCode == PHOTO_LIB_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImage()
        }
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission was granted, open the camera
            openCamera()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        Utils().hideSystemUI(this)
        if (requestCode == RQ_SPEECH_REC && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val handler = Handler(Looper.getMainLooper())
            val runnable = Runnable {
                // Code to be executed after a delay
                binding.inputText.setText(result?.get(0).toString())
            }
            handler.postDelayed(runnable, 500)
        } else if (resultCode == RESULT_OK && requestCode == IMAGE_CAPTURE_CODE) {
            // Process and display the captured image
            if (::currentPhotoPath.isInitialized) {
                val imageBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                val rotatedBitmap = rotateImageIfRequired(imageBitmap)
                textRecognitionHelper.runTextRecognition(rotatedBitmap)
            } else {
                Utils().displayToastMessage(this, "Something went wrong with text detector. Please try again")
                Log.e("MainActivity", "currentPhotoPath is not initialized")
            }
        }
    }

    private fun askSpeechInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Utils().displayToastMessage(this, "Speech recognition is not available")
//            Toast.makeText(this, "Speech recognition is not available", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something!")

            startActivityForResult(i, RQ_SPEECH_REC)
        }
    }

    private val inputFinishChecker = Runnable {
        if (System.currentTimeMillis() > lastTextEdit + delay - 500) {
            if (binding.inputText.text.toString() != "") {
                translatorHelper.detectAndTranslate(binding.inputText.text.toString().trim())
            }
        }
    }

    companion object {
        // Request codes for camera and permissions
        private const val IMAGE_CAPTURE_CODE = 1002
        private const val CAMERA_REQUEST_CODE = 1003
        private const val PHOTO_LIB_REQUEST_CODE = 1004
        private const val RQ_SPEECH_REC = 102
        private const val RECORD_AUDIO_CODE = 103
    }
}