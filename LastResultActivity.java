package com.example.rahayu;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LastResultActivity extends AppCompatActivity {

    private ImageView pictureResult;
    private TextView apiResponseText;
    private TextView faceProblemDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lastresult);

        pictureResult = findViewById(R.id.fragment1);
        apiResponseText = findViewById(R.id.faceAnalystDescription);
        faceProblemDescription = findViewById(R.id.faceProblemDescription);
        TextView doneButton = findViewById(R.id.done);

        // Mendapatkan filePath dan apiResponse dari Intent
        String filePath = getIntent().getStringExtra("filePath");
        String apiResponse = getIntent().getStringExtra("apiResponse");

        // Tombol selesai untuk kembali ke MainActivity
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LastResultActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // Tampilkan gambar dari filePath
        if (filePath != null) {
            pictureResult.setImageBitmap(BitmapFactory.decodeFile(filePath));
        } else {
            pictureResult.setImageResource(R.mipmap.ic_launcher); // Gambar default jika filePath kosong
        }

        // Parsing dan menampilkan respons API
        if (apiResponse != null) {
            try {
                JSONObject jsonObject = new JSONObject(apiResponse);
                JSONArray predictionsArray = jsonObject.getJSONArray("predictions");

                if (predictionsArray.length() > 0) {
                    JSONObject firstPrediction = predictionsArray.getJSONObject(0);

                    String className = firstPrediction.optString("class", "Unknown");
                    double confidence = firstPrediction.optDouble("confidence", 0.0);

                    // Menampilkan hasil di TextView
                    String resultText = className;
                    apiResponseText.setText(resultText);

                    // Menampilkan deskripsi berdasarkan kelas
                    String description = getDescriptionForClass(className);
                    faceProblemDescription.setText(description);
                } else {
                    apiResponseText.setText("No predictions found in the API response.");
                }
            } catch (JSONException e) {
                // Tampilkan kesalahan parsing JSON
                apiResponseText.setText("Error parsing API response.");
                Toast.makeText(LastResultActivity.this, "JSON Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            apiResponseText.setText("No API response received.");
        }
    }

    // Fungsi untuk mendapatkan deskripsi berdasarkan kelas
    private String getDescriptionForClass(String className) {
        switch (className) {
            case "acne":
                return "Salicylic Acid: Membersihkan pori-pori tersumbat.\n" +
                        "Benzoyl Peroxide: Membunuh bakteri penyebab jerawat.\n" +
                        "Niacinamide: Mengurangi peradangan dan membantu penyembuhan kulit.";

            case "dark circle":
                return "Vitamin C: Mencerahkan kulit di bawah mata.\n" +
                        "Caffeine: Mengurangi pembengkakan dan meningkatkan sirkulasi darah.\n" +
                        "Niacinamide: Memperbaiki skin barrier dan meratakan warna kulit.";

            case "freckles":
                return "Vitamin C: Mengurangi hiperpigmentasi dan mencerahkan kulit.\n" +
                        "Alpha Arbutin: Menghambat produksi melanin.\n" +
                        "Kojic Acid: Mengatasi hiperpigmentasi dan flek hitam.";

            case "pores":
                return "Niacinamide: Mengurangi produksi minyak dan mengecilkan tampilan pori.\n" +
                        "Salicylic Acid: Membersihkan pori-pori dari minyak dan kotoran.\n" +
                        "Clay (Kaolin/Bentonite): Menyerap minyak.";

            case "scar":
                return "Centella Asiatica: Meredakan kemerahan dan mempercepat penyembuhan.\n" +
                        "Vitamin E: Meningkatkan elastisitas kulit dan membantu regenerasi.\n" +
                        "Allantoin: Menenangkan kulit dan memperbaiki jaringan kulit.";

            case "wreckles":
                return "Retinol: Merangsang produksi kolagen dan meningkatkan elastisitas kulit.\n" +
                        "Vitamin C: Meningkatkan produksi kolagen.\n" +
                        "Ceramides: Memperbaiki skin barrier untuk menjaga kelembapan.";

            default:
                return "No description available for this class.";
        }
    }
}
