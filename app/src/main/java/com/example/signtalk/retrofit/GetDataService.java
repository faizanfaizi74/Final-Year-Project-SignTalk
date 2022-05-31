package com.example.signtalk.retrofit;

import com.example.signtalk.model.Sentence;

import org.json.JSONObject;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface GetDataService {

    @Headers({"Connection: close"})
    @Multipart
    @POST("/predict")
    Call<Sentence> uploadVideo(@Part MultipartBody.Part video);

    // ye file missing hai.
    // pura new project downalod kia tha ya files copy ki hai is men? new download kiya tha zip files open ki hen
}