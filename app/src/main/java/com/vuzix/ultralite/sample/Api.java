package com.vuzix.ultralite.sample;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface Api {
    @POST("api/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @GET("api/chat")
    Call<List<Message>> getMessages();

    @GET("api/chat/last")
    Call<Message> getLast();
}