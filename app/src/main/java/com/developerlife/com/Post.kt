package com.developerlife.com

import com.google.gson.annotations.SerializedName

data class Post (
    @SerializedName("id") val id: Int,
    @SerializedName("description") val title: String,
    @SerializedName("gifURL") val gifURL: String)