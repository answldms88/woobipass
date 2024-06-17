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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.List;
import java.util.Locale;

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
                                    // 현재 사용자가 대여 중인 경우 반납 처리
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
        String currentTime = getCurrentTimeFormatted();

        docRef.update("대여상태", "O", "대여시간", currentTime, "user_id", userId)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(QRCodeScannerActivity.this, "우산을 대여했습니다.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(QRCodeScannerActivity.this, "Error renting umbrella.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void returnUmbrella(DocumentReference docRef) {
        String currentTime = getCurrentTimeFormatted();

        docRef.update("대여상태", "X", "반납시간", currentTime, "user_id", "X")
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.remove("user_id");
                        editor.apply();
                        Toast.makeText(QRCodeScannerActivity.this, "우산을 반납했습니다.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(QRCodeScannerActivity.this, "Error returning umbrella.", Toast.LENGTH_LONG).show();
                        finish();
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
