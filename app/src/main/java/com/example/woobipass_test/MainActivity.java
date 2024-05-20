package com.example.woobipass_test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

        // mapbtn 버튼 찾기
        Button mapButton = findViewById(R.id.mapbtn);

        // mapbtn 버튼 클릭 리스너 설정
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // MapImageActivity 시작
                Intent intent = new Intent(MainActivity.this, JidoImageActivity.class);
                startActivity(intent);
            }
        });

        // QR 코드 스캔 버튼 클릭 리스너 설정
        Button scanQRButton = findViewById(R.id.scanQR);
        scanQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startQRCodeScannerActivity();
            }
        });

        // 반납 버튼 찾기
        Button returnButton = findViewById(R.id.rebtn);

        // 반납 버튼 클릭 리스너 설정
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 반납 버튼이 클릭되었을 때 수행할 작업
                // Firestore의 "우산 수"를 감소시키는 작업을 여기에 추가

                // 예시: Firestore 문서를 참조하여 우산 수를 1 감소시킴
                DocumentReference umbrellaRef = FirebaseFirestore.getInstance().collection("우산").document("우산개수");
                umbrellaRef.update("개수", FieldValue.increment(-1))
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(MainActivity.this, "우산이 반납되었습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "우산 반납에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    // QRCodeScannerActivity 시작하는 메서드
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
