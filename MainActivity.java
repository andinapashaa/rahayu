package com.example.rahayu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView clickText = findViewById(R.id.clickText);

        clickText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Menavigasi ke Activity untuk mengambil foto
                Intent intent = new Intent(MainActivity.this, TakePicture.class);
                startActivity(intent);
            }
        });
    }
}
