package com.example.woobipass_test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();

        // 사용자 데이터 초기화
        preferences = getSharedPreferences("user_data", MODE_PRIVATE);

        // "umbrellas" 컬렉션 초기화
        initializeUmbrellasCollection();

        // 지도 버튼 설정
        setupMapButton();

        // QR 코드 스캔 버튼 설정
        setupQRScanButton();

        // 데이터 이미지 버튼 설정
        setupDataImageButton();
    }

    // 지도 버튼 설정
    private void setupMapButton() {
        ImageButton mapButton = findViewById(R.id.jido);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 지도 이미지 액티비티 시작
                Intent intent = new Intent(MainActivity.this, JidoImageActivity.class);
                startActivity(intent);
            }
        });
    }

    // QR 코드 스캔 버튼 설정
    private void setupQRScanButton() {
        ImageButton scanQRButton = findViewById(R.id.scanQR);
        scanQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQRCodeScannerActivity();
            }
        });
    }

    // 데이터 이미지 버튼 설정
    private void setupDataImageButton() {
        ImageButton dataImageButton = findViewById(R.id.data);
        dataImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 사용자에게 현재 우산 상태를 보여주는 메서드 호출
                showCurrentUmbrellaStatus();
            }
        });
    }

    // QRCodeScannerActivity 시작 메서드
    private void startQRCodeScannerActivity() {
        Intent intent = new Intent(MainActivity.this, QRCodeScannerActivity.class);
        startActivity(intent);
    }

    // "umbrellas" 컬렉션 초기화 메서드
    private void initializeUmbrellasCollection() {
        final String[] umbrellaIds = {"umbrella_1", "umbrella_2", "umbrella_3"};
        for (String id : umbrellaIds) {
            final DocumentReference docRef = db.collection("umbrellas").document(id);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (!document.exists()) {
                            Map<String, Object> umbrellaData = new HashMap<>();
                            umbrellaData.put("status", "available");
                            // 타임스탬프 필드는 초기화하지 않음
                            umbrellaData.put("rental_time", FieldValue.serverTimestamp());
                            umbrellaData.put("return_time", FieldValue.serverTimestamp());

                            docRef.set(umbrellaData)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d("FirestoreInit", "DocumentSnapshot successfully written!");
                                            } else {
                                                Log.w("FirestoreInit", "Error writing document", task.getException());
                                            }
                                        }
                                    });
                        }
                    } else {
                        Log.d("FirestoreInit", "get failed with ", task.getException());
                    }
                }
            });
        }
    }

    // 현재 우산 상태를 보여주는 메서드
// 사용자에게 현재 우산 상태를 보여주는 메서드
    private void showCurrentUmbrellaStatus() {
        db.collection("umbrellas")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            StringBuilder statusText = new StringBuilder();
                            for (DocumentSnapshot document : task.getResult()) {
                                String umbrellaId = document.getId();
                                String currentStatus = document.getString("대여상태");

                                // 현재 상태가 "X" (대여 중)이면 대여 중임을 표시
                                if ("X".equals(currentStatus)) {
                                    statusText.append("우산 ID: ").append(umbrellaId)
                                            .append(", 대여상태: ").append("X")
                                            .append("\n");
                                } else {
                                    // 그 외의 경우 "O" (대여 가능)로 표시
                                    statusText.append("우산 ID: ").append(umbrellaId)
                                            .append(", 대여상태: ").append("O")
                                            .append("\n");
                                }
                            }

                            // 사용자에게 현재 우산 상태를 보여주는 다이얼로그를 표시합니다.
                            showStatusAlertDialog(statusText.toString());
                        } else {
                            Log.d("Firestore", "Error getting documents: ", task.getException());
                            Toast.makeText(MainActivity.this, "Error fetching umbrella status.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // 사용자에게 현재 우산 상태를 보여주는 다이얼로그 표시
    private void showStatusAlertDialog(String statusText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("현재 우산 상태")
                .setMessage(statusText)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
