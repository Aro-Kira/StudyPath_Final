package com.example.studypath_final.imgur

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Part

interface ImgurService {
    @Multipart
    @POST("3/image")
    fun uploadImage(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part,
        @Part("title") title: RequestBody
    ): Call<ImgurResponse>
}
