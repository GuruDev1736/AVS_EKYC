package com.taskease.yksfoundation.Retrofit

import com.avs.avs_ekyc.Model.UniversalResponseModel
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {

    @POST("Login1.aspx")
    fun login(@Query("data") data : String): Call<UniversalResponseModel>

    @POST("FrmPending_List.aspx")
    fun getPendingList(@Query("data") data : String): Call<UniversalResponseModel>

    @POST("frmkycdetails.aspx")
    fun getKycDetails(@Query("data") data : String): Call<UniversalResponseModel>

}