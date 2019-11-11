package com.justai.aimybox.model.reply

import com.justai.aimybox.model.AudioSpeech

/**
 * Represents audio reply shich should be played without any UI.
 */
open class AudioReply(
    /**
     * URL to the audio source
     */
    val url: String
): Reply

fun AudioReply.asAudioSpeech() = AudioSpeech.Uri(url)