package com.example.translator_project

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognitionHelper(private val activity: MainActivity) {

    /**
     * Runs text recognition on the given bitmap image.
     *
     * @param bitmap The bitmap image to perform text recognition on.
     */
    fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener(
                OnSuccessListener<Any?> { texts ->
                    processTextRecognitionResult(texts as Text)
                })
            .addOnFailureListener(
                OnFailureListener { e -> // Task failed with an exception
                    e.printStackTrace()
                })
    }

    /**
     * Processes the result of text recognition and displays the detected text.
     *
     * @param texts The result of text recognition.
     */
    private fun processTextRecognitionResult(texts: Text) {
        val blocks: List<Text.TextBlock> = texts.textBlocks
        if (blocks.isEmpty()) {
            Utils().displayToastMessage(activity, "No text detected!")
            return
        }

        // Process through line of block of detected text in the image
        var textToDisplay = ""
        for (i in blocks.indices) {
            val lines: List<Text.Line> = blocks[i].lines
            for (j in lines.indices) {
                val elements: List<Text.Element> = lines[j].elements
                for (k in elements.indices) {
                    Log.d("Text Recognized:", elements[k].text)
                    textToDisplay += elements[k].text + " "
                }
                textToDisplay = textToDisplay.trim()
                textToDisplay += "\n"
            }
        }
        Log.d("Text Recognized:", textToDisplay.trim())
        activity.binding.inputText.setText(textToDisplay)
    }
}