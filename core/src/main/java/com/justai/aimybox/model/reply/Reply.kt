package com.justai.aimybox.model.reply

import com.google.gson.JsonObject

/**
 * Reply is a unit of a composite response. One response contains from 0 to infinite replies. Implementation of
 * the interface must have strictly single constructor with single parameter which is overrides [json].
 * */
interface Reply {
    /**
     * The source JSON of the reply. It is useful to extract additional data, which is not provided by included replies.
     * */
    val json: JsonObject
}
