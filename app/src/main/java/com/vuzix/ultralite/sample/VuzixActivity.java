package com.vuzix.ultralite.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vuzix.ultralite.sample.tags.BlackTag;
import com.vuzix.ultralite.sample.tags.BlueTag;
import com.vuzix.ultralite.sample.tags.PinkTag;
import com.vuzix.ultralite.sample.tags.WhiteTag;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VuzixActivity extends AppCompatActivity {
    private static final String TAG = VuzixActivity.class.getName();

    String[] strings;
    int pages = 0;
    int page = 0;
    int chunk = 240;
    SharedPreferences sharedPreferences;
    Api api;

    Call<LoginResponse> login;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("z100", MODE_PRIVATE);
        loadVariables(this);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new RetrofitInterceptor(sharedPreferences))
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .callTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://lostotores.it")
                .client(okHttpClient)
                .build();

        api = retrofit.create(Api.class);

        login = api.login(new LoginRequest("super_admin@lostotores.it", "superadmin451."));



        /*String[] permissionArray = {Manifest.permission.FOREGROUND_SERVICE};
        ActivityCompat.requestPermissions(this, permissionArray, 0);*/
    }

    String[] splitString(String input, int substringLength) {
        int length = input.length();
        if (length == 0 || substringLength <= 0) {
            return new String[]{};
        }

        // Calculate the number of substrings needed
        int numSubstrings = (int) Math.ceil((double) length / substringLength);

        String[] substrings = new String[numSubstrings];

        // Initialize start and end indices
        int start = 0;

        for (int i = 0; i < numSubstrings; i++) {
            int end = Math.min(start + substringLength, length);

            // Backtrack to the last space if we're in the middle of a word
            while (end < length && end > start && !Character.isWhitespace(input.charAt(end))) {
                end--;
            }

            substrings[i] = input.substring(start, end).trim();
            start = end + 1; // Move start index to the next character after the space
        }

        return substrings;
    }

    // Metodo per salvare le variabili nelle SharedPreferences
    void saveVariables(Context context, String[] strings, int pages, int page) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (strings != null) {
            // Salvataggio delle stringhe come stringa unica separata da virgola
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : strings) {
                stringBuilder.append(s).append(" ");
            }
            editor.putString("strings", stringBuilder.toString());
        } else {
            editor.putString("strings", "");
        }
        // Salvataggio degli interi
        editor.putInt("pages", pages);
        editor.putInt("page", page);

        editor.apply();
    }

    String loadVariables(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String preferencesString = sharedPreferences.getString("strings", "");
        if (!preferencesString.isEmpty()) {
            if (preferencesString.length() > chunk) {
                strings = splitString(preferencesString, chunk);
            } else {
                strings = new String[]{preferencesString};
            }
        } else {
            strings = new String[]{};
        }
        pages = sharedPreferences.getInt("pages", 0);
        page = sharedPreferences.getInt("page", 0);
        if (strings != null && strings.length > 0) {
            return strings[page];
        }
        return preferencesString;
    }

    void toastIt(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }


    @SuppressLint("MissingPermission")
    public String getButton(String address) {
        if (address.equals(BlackTag.mac)) {
            return "BLACK TAG";
        }
        if (address.equals(WhiteTag.mac)) {
            return "WHITE TAG";
        }
        if (address.equals(PinkTag.mac)) {
            return "PINK TAG";
        }
        if (address.equals(BlueTag.mac)) {
            return "BLUE TAG";
        }
        return null;
    }

    public boolean isBlackTag(BluetoothGatt gatt) {
        return gatt.getDevice().getAddress().equals(BlackTag.mac);
    }

    public boolean isPinkTag(BluetoothGatt gatt) {
        return gatt.getDevice().getAddress().equals(PinkTag.mac);
    }

    public boolean isWhiteTag(BluetoothGatt gatt) {
        return gatt.getDevice().getAddress().equals(WhiteTag.mac);
    }

    public boolean isBlueTag(BluetoothGatt gatt) {
        return gatt.getDevice().getAddress().equals(BlueTag.mac);
    }
}
