package com.example.woobipass_test;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QRCodeScannerActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 1;
    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        barcodeScannerView = new DecoratedBarcodeView(this);
        setContentView(barcodeScannerView);

        preferences = getSharedPreferences("user_data", MODE_PRIVATE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), null);
        barcodeScannerView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    Log.e("QRCodeScanner", "Scanned data: " + result.getText());
                    barcodeScannerView.setStatusText(result.getText());
                    handleUmbrellaAction(result.getText());
                } else {
                    Toast.makeText(QRCodeScannerActivity.this, "No data found in QR Code.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void possibleResultPoints(List resultPoints) {
                // Handle possible result points during scan
            }
        });
    }

    private void handleUmbrellaAction(String umbrellaName) {
        String userId = preferences.getString("user_id", "");

        db.collection("umbrellas")
                .whereEqualTo("우산명", umbrellaName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String currentStatus = document.getString("대여상태");
                                String currentUser = document.getString("user_id");

                                if ("X".equals(currentStatus)) {
                                    // 우산이 반납 상태일 때 대여 처리
                                    rentUmbrella(document.getReference(), userId);
                                } else if ("O".equals(currentStatus) && userId.equals(currentUser)) {
                                    // 현재 사용자가 대여 중인 경우
                                    returnUmbrella(document.getReference());
                                } else {
                                    // 우산이 이미 대여 중인 경우
                                    Toast.makeText(QRCodeScannerActivity.this, "이미 대여 중인 우산입니다.", Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            }
                        } else {
                            Toast.makeText(QRCodeScannerActivity.this, "Error getting umbrella information.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }

    private void rentUmbrella(DocumentReference docRef, String userId) {
        // 사용자가 이미 대여 중인 우산이 있는지 확인
        db.collection("umbrellas")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("대여상태", "O")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // 이미 대여 중인 우산이 있는 경우
                                Toast.makeText(QRCodeScannerActivity.this, "이미 우산을 대여 중입니다. 반납 후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                // 대여 가능한 경우 우산 대여 처리
                                String currentTime = getCurrentTimeFormatted();

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("대여상태", "O");
                                updates.put("대여시간", currentTime);
                                updates.put("user_id", userId);

                                docRef.update(updates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // 대여 성공 시 Toast 메시지 표시
                                                    Toast.makeText(QRCodeScannerActivity.this, "우산을 대여했습니다. 사용자 ID: " + userId, Toast.LENGTH_LONG).show();

                                                    // SharedPreferences에 사용자 아이디 저장
                                                    SharedPreferences.Editor editor = preferences.edit();
                                                    editor.putString("user_id", userId);
                                                    editor.apply();  // 변경 사항을 저장

                                                    // 대여 완료 후 액티비티 종료
                                                    finish();
                                                } else {
                                                    // 대여 실패 시 Toast 메시지 표시
                                                    Toast.makeText(QRCodeScannerActivity.this, "우산 대여 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();

                                                    // 오류 발생 후 액티비티 종료
                                                    finish();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(QRCodeScannerActivity.this, "Error checking umbrella rental status.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
    }

    private void returnUmbrella(DocumentReference docRef) {
        String currentTime = getCurrentTimeFormatted();

        Map<String, Object> updates = new HashMap<>();
        updates.put("대여상태", "X");
        updates.put("반납시간", currentTime);

        docRef.update(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // 반납 성공 시 Toast 메시지 표시
                            Toast.makeText(QRCodeScannerActivity.this, "우산을 반납했습니다.", Toast.LENGTH_LONG).show();

                            // 반납 완료 후 액티비티 종료
                            finish();
                        } else {
                            // 반납 실패 시 Toast 메시지 표시
                            Toast.makeText(QRCodeScannerActivity.this, "우산 반납 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();

                            // 오류 발생 후 액티비티 종료
                            finish();
                        }
                    }
                });
    }

    private String getCurrentTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) {
            capture.onSaveInstanceState(outState);
        }
    }
}
