package com.moblin.android.various.network

import okhttp3.OkHttpClient

private val httpSharedClient: OkHttpClient by lazy { OkHttpClient() }

fun httpUrlSession(): OkHttpClient = httpSharedClient
