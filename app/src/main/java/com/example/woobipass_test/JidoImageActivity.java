package com.example.woobipass_test;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Button;
import android.view.View;
import android.content.Intent;


public class JidoImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jido_image);

        // map 이미지를 표시할 ImageView 찾기
        ImageView jidoImageView = findViewById(R.id.jidoImageView);

        // map 이미지 설정
        jidoImageView.setImageResource(R.drawable.map);

        // 돌아가는 버튼 찾기
        Button backButton = findViewById(R.id.backButton);

        // 돌아가는 버튼 클릭 리스너 설정
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // MainActivity로 이동하는 인텐트 생성
                Intent intent = new Intent(JidoImageActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}