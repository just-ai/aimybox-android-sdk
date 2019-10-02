package com.justai.aimybox.dialogapi.dialogflow.converter

import com.google.protobuf.GeneratedMessageV3
import com.justai.aimybox.model.reply.Reply

interface ResponseConverter<in T: GeneratedMessageV3, out R: Reply> {

    fun convert(list: List<T>): List<R> = list.map { convert(it) }

    fun convert(msg: T): R
}