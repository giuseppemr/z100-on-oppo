package com.vuzix.ultralite.sample;

import android.content.SharedPreferences;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetrofitInterceptor implements Interceptor {
    private SharedPreferences sharedPreferences;

    public RetrofitInterceptor(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request originalRequest = chain.request();
        try {
            if (Objects.requireNonNull(chain.request().url().pathSegments()).contains("login")) {
                return chain.proceed(originalRequest);
            } else {
                Request.Builder requestBuilder = originalRequest.newBuilder()
                        .header("Authorization", "Bearer " + sharedPreferences.getString("token", null));

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        } catch (Exception e) {

        }
        return chain.proceed(originalRequest);
    }
}
