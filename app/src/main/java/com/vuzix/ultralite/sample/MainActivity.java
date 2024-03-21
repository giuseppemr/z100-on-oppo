package com.vuzix.ultralite.sample;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.vuzix.ultralite.Anchor;
import com.vuzix.ultralite.LVGLImage;
import com.vuzix.ultralite.Layout;
import com.vuzix.ultralite.TextAlignment;
import com.vuzix.ultralite.TextWrapMode;
import com.vuzix.ultralite.UltraliteColor;
import com.vuzix.ultralite.UltraliteSDK;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends VuzixActivity {

    private static final String TAG = MainActivity.class.getName();
    private Model model;
    private TextView currentText;
    private ImageView nextBleCheckView, backBleCheckView;
    private Button next, back;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        ImageView installedImageView = findViewById(R.id.installed);
        ImageView linkedImageView = findViewById(R.id.linked);
        ImageView loginImageView = findViewById(R.id.login);
        TextView nameTextView = findViewById(R.id.name);
        currentText = findViewById(R.id.current);
        nextBleCheckView = findViewById(R.id.next);
        backBleCheckView = findViewById(R.id.back);
        next = findViewById(R.id.go_next);
        back = findViewById(R.id.go_back);
        ImageView connectedImageView = findViewById(R.id.connected);
        Button demoButton = findViewById(R.id.run_demo);
        Button notificationButton = findViewById(R.id.send_notification);

        UltraliteSDK ultralite = UltraliteSDK.get(this);

        ultralite.getAvailable().observe(this, available ->
                installedImageView.setImageResource(available ? R.drawable.ic_check_24 : R.drawable.ic_close_24));

        ultralite.getLinked().observe(this, linked -> {
            linkedImageView.setImageResource(linked ? R.drawable.ic_check_24 : R.drawable.ic_close_24);
            nameTextView.setText(ultralite.getName());
        });

        ultralite.getConnected().observe(this, connected -> {
            connectedImageView.setImageResource(connected ? R.drawable.ic_check_24 : R.drawable.ic_close_24);
            demoButton.setEnabled(connected);
            notificationButton.setEnabled(connected);
        });

        model = new ViewModelProvider(this).get(Model.class);

        model.running.observe(this, running -> {
            if (running) {
                demoButton.setEnabled(false);
            } else {
                demoButton.setEnabled(ultralite.isConnected());
            }
        });

        demoButton.setOnClickListener(v -> model.runDemo());

        notificationButton.setOnClickListener(v ->
                ultralite.sendNotification("Ultralite SDK Sample", "Hello from a sample app!",
                        loadLVGLImage(this, R.drawable.rocket)));

        login.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                Toast.makeText(MainActivity.this, "Login con successo", Toast.LENGTH_LONG).show();
                loginImageView.setImageResource(R.drawable.ic_check_24);
                if (response.body() != null) {
                    sharedPreferences.edit().putString("token", response.body().getToken()).apply();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "LOGIN ERROR", Toast.LENGTH_LONG).show();
                loginImageView.setImageResource(R.drawable.ic_close_24);
            }
        });

        Timer timer = new Timer();
        long delay = 0; // Delay before the first execution (0 for immediate execution)
        long period = 5000; // Repeat every 5 seconds (5000 milliseconds)

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                getLastMessage();
            }
        }, delay, period);

        next.setOnClickListener(view -> goNextPage());

        back.setOnClickListener(view -> goBackPage());
    }

    private Message lastMessage;

    private void getLastMessage() {
        api.getLast().enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (response.body() != null) {
                    if (lastMessage != null && lastMessage.equals(response.body())) {
                        Log.d(TAG, "messaggio uguale : " + response.body().getText());
                    } else {
                        Log.d(TAG, "nuovo messaggio : " + response.body().getText());
                        if (response.body().getText().length() > chunk) {
                            strings = splitString(response.body().getText(), chunk);
                            pages = strings.length;
                            page = 0;
                            Log.d(TAG, "diviso in pagine: " + pages);
                            saveVariables(MainActivity.this, strings, pages, page);
                            updateTextOnScreen(strings[page]);
                            setTextWithBold(currentText, response.body().getText(), strings[page]);
                        } else {
                            updateTextOnScreen(response.body().getText());
                            setTextWithBold(currentText, response.body().getText(), response.body().getText());
                            saveVariables(MainActivity.this, new String[]{response.body().getText()}, pages, page);
                        }
                    }
                    lastMessage = response.body();
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                toastIt("errore lettura messaggi " + t.getMessage());
                Log.e(TAG, "ERRORE LETTURA MESSAGGI");
            }
        });
    }

    private boolean goNextPage() {
        Log.d(TAG, "received next action");
        if (strings != null && strings.length > 0) {
            page++;
            if (page == pages) {
                page = 0;
                pages = 0;
                strings = null;
                saveVariables(MainActivity.this, new String[]{}, pages, page);
                updateTextOnScreen("");
                return true;
            }
            updateTextOnScreen(strings[page]);
            saveVariables(MainActivity.this, strings, pages, page);
        }
        nextBleCheckView.setImageDrawable(getDrawable(R.drawable.baseline_check_box_24));
        return false;
    }

    private boolean goBackPage() {
        Log.d(TAG, "received back action");
        if (strings != null && strings.length > 0) {
            if (page > 0) {
                page--;
                saveVariables(MainActivity.this, new String[]{}, pages, page);
                updateTextOnScreen(strings[page]);
            }
        }
        backBleCheckView.setImageDrawable(getDrawable(R.drawable.baseline_check_box_24));
        return false;
    }

    private void updateTextOnScreen(String text) {
        //TODO vedere se fare update
        model.showMessage(text);
    }

    private static void setTextWithBold(TextView textView, String fullText, String textToBold) {
        // Creazione di un oggetto SpannableStringBuilder per manipolare il testo
        SpannableStringBuilder builder = new SpannableStringBuilder();

        // Aggiunta del testo completo
        builder.append(fullText);

        // Trovare l'indice della prima occorrenza della sottostringa
        int startIndex = fullText.indexOf(textToBold);
        if (startIndex != -1) {
            // Applicare lo stile grassetto alla sottostringa trovata
            builder.setSpan(new StyleSpan(Typeface.BOLD), startIndex, startIndex + textToBold.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Impostare il testo con lo stile applicato alla TextView
        textView.setText(builder);
    }

    public static class Model extends AndroidViewModel {

        private final UltraliteSDK ultralite;

        private final MutableLiveData<Boolean> running = new MutableLiveData<>();
        private boolean lostControl;

        public Model(@NonNull Application application) {
            super(application);
            ultralite = UltraliteSDK.get(application);
            ultralite.getControlledByMe().observeForever(controlledObserver);
        }

        @Override
        protected void onCleared() {
            ultralite.releaseControl();

            // delay removing the observer until after we are notified of losing control
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    ultralite.getControlledByMe().removeObserver(controlledObserver), 500);
        }

        private void showMessage(String message) {
            if (ultralite.requestControl()) {
                lostControl = false;
                new Thread(() -> {
                    running.postValue(true);
                    try {
                        ultralite.setLayout(Layout.CANVAS, 0, true);

                        int textId = ultralite.getCanvas().createText(message, TextAlignment.AUTO, UltraliteColor.WHITE, Anchor.CENTER, 0, 0, 640, -1, TextWrapMode.WRAP, true);
                        if (textId == -1) {
                            throw new Stop(true);
                        }
                        ultralite.getCanvas().commit();
                        pause(5000);

                        ultralite.getCanvas().updateText(textId, "The text can be changed.");
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "The text can be moved.");
                        ultralite.getCanvas().moveText(textId, Anchor.TOP_CENTER, 0, 0);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "If requested, the text can wrap if it grows too large to show on a single line.");
                        ultralite.getCanvas().commit();
                        pause(5000);
                    } catch (Stop stop) {
                        ultralite.releaseControl();
                        if (stop.error) {
                            ultralite.sendNotification("Demo Error", "An error occurred during the demo");
                        } else {
                            ultralite.sendNotification("Demo Control Lost", "The demo lost control of the glasses");
                        }
                    }
                    running.postValue(false);
                }).start();
            }
        }

        private void runDemo() {
            if (ultralite.requestControl()) {
                lostControl = false;
                new Thread(() -> {
                    running.postValue(true);
                    try {
                        ultralite.setLayout(Layout.CANVAS, 0, true);

                        int textId = ultralite.getCanvas().createText("This is a canvas with a text field.", TextAlignment.AUTO, UltraliteColor.WHITE, Anchor.CENTER, 0, 0, 640, -1, TextWrapMode.WRAP, true);
                        if (textId == -1) {
                            throw new Stop(true);
                        }
                        ultralite.getCanvas().commit();
                        pause(5000);

                        ultralite.getCanvas().updateText(textId, "The text can be changed.");
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "The text can be moved.");
                        ultralite.getCanvas().moveText(textId, Anchor.TOP_CENTER, 0, 0);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "The text can be made invisible...");
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().setTextVisible(textId, false);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "and visible again...");
                        ultralite.getCanvas().setTextVisible(textId, true);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "If requested, the text can wrap if it grows too large to show on a single line.");
                        ultralite.getCanvas().commit();
                        pause(5000);

                        ultralite.getCanvas().updateText(textId, "You can create image objects.");

                        LVGLImage rocket = loadLVGLImage(getApplication(), R.drawable.rocket);
                        int imageId = ultralite.getCanvas().createImage(rocket, Anchor.CENTER);
                        if (imageId == -1) {
                            throw new Stop(true);
                        }
                        ultralite.getCanvas().commit();
                        pause(5000);

                        ultralite.getCanvas().updateText(textId, "You can change the image.");
                        ultralite.getCanvas().updateImage(imageId, loadLVGLImage(getApplication(), R.drawable.poop));
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "You can move the image.");
                        ultralite.getCanvas().moveImage(imageId, 100, 100);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "You can hide an image.");
                        ultralite.getCanvas().setImageVisible(imageId, false);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "You can show an image.");
                        ultralite.getCanvas().setImageVisible(imageId, true);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().removeImage(imageId);
                        ultralite.getCanvas().updateText(textId, "Animations are possible too.");

                        LVGLImage happy = loadLVGLImage(getApplication(), R.drawable.happy);
                        LVGLImage wink = loadLVGLImage(getApplication(), R.drawable.wink);
                        int animationId = ultralite.getCanvas().createAnimation(new LVGLImage[]{happy, wink}, Anchor.CENTER, 1000);
                        if (animationId == -1) {
                            throw new Stop(true);
                        }
                        ultralite.getCanvas().commit();
                        pause(5000);

                        ultralite.getCanvas().updateText(textId, "You can move animations.");
                        ultralite.getCanvas().moveAnimation(animationId, 400, 300);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "You can hide animations.");
                        ultralite.getCanvas().setAnimationVisible(animationId, false);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().updateText(textId, "You can show animations.");
                        ultralite.getCanvas().setAnimationVisible(animationId, true);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().removeAnimation(animationId);

                        ultralite.getCanvas().updateText(textId, "You can repeat an image across the background layer with a single command.");
                        Point[] coordinates = {
                                new Point(0, 100),
                                new Point(100, 150),
                                new Point(200, 200),
                                new Point(300, 250),
                                new Point(400, 300),
                                new Point(500, 350)
                        };
                        ultralite.getCanvas().drawBackground(loadBitmap(getApplication(), R.drawable.rocket), coordinates);
                        ultralite.getCanvas().commit();
                        pause(5000);

                        ultralite.getCanvas().updateText(textId, "You can clear areas of the background layer.");
                        ultralite.getCanvas().clearBackgroundRect(100, 100, 440, 280, UltraliteColor.WHITE);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().clearBackgroundRect(100, 100, 440, 280);
                        ultralite.getCanvas().commit();
                        pause();

                        ultralite.getCanvas().clearBackground();
                        ultralite.getCanvas().updateText(textId, "Finally, we're going to send a full screen image to the glasses.");
                        ultralite.getCanvas().commit();
                        pause(4000);

                        ultralite.getCanvas().removeText(textId);

                        Bitmap bigImage = loadBitmap(getApplication(), R.drawable.ultralite_large_ori);
                        ultralite.getCanvas().drawBackground(bigImage, 0, 0);
                        ultralite.getCanvas().commit(() -> Log.d("MainActivity", "full screen image commit is done!"));
                        pause(5000);

                        ultralite.releaseControl();
                        ultralite.sendNotification("Demo Success", "The demo is over");
                    } catch (Stop stop) {
                        ultralite.releaseControl();
                        if (stop.error) {
                            ultralite.sendNotification("Demo Error", "An error occurred during the demo");
                        } else {
                            ultralite.sendNotification("Demo Control Lost", "The demo lost control of the glasses");
                        }
                    }
                    running.postValue(false);
                }).start();
            }
        }

        private void pause() throws Stop {
            pause(2000);
        }

        private void pause(long ms) throws Stop {
            SystemClock.sleep(ms);
            if (lostControl) {
                throw new Stop(false);
            }
        }

        private final Observer<Boolean> controlledObserver = controlled -> {
            if (!controlled) {
                lostControl = true;
            }
        };
    }

    private static class Stop extends Exception {

        private final boolean error;

        public Stop(boolean error) {
            this.error = error;
        }
    }

    private static LVGLImage loadLVGLImage(Context context, int resource) {
        return LVGLImage.fromBitmap(loadBitmap(context, resource), LVGLImage.CF_INDEXED_1_BIT);
    }

    @SuppressWarnings("ConstantConditions")
    private static Bitmap loadBitmap(Context context, int resource) {
        BitmapDrawable drawable = (BitmapDrawable) ResourcesCompat.getDrawable(
                context.getResources(), resource, context.getTheme());
        return drawable.getBitmap();
    }
}
