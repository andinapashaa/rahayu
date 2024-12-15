package com.example.rahayu;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class TakePicture extends AppCompatActivity {
    ImageView logoImage;
    private static final int KODE_KAMERA = 222;
    private static final int PERMISSION_REQUEST = 223;
    String filePath;
    String classValue;

    private static final String API_KEY = "I1zjcfo8SezNL5iY53Ak";
    private static final String MODEL_ENDPOINT = "final-project-mobile-programming/2";
    private static final String UPLOAD_URL = "https://detect.roboflow.com/" + MODEL_ENDPOINT + "?api_key=" + API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_result);

        Button takePhoto = findViewById(R.id.takePicture);
        Button confirmPhoto = findViewById(R.id.confirmPhoto);

        logoImage = findViewById(R.id.resultPicture);

        View.OnClickListener buttonClickListener = view -> {
            if (view.getId() == R.id.takePicture) {
                Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                File imagesFolder = new File(getExternalFilesDir(null), "HasilFoto");
                if (!imagesFolder.exists()) {
                    imagesFolder.mkdirs();
                }

                Date d = new Date();
                CharSequence s = DateFormat.format("yyyyMMdd-hh-mm-ss", d.getTime());
                filePath = imagesFolder + File.separator + s.toString() + ".jpg";
                File image = new File(filePath);

                Uri uriSavedImage = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", image);
                it.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);

                startActivityForResult(it, KODE_KAMERA);
            } else if (view.getId() == R.id.confirmPhoto) {
                Intent intent = new Intent(TakePicture.this, LastResultActivity.class);
                intent.putExtra("result", classValue);
                intent.putExtra("filePath", filePath);
                startActivity(intent);
            }
        };

        takePhoto.setOnClickListener(buttonClickListener);
        confirmPhoto.setOnClickListener(buttonClickListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == KODE_KAMERA && resultCode == Activity.RESULT_OK) {
            loadCapturedPhoto();
        }
    }

    private void loadCapturedPhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        logoImage.setImageBitmap(bitmap);

        Snackbar.make(findViewById(android.R.id.content), "Tunggu Sebentar....", Snackbar.LENGTH_LONG).show();

        new Thread(() -> {
            try {
                String result = processImage(UPLOAD_URL, new File(filePath));
                classValue = result;
                runOnUiThread(() -> {
                    Log.d("Hasil", "Hasil Prediksi: " + result);
                    Snackbar.make(findViewById(android.R.id.content), "Deteksi Berhasil", Snackbar.LENGTH_LONG).show();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String processImage(String requestURL, File file) throws IOException {
        String boundary = UUID.randomUUID().toString();
        String LINE_FEED = "\r\n";
        HttpURLConnection connection = null;
        StringBuilder filteredResult = new StringBuilder();

        try {
            URL url = new URL(requestURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.getName()).append("\"").append(LINE_FEED);
            writer.append("Content-Type: ").append("image/jpg").append(LINE_FEED).append(LINE_FEED).flush();

            FileInputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();

            writer.append(LINE_FEED).flush();
            writer.append("--").append(boundary).append("--").append(LINE_FEED);
            writer.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(result.toString());
                JSONArray predictions = jsonResponse.getJSONArray("predictions");

                for (int i = 0; i < predictions.length(); i++) {
                    JSONObject prediction = predictions.getJSONObject(i);
                    String detectedClass = prediction.getString("class");
                    filteredResult.append("Class: ").append(detectedClass)
                            .append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return filteredResult.toString();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int cameraPermission = this.checkSelfPermission(Manifest.permission.CAMERA);
            int writePermission = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
            }

            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin diberikan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Izin diperlukan untuk menggunakan kamera", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
