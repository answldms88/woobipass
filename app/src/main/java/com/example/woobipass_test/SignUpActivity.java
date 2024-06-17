package com.example.woobipass_test;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        db = FirebaseFirestore.getInstance();

        final EditText editTextId = findViewById(R.id.studentnum);
        final EditText editTextPassword = findViewById(R.id.password);
        final EditText editTextEmail = findViewById(R.id.email);
        Button signUpButton = findViewById(R.id.signup_button);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = editTextId.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String email = editTextEmail.getText().toString().trim();

                if (!id.isEmpty() && !password.isEmpty() && !email.isEmpty()) {
                    createUser(id, password, email);
                } else {
                    Toast.makeText(SignUpActivity.this, "모든 필드를 채워주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createUser(String id, String password, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("password", password);
        user.put("email", email);

        db.collection("users").document(id)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(SignUpActivity.this, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
