package com.z8ten.pennyplan;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DownloadReportActivity extends AppCompatActivity {
    private TextView tvSelectedMonth;
    private Button btnSelectMonth, btnGeneratePDF;
    private DatabaseHelper dbHelper;
    private int selectedYear = 0, selectedMonth = 0, selectedDay=0;
    private RewardedInterstitialAd rewardedInterstitialAd;
    private boolean isAdLoading = false;

    private static final String TAG = "DownloadReportActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_download_report);

        // Request storage permission ONLY for Android 9 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        dbHelper = new DatabaseHelper(this);

        MobileAds.initialize(this, initializationStatus -> {});
        loadRewardedInterstitialAd();
        updateGeneratePDFButton();

        btnSelectMonth.setOnClickListener(v -> showMonthPickerDialog());

        btnGeneratePDF.setOnClickListener(v -> {
            if (!isOnline()) {
                generatePDF();
                return;
            }

            if (rewardedInterstitialAd != null) {
                rewardedInterstitialAd.show(DownloadReportActivity.this, rewardItem -> {
                    generatePDF();
                    loadRewardedInterstitialAd(); // Reload after use
                });
            } else {
                Toast.makeText(this, "Ad not ready, generating PDF anyway", Toast.LENGTH_SHORT).show();
                generatePDF();
                loadRewardedInterstitialAd();
            }
        });
    }

    private void loadRewardedInterstitialAd() {
        if (isAdLoading) return; // Prevent multiple loads

        isAdLoading = true;
        updateGeneratePDFButton();

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(this, "ca-app-pub-3940256099942544/5354046379", // Use real Ad Unit ID in production
                adRequest, new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        rewardedInterstitialAd = ad;
                        isAdLoading = false;
                        updateGeneratePDFButton();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e("AdLoad", "Failed to load rewarded interstitial ad: " + loadAdError.getMessage());
                        rewardedInterstitialAd = null;
                        isAdLoading = false;
                        updateGeneratePDFButton();
                    }
                });
    }

    private void updateGeneratePDFButton() {
        if (!isOnline()) {
            btnGeneratePDF.setEnabled(true);
            btnGeneratePDF.setText("Generate PDF");
            return;
        }

        if (isAdLoading) {
            btnGeneratePDF.setEnabled(false);
            btnGeneratePDF.setText("Loading Ad…");
        } else if (rewardedInterstitialAd != null) {
            btnGeneratePDF.setEnabled(true);
            btnGeneratePDF.setText("Generate PDF (with Ad)");
        } else {
            btnGeneratePDF.setEnabled(true);
            btnGeneratePDF.setText("Generate PDF");
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void showMonthPickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, yearSelected, monthOfYear, dayOfMonth) -> {
                    selectedYear = yearSelected;
                    selectedMonth = monthOfYear + 1;
                    selectedDay = dayOfMonth;
                    tvSelectedMonth.setText("Selected Month: " + selectedMonth + "/" + selectedYear);
                }, year, month, 1);

        datePickerDialog.show();
    }

    private void generatePDF() {

        List<Transaction> transactions = dbHelper.getTransactionsForMonth(selectedMonth, selectedYear);

        if (transactions.isEmpty()) {
            Toast.makeText(this, "No transactions found for this month", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create summary object to get total savings and expenses
        TransactionSummary summary = new TransactionSummary(transactions);
        BigDecimal totalSavings = summary.getTotalSavings();
        BigDecimal totalExpenses = summary.getTotalExpenses();

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        int pageWidth = 600;
        int pageHeight = 1000;
        int yPosition = 100;
        int rowHeight = 40;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // DEBUG: Log summary
//        Log.d("DEBUG_SUMMARY", summary.toString());

        try {
            // Title
            // Title
            paint.setTextSize(18);
            paint.setFakeBoldText(true);
            canvas.drawText("Transaction Report - " + selectedMonth + "/" + selectedYear, 120, yPosition, paint);
            yPosition += 50;

// Column Headers
            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            canvas.drawText("Date", 50, yPosition, paint);
            canvas.drawText("Category", 150, yPosition, paint);
            canvas.drawText("Note", 300, yPosition, paint);
            canvas.drawText("Amount", 500, yPosition, paint); // Amount moved to last
            yPosition += rowHeight;
            canvas.drawLine(50, yPosition, 550, yPosition, paint);
            yPosition += rowHeight;
            paint.setFakeBoldText(false);

// Transactions
            for (Transaction transaction : transactions) {
                canvas.drawText(transaction.getDate(), 50, yPosition, paint);
                canvas.drawText(transaction.getType(), 150, yPosition, paint);
                canvas.drawText(transaction.getNote(), 300, yPosition, paint);
                canvas.drawText("₹" + transaction.getAmount(), 500, yPosition, paint); // Amount at the end
                yPosition += rowHeight;

                // Page break check
                if (yPosition > pageHeight - 100) {
                    pdfDocument.finishPage(page);
                    int pageNumber=1;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = 100;
                }
            }




            // Draw a line before totals
            canvas.drawLine(50, yPosition, 550, yPosition, paint);
            yPosition += rowHeight;

            // Totals
            paint.setFakeBoldText(true);
            canvas.drawText("Total Savings:", 50, yPosition, paint);
            canvas.drawText("₹" + totalSavings, 500, yPosition, paint);
            yPosition += rowHeight;

            canvas.drawText("Total Expenses:", 50, yPosition, paint);
            canvas.drawText("₹" + totalExpenses, 500, yPosition, paint);

            pdfDocument.finishPage(page);
            savePDF(pdfDocument);

        } catch (Exception e) {
//            Log.e("PDF_ERROR", "Error generating PDF: " + e.getMessage());
        }
    }



    private void savePDF(PdfDocument pdfDocument) {
        String fileName = "Report_"+ selectedDay + "_" + selectedMonth + "_" + selectedYear + ".pdf";
        OutputStream outputStream = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {  // Android 10+
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PennyPlan");

                Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri == null) {
//                    Log.e(TAG, "Failed to create URI for saving PDF.");
                    Toast.makeText(this, "Error saving PDF!", Toast.LENGTH_LONG).show();
                    return;
                }

                outputStream = getContentResolver().openOutputStream(uri);
//                Log.d(TAG, "PDF saved to: " + uri.toString());

            } else {  // Android 9 and below
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PennyPlan");

                if (!directory.exists()) {
                    boolean dirCreated = directory.mkdirs();
//                    Log.d(TAG, "Directory created: " + dirCreated);
                    if (!dirCreated) {
//                        Log.e(TAG, "Failed to create directory: " + directory.getAbsolutePath());
                        Toast.makeText(this, "Error creating directory!", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                File file = new File(directory, fileName);
                outputStream = new FileOutputStream(file);
//                Log.d(TAG, "PDF saved to: " + file.getAbsolutePath());

                // Ensure file is visible in the File Manager
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }

            if (outputStream != null) {
                pdfDocument.writeTo(outputStream);
                pdfDocument.close();
                outputStream.flush();
                outputStream.close();
                Toast.makeText(this, "PDF saved successfully!" + fileName, Toast.LENGTH_LONG).show();
            } else {
//                Log.e(TAG, "Output stream is null, PDF not saved.");
            }

        } catch (IOException e) {
//            Log.e(TAG, "Error saving PDF: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
