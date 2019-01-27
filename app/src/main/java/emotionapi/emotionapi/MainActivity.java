package emotionapi.emotionapi;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView; // variable to hold the image view in our activity_main.xml
    private TextView resultText; // variable to hold the text view in our activity_main.xml
    private static final int RESULT_LOAD_IMAGE = 100;
    private static final int REQUEST_CAMERA_CODE = 300;
    private static final int REQUEST_PERMISSION_CODE = 200;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView mImageView;

    Random rand = new Random();
    int pageNumber = rand.nextInt(100) + 1;


    //Google Custom Search API Authentication
    String cxKey = "011259080533467582698:_oz0hfbrhfe";
    String apiKey = "AIzaSyAFaaXEHYtqpWIPfz8qvY9DgxDozAaFJFk";

    String color = "white"; //default to neutral color
    String queryURL = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cxKey +
            "&imgSize=large" +
            "&imgType=photo" +
            "&start=" + String.valueOf(pageNumber) +
            "&q=wallpaper+400x800+" +
            "&imgDominantColor=" + color +
            "&searchType=image" +
            "&fields=url,items(link)";

    HashMap<Integer, Pair<String, String>> emotionColorPairs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initEmotionColorMap();
        // initiate our image view and text view
        imageView = (ImageView) findViewById(R.id.imageView);
        resultText = (TextView) findViewById(R.id.resultText);
    }


    public void getCameraImage(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_CAMERA_CODE);
        }
    }


    // when the "GET IMAGE" Button is clicked this function is called
    public void getGalleryImage(View view) {
        // check if user has given us permission to access the gallery
        if (checkPermission()) {
            Intent choosePhotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(choosePhotoIntent, RESULT_LOAD_IMAGE);
        } else {
            requestPermission();
        }
    }


    // when the "SET WALLPAPER" Button is clicked this function is called
    public void setWallpaper(View view){
        //Parses JSON Object and sets Wallpaper
        WallpaperFactory url = new WallpaperFactory();
        url.execute();
    }


    // This function gets the selected picture from the gallery and shows it on the image view
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // get the photo URI from the gallery, find the file path from URI and send the file path to ConfirmPhoto
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            // a string variable which will store the path to the image in the gallery
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            imageView.setImageBitmap(bitmap);

//            processImageEmotion();
        }

        if (requestCode == REQUEST_CAMERA_CODE && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
        }

        processImageEmotion();
    }


    // convert image to base 64 so that we can send the image to Emotion API
    public byte[] toBase64(ImageView imgPreview) {
        Bitmap bm = ((BitmapDrawable) imgPreview.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        return baos.toByteArray();
    }


    // if permission is not given we get permission
    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_EXTERNAL_STORAGE, CAMERA}, REQUEST_PERMISSION_CODE);
    }


    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        return result == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
    }


    // asynchronous class which makes the api call in the background
    private class GetEmotionCall extends AsyncTask<Void, Void, String> {

        private final ImageView img;

        GetEmotionCall(ImageView img) {
            this.img = img;
        }

        // this function is called before the api call is made
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resultText.setText("Getting results...");
        }

        // this function is called when the api call is made
        @Override
        protected String doInBackground(Void... params) {
            HttpClient httpclient = HttpClients.createDefault();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            try {
                //URIBuilder builder = new URIBuilder("https://southcentralus.api.cognitive.microsoft.com/emotion/v1.0/recognize");
                URIBuilder builder = new URIBuilder("https://southcentralus.api.cognitive.microsoft.com/face/v1.0/detect");

                builder.setParameter("returnFaceId", "false");
                builder.setParameter("returnFaceLandmarks", "false");
                builder.setParameter("returnFaceAttributes", "emotion");

                URI uri = builder.build();
                HttpPost request = new HttpPost(uri);
                //octet-stream
                request.setHeader("Content-Type", "application/octet-stream");
                // enter you subscription key here
                request.setHeader("Ocp-Apim-Subscription-Key", "6657e3394d214c9295956b5c97e2c2d7");

                // Request body.The parameter of setEntity converts the image to base64
                request.setEntity(new ByteArrayEntity(toBase64(img)));

                // getting a response and assigning it to the string res
                HttpResponse response = httpclient.execute(request);
                HttpEntity entity = response.getEntity();
                String res = EntityUtils.toString(entity);

                return res;

            } catch (Exception e) {
                return e.getMessage();
            }

        }

        // this function is called when we get a result from the API call
        @Override
        protected void onPostExecute(String result) {
            JSONArray jsonArray = null;
            try {

                Double[] emotions = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};

                JSONArray jArray = new JSONArray(result.trim());
                for(int i =0; i < jArray.length(); i++)
                {
                    JSONObject jObject = jArray.getJSONObject(i).getJSONObject("faceAttributes").getJSONObject("emotion");
                    emotions[0] += jObject.getDouble("anger");
                    emotions[1] += jObject.getDouble("contempt");
                    emotions[2] += jObject.getDouble("disgust");
                    emotions[3] += jObject.getDouble("fear");
                    emotions[4] += jObject.getDouble("happiness");
                    emotions[5] += jObject.getDouble("neutral");
                    emotions[6] += jObject.getDouble("sadness");
                    emotions[7] += jObject.getDouble("surprise");
                }
                double max =emotions[0]; int maxIndex = 0;
                for(int i =0; i < 8; i++)
                {
                    if(max < emotions[i])
                    {
                        max = emotions[i];
                        maxIndex = i;
                    }
                    emotions[i] /= jArray.length();
                }

                updateColor(maxIndex);

                String emotion = emotionColorPairs.get(maxIndex).second;
                resultText.setText(emotion);
            } catch (Exception e) {
                resultText.setText(e.getMessage());
            }
        }
    }

