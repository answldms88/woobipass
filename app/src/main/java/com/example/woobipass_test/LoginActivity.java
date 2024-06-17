package com.example.woobipass_test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        db = FirebaseFirestore.getInstance();

        ImageButton loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextId = findViewById(R.id.editTextId);
                EditText editTextPassword = findViewById(R.id.editTextPassword);
                String id = editTextId.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                if (TextUtils.isEmpty(id) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    login(id, password);
                }
            }
        });

        TextView signupText = findViewById(R.id.signup);
        signupText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void login(String id, String password) {
        db.collection("users").document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String savedPassword = document.getString("password");
                                if (password.equals(savedPassword)) {
                                    // 로그인 성공 시 사용자 아이디를 SharedPreferences에 저장
                                    SharedPreferences preferences = getSharedPreferences("user_data", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("user_id", id);
                                    editor.apply();

                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "등록되지 않은 학번입니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}