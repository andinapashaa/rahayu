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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class TakePicture extends AppCompatActivity {
    ImageView logoImage;
    private static final int KODE_KAMERA = 222;
    private static final int PERMISSION_REQUEST = 223;
    String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_result);

        Button takePhoto = findViewById(R.id.takePicture);
        Button confirmPhoto = findViewById(R.id.confirmPhoto);

        logoImage = findViewById(R.id.resultPicture);

        requestPermissions();

        View.OnClickListener buttonClickListener = view -> {
            if (view.getId() == R.id.takePicture) {
                Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // Menyimpan gambar di folder hasil foto di external storage
                File imagesFolder = new File(getExternalFilesDir(null), "HasilFoto");
                if (!imagesFolder.exists()) {
                    imagesFolder.mkdirs();
                }

                Date d = new Date();
                CharSequence s = DateFormat.format("yyyyMMdd-hh-mm-ss", d.getTime());
                filePath = imagesFolder + File.separator + s.toString() + ".jpg";
                File image = new File(filePath);

                Uri uriSavedImage = FileProvider.getUriForFile(TakePicture.this, getApplicationContext().getPackageName() + ".provider", image);
                it.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);

                startActivityForResult(it, KODE_KAMERA);
            } else if (view.getId() == R.id.confirmPhoto) {
                // Konfirmasi foto dan kirim ke API
                processImage();
                Intent intent = new Intent(TakePicture.this, LastResultActivity.class);
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

        // Menampilkan gambar di ImageView
        logoImage.setImageBitmap(bitmap);
    }

    private void processImage() {
        // Mengonversi gambar ke Base64 dan mengirimkannya ke API
        new Thread(() -> {
            try {
                File file = new File(filePath);
                FileInputStream fileInputStreamReader = new FileInputStream(file);
                byte[] bytes = new byte[(int) file.length()];
                fileInputStreamReader.read(bytes);
                fileInputStreamReader.close();
                String encodedFile = Base64.encodeToString(bytes, Base64.NO_WRAP);

                String API_KEY = "I1zjcfo8SezNL5iY53Ak"; // Ganti dengan API key Anda
                String MODEL_ENDPOINT = "final-project-mobile-programming/2"; // Endpoint model

                // URL untuk request API
                String uploadURL = "https://detect.roboflow.com/" + MODEL_ENDPOINT + "?api_key=" + API_KEY + "&name=YOUR_IMAGE.jpg";

                // HttpURLConnection untuk mengirimkan gambar
                URL url = new URL(uploadURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(encodedFile.getBytes().length));
                connection.setUseCaches(false);
                connection.setDoOutput(true);

                // Mengirim data
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(encodedFile);
                wr.close();

                // Membaca respons
                InputStream stream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();

                Log.d("TakePicture", "Response: " + response.toString());

                // Mengirim respons API ke LastResultActivity
                Intent intent = new Intent(TakePicture.this, LastResultActivity.class);
                intent.putExtra("filePath", filePath); // Kirim path gambar
                intent.putExtra("apiResponse", response.toString()); // Kirim respons API
                startActivity(intent);

            } catch (Exception e) {
                Log.e("TakePicture", "Error: " + e.getMessage(), e);
            }
        }).start();
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
