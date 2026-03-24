package com.rice.momentocostruzionisolari;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bfr.buddy.ui.shared.FacialEvent;
import com.bfr.buddy.ui.shared.FacialExpression;
import com.bfr.buddy.ui.shared.LabialExpression;
import com.bfr.buddy.utils.values.FloatingWidgetVisibility;
import com.bfr.buddysdk.BuddyActivity;
import com.bfr.buddysdk.BuddyCompatActivity;
import com.bfr.buddysdk.BuddySDK;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.io.FileOutputStream;
import java.io.IOException;


import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;


import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;



public class MainActivity extends BuddyCompatActivity {

    private static final String SUBSCRIPTION_KEY = "";
    private static final String SERVICE_REGION = "westeurope";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private ImageButton button_start;
    private ImageButton button_stop;
    private ImageButton button_go;
    private ImageButton button_costruzioni;
    private ImageButton button_back;
    private ImageView imageView;
    private ImageView imageView_yara;
    private ImageView imageView_akio;
    private ImageView imageView_maria;
    private ImageView imageView_giulio;
    private ImageView imageView_vish;
    private SpeechRecognizer recognizer;
    private BuddyController buddyController=new BuddyController();
    TextView responseTextView;
    private OpenAIClient openAIClient;
    private String systemMessage;
    JsonLoader jsonLoader = new JsonLoader();
    Map<String, Object> jsonMap;
    Map<String, Object> emotionsMap;
    private static final String videoFilePath = "/storage/emulated/0/Movies/Video1.mp4";  // Path to the video
    private VideoPlayer videoPlayer;
    private Handler uiHandler;
    private Handler handler = new Handler();
    Map<String, Integer> imageMap;
    private MediaPlayer mediaPlayer;
    private String recognizer_state;
    private String recognizer_activated;
    private String speaking;
    private Boolean listened;
    private String recognized_sentence;
    private Boolean clicked;
    private String question;
    private JSONArray history_messages;
    private Boolean building;
    private String casa;
    private Boolean touched;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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


    }

    // Will be called once all BFR services are initialized.
    @Override
    public void onSDKReady() {
        // transfer the touch information to BuddyCore in the background
        BuddySDK.UI.setViewAsFace(findViewById(R.id.view_face));
        button_start = findViewById(R.id.buttonstart);
        button_start.setOnClickListener(view->{
            recognizer_state="active";
            activate_recognizer();});
        button_stop = findViewById(R.id.buttonstop);
        button_stop.setOnClickListener(view->{
            recognizer_state="stopped";
        });

        button_go = findViewById(R.id.buttongo);
        button_go.setOnClickListener(view->endcostruzioni());

        button_costruzioni = findViewById(R.id.buttoncostruzioni);
        button_costruzioni.setOnClickListener(view->{
            startstoria();
        });

        button_back = findViewById(R.id.buttonback);
        button_back.setOnClickListener(view->{
            show_case();
        });

        responseTextView = findViewById(R.id.textView);
        imageView=findViewById(R.id.imageView);

        imageView_yara=findViewById(R.id.casa_yara);
        imageView_yara.setOnClickListener(view->show_casa_yara());


        imageView_maria=findViewById(R.id.casa_maria);
        imageView_maria.setOnClickListener(view->show_casa_maria());


        imageView_vish=findViewById(R.id.casa_vish);
        imageView_vish.setOnClickListener(view->show_casa_vish());

        imageView_akio=findViewById(R.id.casa_akio);
        imageView_akio.setOnClickListener(view->show_casa_akio());

        imageView_giulio=findViewById(R.id.casa_caterina);
        imageView_giulio.setOnClickListener(view->show_casa_giulio());



        imageMap = new HashMap<String, Integer>() {{
            put("hello", R.drawable.hello_gif);
            put("nihao", R.drawable.nihao);
            put("yara", R.drawable.casa_yara);
            put("samir", R.drawable.casa_samir);
            put("luca", R.drawable.casa_luca);
            put("akio", R.drawable.casa_akio);
            put("maria", R.drawable.casa_maria);
            put("omar", R.drawable.casa_omar);
            put("giulio", R.drawable.casa_giulio);
            put("mihai", R.drawable.casa_mihai);
            put("vish", R.drawable.casa_vish);
        }};

        Map<String, String> config = loadConfigFromYaml();
        videoPlayer = new VideoPlayer(this, videoFilePath);
        uiHandler = new Handler(Looper.getMainLooper());

        recognizer_state="active";
        recognizer_activated="False";
        speaking="False";
        recognized_sentence="";
        clicked=false;
        building = false;
        buddyController.start_autonomous_head_movements();
        history_messages = new JSONArray();
        touched = false;
        if (config != null) {
            String apiKey = config.get("api_key");
            String organizationId = config.get("organization_id");
            systemMessage = config.get("system_message2");
            Log.i("info", "system message: " + systemMessage);
            openAIClient = new OpenAIClient(apiKey, organizationId);
        } else {
            // responseTextView.setText("Failed to load configuration.");
        }
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        startBodyTouchSensorCheck();
        sayText("Ciao amici, sono venuto a trovarvi, come state?", "buddy");
        listen_response();
        start_autonomous_life();
    }


    private void  wait_touch(){
        touched=false;
        while (!touched) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.i("main", "Thread interrupted", e);
            }
        }
        touched=false;
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
        File file = new File(dir, String.format("costruzioni_solari_%d_%02d_%02d_timestamp.txt", year, month, day));


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
        File file = new File(dir, String.format("costruzioni_solari_%d_%02d_%02d_sentence.txt", year, month, day));

        // Write timestamp and sentence to file
        try (FileWriter writer = new FileWriter(file, true)) { // true = append mode
            writer.write(timestamp + " " + sentence + "\n");
            writer.flush(); // Ensure it's written
            System.out.println("Sentence saved: " + timestamp + " " + sentence);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void start_autonomous_life(){
        button_start.setVisibility(View.VISIBLE);
        button_stop.setVisibility(View.VISIBLE);
        button_costruzioni.setVisibility(View.VISIBLE);
        question="finished";
        recognizer_state="active";
        activate_recognizer();
    }


    private void show_case(){
        recognizer_state="stopped";
        building = false;
        button_start.setVisibility(View.GONE);
        button_stop.setVisibility(View.GONE);
        button_back.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            button_go.setVisibility(View.VISIBLE);
            imageView_akio.setVisibility(View.VISIBLE);
            imageView_vish.setVisibility(View.VISIBLE);
            imageView_maria.setVisibility(View.VISIBLE);
            imageView_yara.setVisibility(View.VISIBLE);
            imageView_giulio.setVisibility(View.VISIBLE);
        }, 1000);

    }


    private void costruzione_case(){
        button_start.setVisibility(View.VISIBLE);
        button_stop.setVisibility(View.VISIBLE);
        button_back.setVisibility(View.VISIBLE);
        recognizer_state="active";
        building = true;
        activate_recognizer();
    }



    private void show_casa_yara(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("La mia amica Yara è una bambina che vive nel Nord Africa, precisamente in Marocco e vive in una casa con un cortile interno chiamato riad ","buddy");
            sayText("رياض","buddy");
            sayText("Nel mio cortile crescono aranci profumati, e quando fa caldo ci sediamo sotto gli alberi a bere tè alla menta.","loto");
            casa = "riad";
            imageView.setImageResource(R.drawable.casa_yara);
            imageView.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
            wait_touch();
            imageView.setVisibility(View.GONE);
            sayText("La vogliamo costruire insieme? ","buddy");
            costruzione_case();
            }, 500);

            }, 1000);
    }

    private void show_casa_samir(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Il mio amico Samir, dall'Africa, racconta: ","buddy");
            sayText("La mia casa è fatta di fango e paglia,  sei chiama rondavel. È fresca anche quando il sole è caldo! Sotto il grande baobab vicino, raccontiamo storie alla luce delle stelle.","loto");
            sayText("La vogliamo costruire insieme? ","buddy");

            casa ="rondavel";

            imageView.setImageResource(R.drawable.casa_samir);
            imageView.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    wait_touch();
                    imageView.setVisibility(View.GONE);
                    sayText("La vogliamo costruire insieme? ","buddy");
                    costruzione_case();
                }, 500);

        }, 1000);
    }

    private void show_casa_maria(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("La mia amica Maria, dal Sud America, vive in una casa colorata fatta di mattoni che si chiama fazendas.","buddy");
            sayText("La mia casa è piena di colori: giallo, blu e rosso. Quando piove, si sente il suono della pioggia sul tetto","loto");

            casa = "fazendas";
            imageView.setImageResource(R.drawable.casa_maria);
            imageView.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                wait_touch();
                imageView.setVisibility(View.GONE);
                sayText("La vogliamo costruire insieme? ","buddy");
                costruzione_case();
            }, 500);

        }, 1000);
    }

    private void show_casa_luca(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Nel cuore dell’Italia, vicino alla storica città di Perugia, vive un mio amico di nome Luca. Lui racconta con affetto:","buddy");
            sayText("La mia casa è accogliente, con un caratteristico tetto rosso e un grande caminetto. Durante l'inverno, quando fuori fa freddo, ci riuniamo tutti insieme accanto al fuoco, avvolti dal suo calore, e trascorriamo le serate raccontandoci storie, immersi in un’atmosfera magica.","loto");

            casa ="casa";
            imageView.setImageResource(R.drawable.casa_luca);
            imageView.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                wait_touch();
                imageView.setVisibility(View.GONE);
                sayText("La vogliamo costruire insieme? ","buddy");
                costruzione_case();
            }, 500);

        }, 1000);
    }

    private void show_casa_vish(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Il mio amico Vish vive in India, in un posto circondato da alte montagne e verdi boschi:", "buddy");
            sayText("La mia casa si chiama घर (ghar) ed è costruita con tanti mattoni per resistere al vento. Quando arrivano le forti piogge chiamati monsoni, il tetto spiovente fa scorrere via tutta l’acqua! Nella mia casa, la sera mangiamo insieme il Patrodé, un piatto buonissimo fatto di riso, erbe selvatiche, cocco e altre spezie profumate","loto");


            casa ="ghar";
            imageView.setImageResource(R.drawable.casa_vish);
            imageView.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                wait_touch();
                imageView.setVisibility(View.GONE);
                sayText("La vogliamo costruire insieme? ","buddy");
                costruzione_case();
            }, 500);

        }, 1000);
    }

    private void show_casa_akio(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Il mio amico Akio, dal Giappone, dice: ","buddy");
            sayText("La mia casa è fatta di legno e carta e si chiama minka (民家)! Quando arriva la primavera, apriamo le porte scorrevoli chiamate shōji . Poi guardiamo i fiori di ciliegio.","loto");
            casa = "minka";
            imageView.setImageResource(R.drawable.casa_akio);
            imageView.setVisibility(View.VISIBLE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                wait_touch();
                imageView.setVisibility(View.GONE);
                sayText("La vogliamo costruire insieme? ","buddy");
                costruzione_case();
            }, 500);

        }, 1000);
    }


    private void show_casa_mihai(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Il mio amico Mihai vive in Romania, in un villaggio circondato da colline verdi e boschi profumati.","buddy");
            sayText("La mia casa si chiama","buddy");
            sayText("“casă țărănească”","buddy");
            sayText("è costruita con legno e mattoni. Le pareti sono bianche con le decorazioni blu, e il tetto è rosso e spiovente, perfetto per affrontare la neve in inverno. Le finestre di legno si aprono su un grande cortile fiorito, dove in estate le galline razzolano felici e la mia nonna stende il bucato profumato al sole","loto");
            show_image("mihai");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                sayText("La vogliamo costruire insieme? ","buddy");
                casa="casă țărănească";
                imageView.setImageResource(R.drawable.casa_mihai);
                imageView.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    wait_touch();
                    imageView.setVisibility(View.GONE);
                    costruzione_case();
                }, 500);
            }, 8000);
        }, 1000);
    }


    private void show_casa_giulio(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("La mia amica Caterina dall’Italia del nord, in una città vicino al mare chiamata Genova, racconta:","buddy");
            sayText("La mia casa è alta e ha le pareti gialle e rosse, come i colori del sole al tramonto. Si affaccia su stradine strette strette, chiamate “caruggi”. Quando soffia il vento di mare, le sue persiane verdi si aprono e si chiudono, come le ali di gabbiani.","loto");

            show_image("giulio");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                sayText("La vogliamo costruire insieme? ","buddy");
                casa= "casa";
                imageView.setImageResource(R.drawable.casa_giulio);
                imageView.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    wait_touch();
                    imageView.setVisibility(View.GONE);
                    costruzione_case();
                }, 500);
            }, 8000);
        }, 1000);
    }

    private void show_casa_omar(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Il mio amico Omar, dall’Egitto, vive in una casa circondata dal deserto dorato","buddy");
            sayText("La mia casa si chiama Bayt\" (بيت). È fatta di mattoni di fango e sabbia e le sue pareti sono color del sole che splende in cielo. Nei giorni più caldi, ci sdraiamo nel cortile sotto il grande albero di fico che offre ombra e frutti dolci","loto");
            show_image("omar");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                sayText("La vogliamo costruire insieme? ","buddy");
                casa = "Bayt";
                imageView.setImageResource(R.drawable.casa_omar);
                imageView.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    wait_touch();
                    imageView.setVisibility(View.GONE);
                    costruzione_case();
                }, 500);
            }, 8000);
        }, 1000);
    }

    private void startstoria() {
        button_start.setVisibility(View.GONE);
        button_stop.setVisibility(View.GONE);
        button_costruzioni.setVisibility(View.GONE);
        recognizer_state = "stopped";
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Mi sono piaciute tanto le case che mi avete raccontato","buddy");
            sayText("vi va di costruire insieme una casetta con i materiali che sono qui? Io conosco una storia di un villaggio speciale chiamato \"Il Paese delle Case\", dove ogni casa è diversa e racconta una storia. Nel villaggio vivono tanti bambini, ognuno con una casa unica. Ogni mattina, i bambini si incontrano per giocare insieme e scoprire le meraviglie delle loro abitazioni.","buddy");
            sayText("Siete pronti a cominciare?","buddy");
            listen_response();
            show_case();
        }, 1000);
    }


    private void endcostruzioni(){
        button_go.setVisibility(View.GONE);
        imageView_akio.setVisibility(View.GONE);
        imageView_vish.setVisibility(View.GONE);
        imageView_maria.setVisibility(View.GONE);
        imageView_yara.setVisibility(View.GONE);
        imageView_giulio.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sayText("Avete visto? Ogni casa è diversa: nelle città più grandi ho potuto vedere anche grattacieli altissimi, mentre nei boschi delle piccole casine fatte tutte di legno. Quando sono stato al Polo Nord, ho visto anche case fatte tutte di ghiaccio. E ce ne sono ancora tantissime altre, ognuna speciale a modo suo! Ogni casa è diversa, proprio come ognuno di noi. Ma tutte le case hanno qualcosa in comune: sono il posto dove ci sentiamo al sicuro e felici. ","buddy");
            sayText("Ora volete costruire un grande villaggio con le case del mondo?","buddy");
            listen_response();
            start_autonomous_life();
            }, 1000);
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
            if (System.currentTimeMillis() - startTime >= 10000) {
                System.out.println("Timeout reached, exiting loop.");
                break;
            }
        }
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

    private String select_animation(){
        Random random = new Random();
        int randomNumber = random.nextInt(5);
        if(randomNumber == 0) return "Happy";
        else if(randomNumber == 1) return "Smile";
        else if(randomNumber == 2) return "Surprised";
        else if(randomNumber == 2) return "Whistle";
        else if(randomNumber == 2) return "Dance";
        else return "Love";
    }

    private void select_face(){
        Random random = new Random();
        int randomNumber = random.nextInt(6);
        if(randomNumber == 0) BuddySDK.UI.setFacialExpression(FacialExpression.HAPPY);
        else if(randomNumber == 1) BuddySDK.UI.setFacialExpression(FacialExpression.THINKING);
        else if(randomNumber == 2) BuddySDK.UI.setFacialExpression(FacialExpression.SURPRISED);
        else BuddySDK.UI.setFacialExpression(FacialExpression.LOVE);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            BuddySDK.UI.setFacialExpression(FacialExpression.NEUTRAL);
        }, 2000);
    }

    private void checkBodyTouchAndReact() {
        if (BuddySDK.Sensors.BodyTouchSensors().RightShoulder().isTouched()) {
            if (speaking.equals("False")) {
                save_timestamp();
                select_face();
                activate_recognizer();
            }
        }

        if (BuddySDK.Sensors.BodyTouchSensors().LeftShoulder().isTouched()) {
            if (speaking.equals("False")) {
                save_timestamp();
                select_face();
                activate_recognizer();
            }
        }

        if (BuddySDK.Sensors.BodyTouchSensors().Torso().isTouched()) {
            Log.i("info", "TOUCHED");
            if (speaking.equals("False")) {
                save_timestamp();
                select_face();
                activate_recognizer();
            }
        }

        if ((BuddySDK.Sensors.HeadTouchSensors().Top().isTouched() ||
                BuddySDK.Sensors.HeadTouchSensors().Left().isTouched() ||
                BuddySDK.Sensors.HeadTouchSensors().Right().isTouched())) {

            touched=true;
            Log.i("info", "TOUCHED");
            if (speaking.equals("False")) {
                if(building){
                    Random random = new Random();
                    int num = random.nextInt(8);
                    String sentence_to_say="Ma che bella "+casa+" che state costruendo";
                    if(num==1) sentence_to_say="Che bello!";
                    if(num==2) sentence_to_say="State costruendo una "+casa+" bellissima";
                    if(num==3) sentence_to_say="è meravigliosa la vostra "+casa;
                    if(num==4) sentence_to_say="Wow, la vostra "+casa+" è bellissima";
                    if(num==5) sentence_to_say="Avete un talento nel costruire "+casa;
                    if(num==6) sentence_to_say="Vorrei andare a abitare nella vostra "+casa+". è favolosa ";
                    if(num==7) sentence_to_say="Che meraviglia";
                    sayText(sentence_to_say,"buddy");
                }
                save_timestamp();
                select_face();
                activate_recognizer();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        buddyController.stop_autonomous_movement();
        recognizer_state="stopped";
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
        recognizer_state="stopped";
        buddyController.enableWheels(1, 1);
        buddyController.rotateBuddy(50, -90);
        activate_recognizer();
    }

    private void turn_right() {
        recognizer_state="stopped";
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
            recognizer_activated="True";
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
                recognized_sentence=result.getText();
                recognizer_activated="False";
                listened = true;
                if (!recognizer_state.equals("stopped")) askGPTResponse(result.getText());
            }


            else if (result.getReason() == ResultReason.NoMatch) {
                Log.i("info", "No speech could be recognized.");
                recognizer_activated="False";
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

                            history_messages.remove(0);
                            history_messages.remove(0);
                        }

                        JSONObject message1 = new JSONObject();
                        message1.put("role", "user");
                        message1.put("content", userInput);
                        history_messages.put(message1);

                        if (recognized_sentence.toLowerCase().contains("destra")) turn_left();
                        else if (recognized_sentence.toLowerCase().contains("sinistra"))turn_right();
                        else if(recognized_sentence.toLowerCase().contains("iniziamo")){
                            if(building==false){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    startstoria();
                                }
                            });}
                        }
                        else if(recognized_sentence.toLowerCase().contains("ciao") || recognized_sentence.toLowerCase().contains("a presto")|| recognized_sentence.toLowerCase().contains("arrivederci")){
                            sayText(sentenceKey,"buddy");
                            Random random = new Random();
                            int randomNumber = random.nextInt(2);
                            if (randomNumber == 1 ) {
                                show_image("hello");
                            }
                        }
                        else if(recognized_sentence.toLowerCase().contains("saluta in francese")){sayText("Salut","buddy");show_image("hello");}
                        else if(recognized_sentence.toLowerCase().contains("saluta in cinese")) saynihao();
                        else sayText(sentenceKey,"buddy");
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
                        if (question.equals("finished"))activate_recognizer();
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

    private void sayText(String text, String voice) {
        try {
            if( speaking.equals("False")){
                SpeechConfig config = SpeechConfig.fromSubscription(SUBSCRIPTION_KEY, SERVICE_REGION);
                if(voice.equals("drago")){
                    config.setSpeechSynthesisVoiceName("it-IT-LisandroNeural");
                }
                else if (voice.equals("loto")){config.setSpeechSynthesisVoiceName("it-IT-PierinaNeural");}
                else{config.setSpeechSynthesisVoiceName("it-IT-IsabellaMultilingualNeural");}
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
        sayText("你好","buddy");
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
        handler.postDelayed(() -> {
            soundAction.run();
            imageAction.run();
        }, 10);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        videoPlayer.onRequestPermissionsResult(requestCode, grantResults);
    }
}





