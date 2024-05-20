package com.example.woobipass_test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login); // "login" 대신에 "login.xml" 파일을 로드하는 이름으로 변경

        // 로그인 버튼을 찾아서 클릭 이벤트를 설정합니다.
        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 로그인 버튼을 클릭하면 MainActivity로 이동합니다.
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // 로그인 액티비티를 종료합니다.
            }
        });
    }
}
