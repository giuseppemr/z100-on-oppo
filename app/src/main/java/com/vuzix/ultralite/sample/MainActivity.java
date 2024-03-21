package com.vuzix.ultralite.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import com.vuzix.ultralite.sample.tags.BlackTag;
import com.vuzix.ultralite.sample.tags.PinkTag;
import com.vuzix.ultralite.sample.tags.WhiteTag;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        startScanning();
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
                setTextWithBold(currentText, "", "");
                return true;
            }
            updateTextOnScreen(strings[page]);
            saveVariables(MainActivity.this, strings, pages, page);
            setTextWithBold(currentText, String.join("", strings), strings[page]);
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
                setTextWithBold(currentText, String.join("", strings), strings[page]);
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

    //BLUETOOTH LE BUTTONS
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothManager btManager;
    private BluetoothGatt blackGatt, whiteGatt, pinkGatt;
    BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {
        switch (device.getAddress()) {
            case BlackTag.mac:
                BlackTag.device = device;
            case PinkTag.mac:
                PinkTag.device = device;
            case WhiteTag.mac:
                WhiteTag.device = device;
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                @SuppressLint("MissingPermission")
                List<BluetoothDevice> devices = btManager.getConnectedDevices(BluetoothProfile.GATT);
                Log.e(TAG, "FOUND " + getButton(device.getAddress()));
                if (!devices.contains(device)) {
                    Log.e(TAG, "TRYING TO CONNECT TO " + getButton(device.getAddress()));
                    connectToDevice(device);
                } else {
                    Log.e(TAG, "ALREADY CONNECTED TO " + getButton(device.getAddress()));
                }
                break;
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (device.getAddress().equals(BlackTag.mac)) {
            blackGatt = device.connectGatt(this, false, gattCallback);
        } else if (device.getAddress().equals(PinkTag.mac)) {
            pinkGatt = device.connectGatt(this, false, gattCallback);
        } else if (device.getAddress().equals(WhiteTag.mac)) {
            whiteGatt = device.connectGatt(this, false, gattCallback);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Log.e(TAG, "CONNESSO CON " + getButton(gatt.getDevice().getAddress()));
                if (isBlackTag(gatt)) {
                    blackGatt.discoverServices();
                } else if (isPinkTag(gatt)) {
                    pinkGatt.discoverServices();
                    backBleCheckView.setImageDrawable(getDrawable(R.drawable.baseline_check_box_24));
                } else {
                    whiteGatt.discoverServices();
                    nextBleCheckView.setImageDrawable(getDrawable(R.drawable.baseline_check_box_24));
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "DISCONNESSO CON " + getButton(gatt.getDevice().getAddress()));
                if (isBlackTag(gatt)) {
                    blackGatt.close();
                    blackGatt = null;
                } else if (isPinkTag(gatt)) {
                    backBleCheckView.setImageDrawable(getDrawable(R.drawable.baseline_check_box_outline_blank_24));
                    pinkGatt.close();
                    pinkGatt = null;
                } else {
                    nextBleCheckView.setImageDrawable(getDrawable(R.drawable.baseline_check_box_outline_blank_24));
                    whiteGatt.close();
                    whiteGatt = null;
                }
            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothDevice device = gatt.getDevice();
                String tagServiceUuid = null;
                String tagCharacteristicUuid = null;
                String tagType = null;

                if (device.equals(BlackTag.device)) {
                    tagServiceUuid = BlackTag.servicestr;
                    tagCharacteristicUuid = BlackTag.charactstr;
                    tagType = "BLACK";
                } else if (device.equals(PinkTag.device)) {
                    tagServiceUuid = PinkTag.servicestr;
                    tagCharacteristicUuid = PinkTag.charactstr;
                    tagType = "PINK";
                } else if (device.equals(WhiteTag.device)) {
                    tagServiceUuid = WhiteTag.servicestr;
                    tagCharacteristicUuid = WhiteTag.charactstr;
                    tagType = "WHITE";
                }

                if (tagServiceUuid != null) {
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        String serviceUUID = service.getUuid().toString();
                        if (serviceUUID.equals(tagServiceUuid)) {
                            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                            for (BluetoothGattCharacteristic characteristic : characteristics) {
                                String characteristicUUID = characteristic.getUuid().toString();
                                if (characteristicUUID.equals(tagCharacteristicUuid)) {
                                    gatt.setCharacteristicNotification(characteristic, true);
                                    Log.e(TAG, "ENABLING " + tagType + " CHAR");
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Questo metodo viene chiamato quando i dati notificati sono ricevuti.
            byte[] data = characteristic.getValue();
            //TODO qua mandare il testo giusto direttamente
            byte[] send;
            if (isBlackTag(gatt)) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "foto action from button", Toast.LENGTH_SHORT).show());
                Log.e(TAG, "foto action");
                send = "take".getBytes();
            } else if (isPinkTag(gatt)) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "back action from button", Toast.LENGTH_SHORT).show());
                Log.e(TAG, "back action");
                send = "back".getBytes();
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "next action from button", Toast.LENGTH_SHORT).show());
                Log.e(TAG, "next action");
                send = "next".getBytes();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                String value = new String(data);
            }
        }
    };

    @Override
    protected void onDestroy() {
        Log.e(TAG, "CLEANUP");
        stopScanning();
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    private void startScanning() {
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    @SuppressLint("MissingPermission")
    private void stopScanning() {
        if (blackGatt != null) {
            blackGatt.close();
            blackGatt.disconnect();
        }
        if (whiteGatt != null) {
            whiteGatt.close();
            whiteGatt.disconnect();
        }
        if (pinkGatt != null) {
            pinkGatt.close();
            pinkGatt.disconnect();
        }
        if (bluetoothAdapter != null && leScanCallback != null) {
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
}
