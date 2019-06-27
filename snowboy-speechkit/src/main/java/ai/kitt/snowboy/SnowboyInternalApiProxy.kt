package ai.kitt.snowboy

/**
 * This class is required to hide snowboy java library api. It's impossible to move speechkit
 * and snowboy library in the same package because native library requires the package to be "ai.kitt.snowboy" to load.
 * And without this class impossible to access the package-private class SnowboyDetect from "com.aimybox.speechkit.snowboy"
 * */
internal class SnowboyInternalApiProxy(resourceFilePath: String, modelFilePath: String) {
    private val instance = SnowboyDetect(resourceFilePath, modelFilePath)

    fun setSensitivity(sensitivity: Float) = instance.SetSensitivity(sensitivity.toString())

    fun setAudioGain(gain: Float) = instance.SetAudioGain(gain)

    fun applyFrontend() = instance.ApplyFrontend(true)

    fun runDetection(data: ShortArray) = instance.RunDetection(data, data.size)

    fun delete() = instance.delete()
}