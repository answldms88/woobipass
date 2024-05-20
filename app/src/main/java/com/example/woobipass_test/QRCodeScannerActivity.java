package com.example.woobipass_test;

import static android.webkit.URLUtil.isValidUrl;

import android.Manifest;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        barcodeScannerView = new DecoratedBarcodeView(this);
        setContentView(barcodeScannerView);

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
                    handleUmbrellaRental(result.getText());
                } else {
                    Toast.makeText(QRCodeScannerActivity.this, "No data found in QR Code.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void possibleResultPoints(List resultPoints) {
                // 스캔 시 발견 가능한 결과 포인트들을 처리하는 메소드입니다.
            }
        });
    }

    private void handleUmbrellaRental(String umbrellaName) {
        db.collection("umbrellas").whereEqualTo("우산명", umbrellaName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    DocumentReference docRef = document.getReference();
                                    String currentStatus = document.getString("대여상태");
                                    String newStatus = "X".equals(currentStatus) ? "O" : "X"; // Toggle status
                                    String timestampField = "X".equals(currentStatus) ? "대여시간" : "반납시간";

                                    String currentTime = getCurrentTimeFormatted();
                                    docRef.update("대여상태", newStatus, timestampField, currentTime)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    String message = "X".equals(currentStatus) ? "대여되었습니다" : "반납되었습니다";
                                                    Toast.makeText(QRCodeScannerActivity.this, message, Toast.LENGTH_LONG).show();
                                                    finish(); // Finish the activity to close the scanning window
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(QRCodeScannerActivity.this, "Error updating umbrella rental status.", Toast.LENGTH_LONG).show();
                                                }
                                            });
                                }
                            } else {
                                Toast.makeText(QRCodeScannerActivity.this, "No matching umbrella found.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(QRCodeScannerActivity.this, "Error getting umbrella information.", Toast.LENGTH_LONG).show();
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
