package com.justai.aimybox.speechkit.pocketsphinx

import edu.cmu.pocketsphinx.SpeechRecognizer
import edu.cmu.pocketsphinx.SpeechRecognizerSetup
import java.io.File

class PocketsphinxRecognizerProvider(
    private val assets: PocketsphinxAssets,
    private val sampleRate: Int = 16000,
    private val keywordThreshold: Float = 1e-40f
) {

    val recognizer: SpeechRecognizer by lazy { createRecognizer() }

    private fun createRecognizer() =
        SpeechRecognizerSetup.defaultSetup()
            .setAcousticModel(File(assets.acousticModelFilePath))
            .setDictionary(File(assets.dictionaryFilePath))
            .setSampleRate(sampleRate)
            .setKeywordThreshold(keywordThreshold)
            .recognizer
}