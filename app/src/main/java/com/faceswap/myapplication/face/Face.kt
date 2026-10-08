package com.faceswap.myapplication.face

data class Face(
    val bbox: FloatArray,
    val landmarks: FloatArray,
    var embedding: FloatArray? = null
)