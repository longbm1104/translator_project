package com.example.translator_project

import android.util.Log
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorHelper(private val activity: MainActivity) {

    private lateinit var translator: Translator
    private var languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.55f)
            .build()
    )

    fun detectAndTranslate(s: String) {
        activity.binding.translateText.setText("")
        val re = Regex("[\\.,]")
        val noPuncStr = re.replace(s, "").trim() // works
        languageIdentifier.identifyLanguage(noPuncStr).addOnSuccessListener { languageCode ->
            if (languageCode == "und") {
                Log.i("LanguageIdentifier Log Can't Detect", "Can't identify language of " + noPuncStr)
                Toast.makeText(activity, "Language input is not detected", Toast.LENGTH_SHORT).show()
            } else {
                Log.i("LanguageIdentifier Log Detect", "Str(${noPuncStr}) is of Language: $languageCode")
                val langIdx = activity.languageCodes.indexOf(languageCode)

                if (langIdx == -1) {
                    Toast.makeText(activity, "Language $languageCode is not supported", Toast.LENGTH_SHORT).show()
                } else {
                    activity.fromIdx = langIdx
                    activity.fromSpinner.setSelection(activity.fromIdx)
                }

                if (activity.toIdx <= 0) {
                    Toast.makeText(activity, "Please select target language to translate", Toast.LENGTH_SHORT).show()
                }

                if (activity.fromIdx > 0 && activity.toIdx > 0) {
                    Toast.makeText(activity, "Translating (${activity.languageCodes[activity.fromIdx]} -> ${activity.languageCodes[activity.toIdx]})...", Toast.LENGTH_SHORT).show()
                    translateText(activity.languageCodes[activity.fromIdx], activity.languageCodes[activity.toIdx], s)
                }
            }
        }
    }

    private fun translateText(from:String, to:String, text: String) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(from)
            .setTargetLanguage(to)
            .build()
        translator = Translation.getClient(options)
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // Model downloaded successfully. Okay to start translating.
                // (Set a flag, unhide the translation UI, etc.)
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        // Translation successful.
                        activity.binding.translateText.setText(translatedText)
                    }
                    .addOnFailureListener { exception ->
                        // Error.
                        // ...
                        Toast.makeText(activity, "Error to translate", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                // Model couldn’t be downloaded or other internal error.
                // ...
                Log.d("Error Line 162:", exception.toString())
                Toast.makeText(activity, "Error in download model", Toast.LENGTH_SHORT).show()
            }
    }
}