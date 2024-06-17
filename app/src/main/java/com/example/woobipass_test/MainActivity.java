package com.example.woobipass_test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();

        // "umbrellas" 컬렉션 초기화
        initializeUmbrellasCollection();

        // 지도 버튼 설정
        setupMapButton();

        // QR 코드 스캔 버튼 설정
        setupQRScanButton();
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
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("FirestoreInit", "DocumentSnapshot successfully written!");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("FirestoreInit", "Error writing document", e);
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
}
