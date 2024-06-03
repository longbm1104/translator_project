package com.example.translator_project

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateRemoteModel
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

    init {
        downloadTranslationModels()
    }

    private fun downloadTranslationModels() {
        val modelManager = RemoteModelManager.getInstance()
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                // ...
                val downloadedLanguages = models.map { it.language }
                for (languageCode in activity.languageCodes) {
                    if (languageCode != "detect" && !downloadedLanguages.contains(languageCode)) {
                        val model = TranslateRemoteModel.Builder(languageCode).build()
                        val conditions = DownloadConditions.Builder()
                            .requireWifi()
                            .build()
                        modelManager.download(model, conditions)
                            .addOnSuccessListener {
                                // Model downloaded.
                                Log.i("Download Translation Model", "Successufully download translation for $languageCode")
                            }
                            .addOnFailureListener {
                                // Error.
                                Log.i("Download Translation Model", "Unsuccessufully download translation for $languageCode")
                            }
                    }
                }
            }
            .addOnFailureListener {
                // Error.
            }
    }

    fun detectAndTranslate(s: String) {
        activity.binding.translateText.setText("")
        val re = Regex("[\\.,]")
        val noPuncStr = re.replace(s, "").trim() // works
        languageIdentifier.identifyLanguage(noPuncStr).addOnSuccessListener { languageCode ->
            if (languageCode == "und") {
                Log.i("LanguageIdentifier Log Can't Detect", "Can't identify language of " + noPuncStr)
                Utils().displayToastMessage(activity, "Language input is not detected")
            } else {
                Log.i("LanguageIdentifier Log Detect", "Str(${noPuncStr}) is of Language: $languageCode")
                val langIdx = activity.languageCodes.indexOf(languageCode)

                if (langIdx == -1) {
                    Utils().displayToastMessage(activity, "Language $languageCode is not supported")
                } else {
                    activity.fromIdx = langIdx
                    activity.fromSpinner.setSelection(activity.fromIdx)
                }

                if (activity.toIdx <= 0) {
                    Utils().displayToastMessage(activity, "Please select target language to translate")
                }

                if (activity.fromIdx > 0 && activity.toIdx > 0) {
                    Utils().displayToastMessage(activity, "Translating (${activity.languageCodes[activity.fromIdx]} -> ${activity.languageCodes[activity.toIdx]})...")
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
                        Log.d("TranslatorHelperLog", "Failure on Translation with Error $exception")
                        Utils().displayToastMessage(activity, "Error to translate")
                    }
            }
            .addOnFailureListener { exception ->
                // Model couldn’t be downloaded or other internal error.
                // ...
                Log.d("Error Line 162:", exception.toString())
                Utils().displayToastMessage(activity, "Error in download model")
            }
    }
}