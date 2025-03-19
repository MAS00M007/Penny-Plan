package com.z8ten.pennyplan;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

public class DownloadReportActivity extends AppCompatActivity {
    private TextView tvSelectedMonth;
    private Button btnSelectMonth, btnGeneratePDF;
    private DatabaseHelper dbHelper;
    private int selectedYear = 0, selectedMonth = 0;
    private RewardedInterstitialAd rewardedInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_download_report);

        // Initialize views
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        dbHelper = new DatabaseHelper(this);

        // Initialize Ads
        MobileAds.initialize(this, initializationStatus -> {});

        // Load Ad Immediately
        loadRewardedInterstitialAd();

        btnSelectMonth.setOnClickListener(v -> showMonthPickerDialog());

        btnGeneratePDF.setOnClickListener(v -> {
            if (!isOnline()) {
                generatePDF();
                return;
            }

            if (rewardedInterstitialAd != null) {
                rewardedInterstitialAd.show(DownloadReportActivity.this, rewardItem -> {
                    generatePDF();
                    loadRewardedInterstitialAd(); // Reload for next use
                });
            } else {
                Toast.makeText(this, "Ad not ready, generating PDF anyway", Toast.LENGTH_SHORT).show();
                generatePDF();
                loadRewardedInterstitialAd(); // Try to load again for next use
            }
        });
    }

    // Load Rewarded Interstitial Ad
    private void loadRewardedInterstitialAd() {
        if (!isOnline()) {
            btnGeneratePDF.setEnabled(true);
            return;
        }

        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedInterstitialAd.load(this, "ca-app-pub-3940256099942544/5354046379", adRequest,
                    new RewardedInterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                            rewardedInterstitialAd = ad;
                            btnGeneratePDF.setEnabled(true);
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                            rewardedInterstitialAd = null;
                            btnGeneratePDF.setEnabled(true);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Check if device is online
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    private void showMonthPickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, yearSelected, monthOfYear, dayOfMonth) -> {
            selectedYear = yearSelected;
            selectedMonth = monthOfYear + 1;
            tvSelectedMonth.setText("Selected Month: " + selectedMonth + "/" + selectedYear);
        }, year, month, 1);
        datePickerDialog.show();
    }

    private void generatePDF() {
        if (selectedYear <= 0 || selectedMonth <= 0) {
            Toast.makeText(this, "Please select a month first", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Transaction> transactions = dbHelper.getTransactionsForMonth(selectedMonth, selectedYear);
        if (transactions.isEmpty()) {
            Toast.makeText(this, "No transactions found for the selected month", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(2);

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(600, 800, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int xStart = 50;
        int yStart = 100;
        int rowHeight = 40;
        int colWidth = 120;

        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText("Transaction Report - " + selectedMonth + "/" + selectedYear, 150, 50, paint);

        paint.setTextSize(14);
        int y = yStart;

        canvas.drawText("Date", xStart, y, paint);
        canvas.drawText("Type", xStart + colWidth, y, paint);
        canvas.drawText("Amount", xStart + 2 * colWidth, y, paint);
        canvas.drawText("Note", xStart + 3 * colWidth, y, paint);

        y += rowHeight;
        canvas.drawLine(xStart, y, xStart + 4 * colWidth, y, linePaint);

        paint.setTextSize(12);
        for (Transaction transaction : transactions) {
            y += rowHeight;
            if (y > 750) break;

            canvas.drawText(transaction.getDate(), xStart, y, paint);
            canvas.drawText(transaction.getType(), xStart + colWidth, y, paint);
            canvas.drawText("₹" + transaction.getAmount(), xStart + 2 * colWidth, y, paint);
            canvas.drawText(transaction.getNote(), xStart + 3 * colWidth, y, paint);

            canvas.drawLine(xStart, y + 10, xStart + 4 * colWidth, y + 10, linePaint);
        }

        pdfDocument.finishPage(page);
        savePDF(pdfDocument);
    }

    private void savePDF(PdfDocument pdfDocument) {
        String fileName = "Report_" + selectedMonth + "_" + selectedYear + ".pdf";
        OutputStream outputStream = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri != null) {
                try {
                    outputStream = getContentResolver().openOutputStream(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        try {
            pdfDocument.writeTo(outputStream);
            pdfDocument.close();
            outputStream.close();
            Toast.makeText(this, "PDF saved successfully to Documents", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
