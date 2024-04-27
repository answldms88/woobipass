// 이 코드는 Java 파일의 패키지 이름을 지정합니다.
package com.example.woobipass_test;

// 여러 외부 클래스 및 메소드를 가져오는데 사용되는 import 문입니다.
import static android.webkit.URLUtil.isValidUrl;

// Android 개발에 필요한 클래스들을 import 합니다.
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;

// AndroidX 라이브러리의 클래스들을 import 합니다.
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// 바코드 스캔 라이브러리 관련 클래스를 import 합니다.
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.List;

// AppCompatActivity를 상속받는 QRCodeScannerActivity 클래스를 정의합니다.
public class QRCodeScannerActivity extends AppCompatActivity {
    // 카메라 권한 요청에 사용될 상수를 정의합니다.
    private static final int PERMISSION_REQUEST_CAMERA = 1;
    // CaptureManager 및 DecoratedBarcodeView 객체에 대한 참조를 저장할 변수를 선언합니다.
    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private void updateUmbrellaCount() {
        DocumentReference docRef = db.collection("우산").document("우산개수"); // your_document_id에 실제 문서 ID를 넣어야 합니다.
        db.runTransaction(transaction -> {
            Long currentCount = transaction.get(docRef).getLong("개수");
            if (currentCount == null) currentCount = 0L; // 초기 값이 설정되지 않았을 경우를 대비
            transaction.update(docRef, "개수", currentCount + 1);
            return null; // 이 트랜잭션은 반환값이 필요 없습니다.
        }).addOnSuccessListener(aVoid -> {
            Log.d("UpdateCount", "개수 successfully updated!");
        }).addOnFailureListener(e -> {
            Log.w("UpdateCount", "Error updating 개수", e);
        });
    }
    // 액티비티가 생성될 때 호출되는 메소드입니다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // DecoratedBarcodeView 인스턴스를 생성하고 액티비티의 뷰로 설정합니다.
        barcodeScannerView = new DecoratedBarcodeView(this);
        setContentView(barcodeScannerView);

        // 카메라 권한을 확인하고 요청합니다.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            // 권한이 이미 허용된 경우 스캔을 시작합니다.
            startScanning();
        }
    }

    // 스캐닝을 시작하는 메소드입니다.
    private void startScanning() {
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), null);
        barcodeScannerView.decodeSingle(new BarcodeCallback() {
            @Override
            // QRCodeScannerActivity에서의 barcodeResult 메소드 일부를 수정
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    Log.e("QRCodeScanner", "Scanned data: " + result.getText());
                    barcodeScannerView.setStatusText(result.getText());

                    if (isValidUrl(result.getText())) {
                        Toast.makeText(QRCodeScannerActivity.this, "Opening URL: " + result.getText(), Toast.LENGTH_LONG).show();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getText()));
                        startActivity(browserIntent);
                    } else {
                        Toast.makeText(QRCodeScannerActivity.this, "Scanned data is not a valid URL", Toast.LENGTH_LONG).show();
                    }
                    updateUmbrellaCount(); // 개수 업데이트 호출
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

    // 권한 요청 결과를 처리하는 메소드입니다.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되면 스캐닝을 시작합니다.
                startScanning();
            } else {
                // 권한이 거부되면 토스트 메시지를 보여주고 액티비티를 종료합니다.
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    // 액티비티가 다시 시작될 때 호출되는 메소드입니다.
    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    // 액티비티가 일시 정지될 때 호출되는 메소드입니다.
    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    // 액티비티가 종료될 때 호출되는 메소드입니다.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    // 액티비티의 상태를 저장할 때 호출되는 메소드입니다.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (capture != null) {
            capture.onSaveInstanceState(outState);
        }
    }
}
