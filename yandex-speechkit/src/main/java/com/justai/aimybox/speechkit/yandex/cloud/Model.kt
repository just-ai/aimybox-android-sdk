package com.justai.aimybox.speechkit.yandex.cloud


class Language(internal val stringValue: String) {
    companion object {
        val RU = Language("ru-RU")
        val EN = Language("en-US")
        val TR = Language("tr-TR")
    }
}

class VoiceModel(internal val stringValue: String) {
    companion object {
        val GENERAL = VoiceModel("general")
        val MAPS = VoiceModel("maps")
        val DATES = VoiceModel("dates")
        val NAMES = VoiceModel("names")
        val NUMBERS = VoiceModel("numbers")
    }
}

class Voice(internal val stringValue: String) {
    companion object {
        val ALYSS = Voice("alyss")
        val JANE = Voice("jane")
        val OKSANA = Voice("oksana")
        val OMAZH = Voice("omazh")
        val ZAHAR = Voice("zahar")
        val ERMIL = Voice("ermil")
        val FILIPP = Voice("filipp")
        val ALENA = Voice("alena")
    }
}

class Emotion(internal val stringValue: String) {
    companion object {
        val GOOD = Emotion("good")
        val EVIL = Emotion("evil")
        val NEUTRAL = Emotion("neutral")
    }
}

class Speed(val value: Float) {

    companion object {
        const val MAX = 3.0F
        const val MIN = 0.1F
        val DEFAULT = Speed(1.0F)
    }

    internal val floatValue = value.coerceIn(MIN..MAX)
}