//______________________________________________________________________________________________
//                                  HTTP REQUEST HANDLER
//_____________________________________________________________________________________________

    private class WallpaperFactory extends AsyncTask <Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //Extracts JSON Object from Image Query URL
        @Override
        protected String doInBackground(Void... voids) {
            JsonReader reader = new JsonReader();
            String link = null;
            try {
                JSONObject object = reader.readJsonFromUrl(queryURL);

                JSONArray urlArray = object.getJSONArray("items");
//                for (int i = 0; i < urlArray.length(); i++) {
//                    String link = urlArray.getJSONObject(i).getString("link");
//                    System.out.println(link);
//                }

                link = urlArray.getJSONObject(0).getString("link");

                if(link != null){
                    Bitmap result = Picasso.get().load(link).get();
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                    wallpaperManager.setBitmap(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return link;
        }

        @Override
        protected void onPostExecute(String imageURL) {
            super.onPostExecute(imageURL);

            if (imageURL == null) {
                Toast toast = Toast.makeText(MainActivity.this, "Failed To Set Wallpaper", Toast.LENGTH_LONG );
                toast.show();
            } else {
                System.out.println(imageURL);
                updatePageNumber();
            }
        }
    }

//______________________________________________________________________________________________
//                                   HELPER FUNCTIONS
//______________________________________________________________________________________________

    public void processImageEmotion(){
        // run the GetEmotionCall class in the background
        GetEmotionCall emotionCall = new GetEmotionCall(imageView);
        emotionCall.execute();
    }

    //Reseeds random value for page number
    void updatePageNumber() {
        pageNumber = rand.nextInt(100)+1;

        queryURL = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cxKey +
                "&imgSize=large" +
                "&imgType=photo" +
                "&start=" + String.valueOf(pageNumber) +
                "&q=wallpaper+400x800+" +
                "&imgDominantColor=" + color +
                "&searchType=image" +
                "&fields=url,items(link)";
    }


    void updateColor(int colorKey){
        color = emotionColorPairs.get(colorKey).first;

        queryURL = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + cxKey +
                "&imgSize=large" +
                "&imgType=photo" +
                "&start=" + String.valueOf(pageNumber) +
                "&q=wallpaper+400x800+" +
                "&imgDominantColor=" + color +
                "&searchType=image" +
                "&fields=url,items(link)";
    }


    void initEmotionColorMap() {
        emotionColorPairs = new HashMap<>();

        emotionColorPairs.put(0, new Pair<>("red", "Angry"));
        emotionColorPairs.put(1, new Pair<>("black", "Contempt"));
        emotionColorPairs.put(2, new Pair<>("green" , "Disgust"));
        emotionColorPairs.put(3, new Pair<>("purple", "Fear"));
        emotionColorPairs.put(4, new Pair<>("yellow", "Happy"));
        emotionColorPairs.put(5, new Pair<>("white", "Neutral"));
        emotionColorPairs.put(6, new Pair<>("blue", "Sad"));
        emotionColorPairs.put(7, new Pair<>("orange", "Surprised"));
    }
}
