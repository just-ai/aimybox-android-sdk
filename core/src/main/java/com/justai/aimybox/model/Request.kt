package com.justai.aimybox.model

import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.CustomSkill

/**
 * Request model, which is used across the library.
 * You can extend it by adding some fields to [data] JSON in [CustomSkill] or custom [DialogApi].
 * */
interface Request {
    /**
     * User input, recognized by STT or manually entered.
     * */
    val query: String
}