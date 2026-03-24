package com.rice.momentocalendariopresentazionealice;

import static com.bfr.buddysdk.BuddySDK.Modules.USB;

import com.bfr.buddy.usb.shared.IUsbCommadRsp;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.concurrent.Future;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;


import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import com.bfr.buddy.ui.shared.FacialEvent;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddySDK;

import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;


import java.io.IOException;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


import java.util.Map;
import java.util.Random;


import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class MainActivity extends BuddyActivity {
    private static final String SUBSCRIPTION_KEY = "";
    private static final String SERVICE_REGION = "westeurope";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private ImageButton button_start;
    private ImageButton button_stop;
    private ImageButton button_presentazione;
    private ImageButton button_giorno;
    private ImageButton button_stagioni;
    private ImageButton button_meteo;
    private ImageButton button_dance;
    private ImageView imageView;
    private SpeechRecognizer recognizer;
    private BuddyController buddyController;
    TextView responseTextView;
    private OpenAIClient openAIClient;
    private String systemMessage;
    private String systemMeteo;
    private String systemCity;
    JsonLoader jsonLoader;
    Map<String, Object> jsonMap;
    Map<String, Object> emotionsMap;
    private static final String videoFilePath = "/storage/emulated/0/Movies/Video1.mp4";  // Path to the video
    private VideoPlayer videoPlayer;
    private Handler uiHandler;
    private Handler handler;
    private String question;
    private String phase;
    Map<String, Integer> imageMap;
    Map<String, String> seasons;
    private MediaPlayer mediaPlayer;
    private String recognizer_state;
    private String recognizer_activated;
    private String speaking;
    private Boolean next = false;
    private Boolean cooking = false;
    private Boolean listened;
    private String recognized_sentence;
    private Boolean clicked;
    private JSONArray history_messages;
    private WeatherService ws;
    private String meteo_sentence;
    private Boolean presented;
    private Boolean dancing;
    private String presentation = "phase0";
    private Boolean saying_name;
    private Boolean touching;
    private MediaPlayer mediaPlayer2;
    private Boolean singing;
    private Boolean button_pressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        jsonLoader = new JsonLoader();
        handler = new Handler();
        jsonMap = jsonLoader.loadJSONFromAsset(getApplicationContext(), "dialogue.json");
        Log.i("info", "MAP");
        Log.i("info", jsonMap.toString());
        emotionsMap = jsonLoader.loadJSONFromAsset(getApplicationContext(), "emotions.json");
        // Request microphone permission if not already granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initializeRecognizer();
        }

        buddyController = new BuddyController();

    }


    @Override
    public void onSDKReady() {

        // transfer the touch information to BuddyCore in the background
        BuddySDK.UI.setViewAsFace(findViewById(R.id.view_face));
        button_start = findViewById(R.id.buttonstart);
        button_start.setOnClickListener(view -> {
            recognizer_state = "active";
            activate_recognizer();
        });
        button_stop = findViewById(R.id.buttonstop);
        button_stop.setOnClickListener(view -> {
            recognizer_state = "stopped";
        });

        button_presentazione = findViewById(R.id.buttonpresentazione);
        button_presentazione.setOnClickListener(view -> {
            recognizer_state = "stopped";
            if(presentation.equals("phase0")) {
                presentazione();
                presentation = "phase1";
            }

            else if(presentation.equals("phase1")) {
                presentazione2();
                presentation = "phase2";
            }
        });

        button_giorno = findViewById(R.id.buttongiorno);
        button_giorno.setOnClickListener(view -> {
            recognizer_state = "stopped";
            ripeti_giorno();

        });

        button_stagioni = findViewById(R.id.buttonstagione);
        button_stagioni.setOnClickListener(view -> {
            recognizer_state = "stopped";
            ripeti_stagione("primavera");

        });

        button_meteo = findViewById(R.id.buttonmeteo);
        button_meteo.setOnClickListener(view -> {
            recognizer_state = "stopped";
            button_pressed = true;
            ask_weather("Genova,it");
        });

        button_dance = findViewById(R.id.buttondance);
        button_dance.setOnClickListener(view -> {
            dancing=true;
            start_dance();
        });


        responseTextView = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView);
        // Load values from the config.yaml file
        question = "zero";
        phase = "";
        saying_name = false;
        touching = false;
        mediaPlayer2 = MediaPlayer.create(this, R.raw.gs_tutta);
        imageMap = new HashMap<String, Integer>() {{
            put("albero", R.drawable.albero);
            put("casa", R.drawable.casa);
            put("cane", R.drawable.cane);
            put("nuvole", R.drawable.nuvole);
            put("sole", R.drawable.sole);
            put("pioggia", R.drawable.pioggia);
            put("vento", R.drawable.vento);
            put("hello", R.drawable.hello_gif);
            put("nihao", R.drawable.nihao);
            put("autunno", R.drawable.autunno);
            put("inverno", R.drawable.inverno);
            put("primavera", R.drawable.primavera_calendario);
            put("estate", R.drawable.estate);
            put("giorni", R.drawable.giornisettimana);
            put("giovedi", R.drawable.giovedi);
            put("venerdi", R.drawable.venerdi);
        }};


        seasons = new HashMap<String, String>() {{
            put("autunno", "秋天");
            put("inverno", "冬天");
            put("primavera", "春天");
            put("estate", "夏天");
        }};

        Map<String, String> config = loadConfigFromYaml();
        videoPlayer = new VideoPlayer(this, videoFilePath);
        uiHandler = new Handler(Looper.getMainLooper());

        recognizer_state = "active";
        recognizer_activated = "False";
        speaking = "False";
        recognized_sentence = "";
        clicked = false;
        presented = false;
        dancing =false;
        ws = new WeatherService();
        history_messages = new JSONArray();
        singing = false;
        button_pressed = false;
        if (config != null) {
            String apiKey = config.get("api_key");
            String organizationId = config.get("organization_id");
            systemMessage = config.get("system_message2");
            systemMeteo = config.get("system_message_meteo");
            systemCity = config.get("system_message_meteo_city");
            Log.i("info", "system message: " + systemMessage);
            openAIClient = new OpenAIClient(apiKey, organizationId);
        } else {
            // responseTextView.setText("Failed to load configuration.");
        }

        buddyController.start_autonomous_head_movements();
        startBodyTouchSensorCheck();
        start_autonomous_life();

    }


    private void save_timestamp() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.e("error","NO PERSMISSION");
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return; // Stop execution here, wait for permission result
            }
        }


        // Get current timestamp
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);

        // Extract year, month, and day for filename
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();


        File dir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dir = new File(getExternalFilesDir(null), "LogFiles"); // No permissions required
        } else {
            dir = new File(Environment.getExternalStorageDirectory(), "LogFiles");
        }

        // Ensure the directory exists
        if (!dir.exists() && !dir.mkdirs()) {
            System.out.println("Failed to create directory!");
            return;
        }

        // Create file in the directory
        File file = new File(dir, String.format("presentazione_alice_%d_%02d_%02d_timestamp.txt", year, month, day));


        // Write timestamp to file
        try (FileWriter writer = new FileWriter(file, true)) { // true = append mode
            writer.write(timestamp + "\n");
            writer.flush(); // Ensure it's written
            System.out.println("Sentence saved at: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void save_sentence(String sentence) {
        // Check and request WRITE_EXTERNAL_STORAGE permission (for Android < 10)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // Android 9 or lower
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return; // Exit function to wait for permission response
            }
        }

        // Get current timestamp
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);

        // Extract year, month, and day for filename
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();

        // Use app-specific storage directory (no permissions required on Android 10+)
        File dir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dir = new File(getExternalFilesDir(null), "LogFiles"); // /storage/emulated/0/Android/data/your.package.name/files/LogFiles/
        } else {
            dir = new File(Environment.getExternalStorageDirectory(), "LogFiles"); // /storage/emulated/0/LogFiles/
        }

        // Ensure directory exists
        if (!dir.exists() && !dir.mkdirs()) {
            System.out.println("Failed to create directory!");
            return;
        }

        // Create file in the directory
        File file = new File(dir, String.format("presentazione_alice_%d_%02d_%02d_sentence.txt", year, month, day));

        // Write timestamp and sentence to file
        try (FileWriter writer = new FileWriter(file, true)) { // true = append mode
            writer.write(timestamp + " " + sentence + "\n");
            writer.flush(); // Ensure it's written
            System.out.println("Sentence saved: " + timestamp + " " + sentence);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject parseWeatherData(String jsonResponse) {
        Gson gson = new Gson();

        // Parse the response into a WeatherResponse object
        WeatherResponse weatherResponse = gson.fromJson(jsonResponse, WeatherResponse.class);

        // Access the main weather fields
        double temperature = weatherResponse.main.temp;  // Temperature in Celsius
        double feelsLike = weatherResponse.main.feels_like;  // Feels like temperature
        int humidity = weatherResponse.main.humidity;  // Humidity percentage
        String weatherDescription = weatherResponse.weather[0].description;  // Weather description ("broken clouds")
        double windSpeed = weatherResponse.wind.speed;  // Wind speed
        String cityName = weatherResponse.name;  // City name ("Genoa")

        // Log the data
        Log.d("Weather", "City: " + cityName);
        Log.d("Weather", "Temperature: " + temperature + "°C");
        Log.d("Weather", "Feels Like: " + feelsLike + "°C");
        Log.d("Weather", "Humidity: " + humidity + "%");
        Log.d("Weather", "Description: " + weatherDescription);
        Log.d("Weather", "Wind Speed: " + windSpeed + " m/s");
        // Create a JSONObject to hold the weather data
        JSONObject weather = new JSONObject();
        try {
            weather.put("city", cityName);
            weather.put("description", weatherDescription);
            weather.put("temperature", String.valueOf(temperature));  // Convert double to String
        } catch (Exception e) {
            e.printStackTrace();
        }

        return weather;
    }


    public interface WeatherDataCallback {
        void onWeatherDataReceived(JSONObject weatherData) throws JSONException;

        void onError(String errorMessage);
    }

    // Modify your get_weather_data method to accept a callback
    private void get_weather_data(String city, final WeatherDataCallback callback) {
        ws.getWeather(city, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                // Parse the weather data from the JSON response
                JSONObject weather = parseWeatherData(jsonResponse);

                try{
                // Pass the weather data to the callback
                callback.onWeatherDataReceived(weather);

                } catch (JSONException e) {
                    // Handle the exception here
                    Log.e("WeatherError", "Error parsing weather data: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Pass the error message to the callback
                callback.onError(errorMessage);
            }
        });
    }

    private void listen_response(){
        listened=false;
        long startTime = System.currentTimeMillis();
        while (!listened) {
            recognizer_state = "stopped";

            if (recognizer_activated.equals("False")) {
                recognizeSpeechOnce();
            }

            // Break if 10 seconds (10,000 milliseconds) have elapsed
            if (System.currentTimeMillis() - startTime >= 5000) {
                System.out.println("Timeout reached, exiting loop.");
                break;
            }
        }
    }

    private void presentazione() {
        button_start.setVisibility(View.GONE);
        button_stop.setVisibility(View.GONE);
        recognizer_state="stopped";
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
          sayText("Ciao amici, mi chiamo Sonrie. Sono venuto da Robolandia, un posto fantastico dove tutti i robot vengono creati.");
          sayText("In questo posto si parlano tutte le lingue del mondo, e quando ci salutiamo con gli altri miei amici robot diciamo CLICK. ");
          sayText("La mia casa è color arcobaleno, il mio colore preferito! Sono venuto sul vostro pianeta per conoscere amici come voi. Le vostre maestre mi hanno chiamato e hanno esaudito questo mio desiderio. ");
          sayText("Porto con me tante cose da raccontarvi, come dono per tutti voi. Perché ho già viaggiato tanto. Nel pianeta Terra ho una casa ed è a Genova. Li sto con i miei ingegnosi inventori, che hanno sempre cura di me. Sono loro che mi accompagnano nelle mie avventure, mi piace molto viaggiare, conoscere nuovi amici e nuovi posti.  ");
          sayText("Vi piacerebbe sapere qualcosa di più di me? ");
          listen_response();
          start_autonomous_life();
        }, 1000);
    }


    private void presentazione2() {
        button_start.setVisibility(View.GONE);
        button_stop.setVisibility(View.GONE);
        recognizer_state="stopped";
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Non vedo l’ora di raccontarvi le mie avventure! Ma prima, vorrei sapere qualcosa su di voi. Come vi chiamate?");
            saying_name=true;
            while (saying_name) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.i("main", "Thread interrupted", e);
                    }
                }
            sayText("I vostri nomi sono bellissimi, proprio come quelli degli amici che ho conosciuto durante i miei viaggi. Quando conosco nuovi amici mi piace imparare qualcosa di loro, come la loro lingua, il loro piatto preferito o le loro canzoni. E voi ne avete una da insegnarmi?");
            listen_response();
            start_autonomous_life();
        }, 1000);
    }



    private void ripeti_stagione(String stagione) {
        button_start.setVisibility(View.GONE);
        button_stop.setVisibility(View.GONE);
        recognizer_state="stopped";
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            //sayText(stagione + " in cinese si dice");
            //sayText((String) seasons.get(stagione));
            sayText("Oggi a Genova è primavera e gli uccellini cinguettano sugli alberi");
            show_image(stagione);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                start_autonomous_life();
            }, 5000);
        }, 1000);
    }


    private void ripeti_giorno() {
        button_start.setVisibility(View.GONE);
        button_stop.setVisibility(View.GONE);
        recognizer_state="stopped";

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Venerdì, giorno marrone, abbaio come un cagnolone!");
            show_image("venerdi");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                sayText("Conosco una canzone sui giorni della settimana. Vi piacerebbe ascoltarla?");
                touching = false;
                while (!touching) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.i("main", "Thread interrupted", e);
                    }
                }

                if (mediaPlayer2 != null) {
                    singing=true;
                    Log.i("main", "starting song");
                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                    mediaPlayer2.start();
                }

                mediaPlayer2.setOnCompletionListener(mp -> {
                    mediaPlayer2.release();
                    mediaPlayer2 = null; // Prevent null pointer issues
                    BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                    sayText("Vi è piaciuta?");
                    singing=false;
                    start_autonomous_life();
                });

            }, 6000);
        }, 1000);
    }


    private void stop_song(){
        if (mediaPlayer2 != null) {
            mediaPlayer2.stop();
            mediaPlayer2.prepareAsync(); // Prepare it again
        }
        BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
        sayText("Vi è piaciuta?");
        singing=false;
        start_autonomous_life();
    }

    private void start_autonomous_life() {
        button_start.setVisibility(View.VISIBLE);
        button_stop.setVisibility(View.VISIBLE);
        phase = "end";
        question = "finished";
        recognizer_state = "active";
        activate_recognizer();
    }


    private void startBodyTouchSensorCheck() {
        // Create and start a new thread to run the task
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500); // Delay of 500ms
                    checkBodyTouchAndReact();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String select_animation() {
        Random random = new Random();
        int randomNumber = random.nextInt(5);
        if (randomNumber == 0) return "Happy";
        else if (randomNumber == 1) return "Smile";
        else if (randomNumber == 2) return "Surprised";
        else if (randomNumber == 3) return "Whistle";
        else if (randomNumber == 4) return "Dance";
        else return "Love";
    }

    private void select_face() {
        Random random = new Random();
        int randomNumber = random.nextInt(6);
        if (randomNumber == 0) BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY);
        else if (randomNumber == 1) BuddySDK.UI.setFacialExpression(FacialExpression.THINKING);
        else if (randomNumber == 2) BuddySDK.UI.setFacialExpression(FacialExpression.SURPRISED);
        else BuddySDK.UI.setFacialExpression(FacialExpression.LOVE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
        }, 2000);
    }

    private void checkBodyTouchAndReact() {
        if (BuddySDK.Sensors.BodyTouchSensors().RightShoulder().isTouched()) {
            if (!cooking & speaking.equals("False")) {
                //buddyController.runBehaviour(select_animation());
                save_timestamp();
                select_face();
                activate_recognizer();
            }
        }

        if (BuddySDK.Sensors.BodyTouchSensors().LeftShoulder().isTouched()) {
            if (!cooking & speaking.equals("False")) {
                //buddyController.runBehaviour(select_animation());
                save_timestamp();
                select_face();
                activate_recognizer();
            }
        }

        if (BuddySDK.Sensors.BodyTouchSensors().Torso().isTouched()) {
            Log.i("info", "TOUCHED");
            if (!cooking & speaking.equals("False")) {
                save_timestamp();
                select_face();
                activate_recognizer();
            }
        }

        if ((BuddySDK.Sensors.HeadTouchSensors().Top().isTouched() ||
                BuddySDK.Sensors.HeadTouchSensors().Left().isTouched() ||
                BuddySDK.Sensors.HeadTouchSensors().Right().isTouched())) {
            if(dancing) stop_dance();
            save_timestamp();
            if(saying_name){
                saying_name=false;
            }
            if(!presented){
                //sayText("Io mi chiamo Sonrie e sono presente");
                presented=true;
            }
            if(!touching){
                touching=true;
            }

            next = true;
            Log.i("info", "TOUCHED");
            if (!cooking & speaking.equals("False")) {
                select_face();
                activate_recognizer();
            }

            if(singing && touching){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stop_song();
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        buddyController.stop_autonomous_movement();
        recognizer_state = "stopped";
        handler.removeCallbacksAndMessages(null);  // Stop the handler when the activity is destroyed
    }


    private Map<String, String> loadConfigFromYaml() {
        try {
            // Load the YAML file from res/raw
            InputStream inputStream = getResources().openRawResource(R.raw.config_dialog_buddy);
            Yaml yaml = new Yaml();
            return yaml.load(inputStream); // Load YAML data into a Map
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null in case of failure
        }
    }


    private void turn_left() {
        recognizer_state = "stopped";
        buddyController.enableWheels(1, 1);
        buddyController.rotateBuddy(50, -90);
        activate_recognizer();
    }

    private void turn_right() {
        recognizer_state = "stopped";
        buddyController.enableWheels(1, 1);
        buddyController.rotateBuddy(50, 90);
        activate_recognizer();
    }


    private void initializeRecognizer() {
        SpeechConfig config = SpeechConfig.fromSubscription(SUBSCRIPTION_KEY, SERVICE_REGION);
    }


    private void recognizeSpeechOnce() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initializeRecognizer();
        }
        try {
            recognizer_activated = "True";
            SpeechConfig config = SpeechConfig.fromSubscription(SUBSCRIPTION_KEY, SERVICE_REGION);
            List<String> languages = Arrays.asList("it-IT");
            AutoDetectSourceLanguageConfig autoDetectSourceLanguageConfig = AutoDetectSourceLanguageConfig.fromLanguages(languages);

            recognizer = new SpeechRecognizer(config, autoDetectSourceLanguageConfig);

            // Recognize speech once and handle it synchronously
            Future<SpeechRecognitionResult> futureResult = recognizer.recognizeOnceAsync();

            // Wait for the result to be available
            SpeechRecognitionResult result = futureResult.get();

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                save_sentence(result.getText());
                Log.i("info", "Recognized: " + result.getText());
                recognized_sentence = result.getText();
                recognizer_activated = "False";
                listened = true;
                if (!recognizer_state.equals("stopped")) askGPTResponse(result.getText());
            } else if (result.getReason() == ResultReason.NoMatch) {
                Log.i("info", "No speech could be recognized.");
                recognizer_activated = "False";
                activate_recognizer();
            }

        } catch (Exception e) {
            Log.i("info", "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void activate_recognizer() {
        if (recognizer_state.equals("active") && recognizer_activated.equals("False")) {
            Thread recognitionThread = new Thread(() -> {
                try {
                    recognizeSpeechOnce();
                } catch (Exception e) {
                    Log.e("RecognizerThread", "Error during speech recognition: " + e.getMessage(), e);
                }
            });
            recognitionThread.start();
        }
    }


    private void start_dance(){
        recognizer_state="stopped";
        new Thread(() -> {
            try {
                Thread.sleep(500);
                sayText("Mi è piaciuta tantissimo la canzone che mi avete insegnato alcune settimane fa, quella che parlava di un treno che fischia. Vi piacerebbe cantarla di nuovo insieme?");
                BuddySDK.USB.blinkAllLed("#00FF00", 100, new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        Log.i("coucou", "Message received: " + s);
                    }

                    @Override
                    public void onFailed(String s) throws RemoteException {
                        Log.e("coucou", "LED Blink Failed: " + s);
                    }
                });

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }).start();

        buddyController.enable_yes(true);

    }


    private void stop_dance(){

        BuddySDK.USB.stopAllLed(new IUsbCommadRsp.Stub() {
            @Override
            public void onSuccess(String s) throws RemoteException {
                Log.i("coucou", "Message received : "+ s);
                BuddySDK.USB.updateAllLed("#C3C435",new IUsbCommadRsp.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        Log.i("coucou", "Message received : "+ s);
                        buddyController.enable_yes(false);
                        dancing = false;
                        sayText("Sono Sonrie e anche io sono un macchinista! Che bella canzone, quando tornerò a Robolandia la insegnerò anche ai miei amici e alle mie amiche");
                        start_autonomous_life();
                    }

                    @Override
                    public void onFailed(String s) throws RemoteException {

                    }
                });

            }

            @Override
            public void onFailed(String s) throws RemoteException {

            }
        });
    }

    private void ask_weather(String city){

        get_weather_data(city, new WeatherDataCallback() {
            @Override
            public void onWeatherDataReceived(JSONObject weatherData) throws JSONException {
                // Handle the weather data here
                Log.d("Weather", "Weather data received: " + weatherData.toString());
                String systemMeteoUpdated;
                // Safely extract values from weatherData
                String city = weatherData.getString("city");
                String description = weatherData.getString("description");
                String temperature = weatherData.getString("temperature");

                // Update systemMeteo with the retrieved data
                systemMeteoUpdated = systemMeteo.replace("{CITY}", city)
                        .replace("{WEATHER}", description)
                        .replace("{TEMPERATURE}", temperature);

                // Call askGPTMeteo with the updated system message
                askGPTMeteo(systemMeteoUpdated,"say", "");

            }

            @Override
            public void onError(String errorMessage) {
                // Handle the error here
                Log.e("WeatherError", "Error fetching weather data: " + errorMessage);
            }
        });
    }

    private void askGPTResponse(String userInput) {

        openAIClient.queryGPT(userInput, systemMessage, history_messages, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // runOnUiThread(() -> responseTextView.setText("Request failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray choices = jsonObject.getJSONArray("choices");
                        String gptResponse = choices.getJSONObject(0).getJSONObject("message").getString("content");

                        //runOnUiThread(() -> responseTextView.setText(gptResponse));
                        Log.i("info", "GPT response: " + gptResponse);

                        // Convert the string to a JSONObject
                        JSONObject jsonGPT = new JSONObject(gptResponse);

                        String sentenceKey = jsonGPT.getString("sentence");
                        Log.i("info", "KEY: " + sentenceKey);

                        String em = jsonGPT.getString("emotion");
                        set_emotion(em);

                        if (history_messages.length() >= 6) {
                            // Remove the first two elements
                            history_messages.remove(0);
                            history_messages.remove(0);
                        }

                        JSONObject message1 = new JSONObject();
                        message1.put("role", "user");
                        message1.put("content", userInput);
                        history_messages.put(message1);


                        if (recognized_sentence.toLowerCase().contains("destra")) turn_left();
                        else if (recognized_sentence.toLowerCase().contains("sinistra")) turn_right();
                        else if (recognized_sentence.toLowerCase().contains("Genova") & (recognized_sentence.toLowerCase().contains("tempo") || recognized_sentence.toLowerCase().contains("meteo"))) {
                            ask_weather("Genova,it");
                            jsonGPT.put("sentence", meteo_sentence);
                            gptResponse = jsonGPT.toString();
                        }
                        else if (recognized_sentence.toLowerCase().contains("tempo") || recognized_sentence.toLowerCase().contains("meteo")) {
                            String Updatedsystemprompt = systemCity.replace("{TEXT}",recognized_sentence);
                            askGPTMeteo(Updatedsystemprompt,"city", sentenceKey );
                            jsonGPT.put("sentence", meteo_sentence);
                            gptResponse = jsonGPT.toString();
                        }
                        else if (recognized_sentence.toLowerCase().contains("iniziamo") || recognized_sentence.toLowerCase().contains("nuovo amico")) {
                            question = "start";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(presentation.equals("phase0")) {
                                        presentazione();
                                        presentation = "phase1";
                                    }

                                    else if(presentation.equals("phase1")) {
                                        presentazione2();
                                        presentation = "phase2";
                                    }

                                }
                            });
                        }
                        else if (recognized_sentence.toLowerCase().contains("stagione")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ripeti_stagione("primavera");
                                }
                            });
                        }
                        else if (recognized_sentence.toLowerCase().contains("giorno")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ripeti_giorno();
                                }
                            });
                        }

                        else if (recognized_sentence.toLowerCase().contains("balliamo")) {

                                dancing=true;
                                start_dance();

                        }
                        else if (recognized_sentence.toLowerCase().contains("ciao") || recognized_sentence.toLowerCase().contains("a presto") || recognized_sentence.toLowerCase().contains("arrivederci")) {
                            sayText(sentenceKey);
                            Random random = new Random();
                            int randomNumber = random.nextInt(2);
                            if (randomNumber == 1) {
                                show_image("hello");
                            }
                        } else if (recognized_sentence.toLowerCase().contains("saluta in francese")) {
                            sayText("Salut");
                            show_image("hello");
                        } else if (recognized_sentence.toLowerCase().contains("saluta in cinese"))
                            saynihao();
                        else sayText(sentenceKey);
                        set_emotion(em);
                        play_facial_event(em);
                        BuddySDK.UI.setMood(FacialExpression.NEUTRAL);
                        JSONObject message2 = new JSONObject();
                        message2.put("role", "assistant");
                        message2.put("content", gptResponse);
                        history_messages.put(message2);
                        if (question.equals("finished")) activate_recognizer();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.i("info", "GPT responnseerror");
                        //runOnUiThread(() -> responseTextView.setText("JSON parsing error: " + e.getMessage()));
                        if (question.equals("finished")) activate_recognizer();
                    }
                }
            }
        });
    }


    private void askGPTMeteo(String inputsystem, String mode, String resp_old) {


        openAIClient.queryGPTMeteo(inputsystem, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // runOnUiThread(() -> responseTextView.setText("Request failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray choices = jsonObject.getJSONArray("choices");
                        String gptResponse = choices.getJSONObject(0).getJSONObject("message").getString("content");

                        //runOnUiThread(() -> responseTextView.setText(gptResponse));
                        Log.i("info", "GPT response: " + gptResponse);

                        // Convert the string to a JSONObject
                        JSONObject jsonGPT = new JSONObject(gptResponse);
                        if(mode.equals("say")){
                        String sentenceKey = jsonGPT.getString("sentence");
                        Log.i("info", "KEY: " + sentenceKey);

                        sayText(sentenceKey);
                        meteo_sentence = sentenceKey;
                            if (sentenceKey.contains("Genova")){
                                sayText("Conosco tantissimi amici e amiche che vivono in altre parti del mondo. Nei loro paesi oggi il tempo è molto diverso da qui. Volete sapere com’è il meteo in altre parti del mondo?");

                            }
                            if (button_pressed){
                                button_pressed=false;
                                start_autonomous_life();
                            }

                        }
                        else{
                            String detected_city = jsonGPT.getString("city");
                            if(detected_city.equals("")) sayText(resp_old);
                            else ask_weather(detected_city);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.i("info", "GPT responnseerror");
                        //runOnUiThread(() -> responseTextView.setText("JSON parsing error: " + e.getMessage()));
                    }
                }
            }
        });
    }


    private void set_emotion(String em){
        if(em.equals("Happy"))BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY);
        else if(em.equals("Neutral"))BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
        else if(em.equals("Love"))BuddySDK.UI.setFacialExpression(FacialExpression.LOVE);
        else if(em.equals("Surprised"))BuddySDK.UI.setFacialExpression(FacialExpression.SURPRISED);
        else if(em.equals("Sad"))BuddySDK.UI.setFacialExpression(FacialExpression.SAD);
        else if(em.equals("Scared"))BuddySDK.UI.setFacialExpression(FacialExpression.SCARED);
        else if(em.equals("Sick"))BuddySDK.UI.setFacialExpression(FacialExpression.SICK);
        else if(em.equals("Tired"))BuddySDK.UI.setFacialExpression(FacialExpression.TIRED);
        else if(em.equals("Grumpy"))BuddySDK.UI.setFacialExpression(FacialExpression.GRUMPY);
        else if(em.equals("Angry"))BuddySDK.UI.setFacialExpression(FacialExpression.ANGRY);
    }

    private void play_facial_event(String em){
        if(em.equals("Smile"))BuddySDK.UI.playFacialEvent(FacialEvent.SMILE);
        else if(em.equals("blink right eye"))BuddySDK.UI.playFacialEvent(FacialEvent.BLINK_RIGHT_EYE);
        else if(em.equals("Awake"))BuddySDK.UI.playFacialEvent(FacialEvent.AWAKE);
    }

    private void sayText(String text) {
        try {
            if( speaking.equals("False")){
                SpeechConfig config = SpeechConfig.fromSubscription(SUBSCRIPTION_KEY, SERVICE_REGION);
                config.setSpeechSynthesisVoiceName("it-IT-IsabellaMultilingualNeural");
                SpeechSynthesizer synthesizer = new SpeechSynthesizer(config);
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        buddyController.enable_yes(true);
                        buddyController.start_autonomous_head_movements();
                        BuddySDK.UI.setLabialExpression(LabialExpression.SPEAK_NEUTRAL);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }).start();
                speaking="True";
                SpeechSynthesisResult result = synthesizer.SpeakText(text);
                Log.i("EM ","no ex");
                BuddySDK.UI.setLabialExpression(LabialExpression.NO_EXPRESSION);
                Log.i("EM ","finish no ex");
                buddyController.enable_yes(false);
                result.close();
                synthesizer.close();
                speaking="False";
            }

        } catch (Exception e) {
            Log.i("info","Error: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private Runnable hideImageRunnable = new Runnable() {
        @Override
        public void run() {
            imageView.setVisibility(View.GONE);
        }
    };

    private void saynihao(){
        sayText("你好");
        show_image("nihao");
    }

    private void show_image(String Type) {
        Integer drawableId;
        Log.i("TYPE",Type);
        drawableId= (Integer) imageMap.get(Type);
        Log.i("ID", String.valueOf(drawableId));
        runOnUiThread(() -> {
            Drawable im = ContextCompat.getDrawable(this, drawableId);
            if (im != null) {
                imageView.setImageDrawable(im);
                imageView.setVisibility(View.VISIBLE);
                // Cancel any pending hide actions and post a new one
                uiHandler.removeCallbacks(hideImageRunnable);
                uiHandler.postDelayed(hideImageRunnable, 5000);
            } else {
                Log.e("info", "Image resource not found for " + Type);
                imageView.setVisibility(View.GONE);
            }
        });
    }

    private void playSound(int resourceId) {
        // Release the previous MediaPlayer if it exists
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null; // Set to null to avoid memory leaks
        }

        mediaPlayer = MediaPlayer.create(this, resourceId);
        if (mediaPlayer != null) {
            mediaPlayer.start();

            // Stop and release after 5 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop(); // Stop playback if playing
                    }
                    mediaPlayer.release(); // Release resources
                    mediaPlayer = null; // Set to null to avoid memory leaks
                }
            }, 5000); // 5000 milliseconds = 5 seconds

            mediaPlayer.setOnCompletionListener(mp -> {
                if (mp != null) {
                    mp.release();
                }
                mediaPlayer = null; // Set to null to avoid memory leaks
            });
        } else {
            Log.e("INFO", "Failed to create MediaPlayer");
        }
    }


    private void delayedResponse(Runnable soundAction, Runnable imageAction) {
        buddyController.stop_autonomous_movement();
        handler.postDelayed(() -> {
            soundAction.run();  // Play sound
            imageAction.run();  // Show image
        }, 1000); // Adjust the delay time if needed
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        videoPlayer.onRequestPermissionsResult(requestCode, grantResults);
    }

}





