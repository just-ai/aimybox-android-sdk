package com.justai.aimybox.speechkit.google.cloud.model

import com.google.cloud.texttospeech.v1.SsmlVoiceGender

enum class Gender(internal val value: SsmlVoiceGender) {
    NEUTRAL(SsmlVoiceGender.NEUTRAL),
    MALE(SsmlVoiceGender.MALE),
    FEMALE(SsmlVoiceGender.FEMALE)
}