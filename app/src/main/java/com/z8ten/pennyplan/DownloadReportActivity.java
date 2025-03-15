package com.z8ten.pennyplan;

import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import android.Manifest;

public class DownloadReportActivity extends AppCompatActivity {
    private TextView tvSelectedMonth;
    private Button btnSelectMonth, btnGeneratePDF;
    private DatabaseHelper dbHelper;
    private int selectedYear, selectedMonth;
    private static final int STORAGE_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_report);

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        dbHelper = new DatabaseHelper(this);

        btnSelectMonth.setOnClickListener(v -> showMonthPickerDialog());


        btnGeneratePDF.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                generatePDF();
            } else {
                requestStoragePermission();
            }
        });
    }

    private void showMonthPickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, yearSelected, monthOfYear, dayOfMonth) -> {
            selectedYear = yearSelected;
            selectedMonth = monthOfYear + 1; // Months are 0-based
            tvSelectedMonth.setText("Selected Month: " + selectedMonth + "/" + selectedYear);
        }, year, month, 1);
        datePickerDialog.show();
    }

    private void generatePDF() {
        if (selectedYear == 0 || selectedMonth == 0) {
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

        // Title
        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        canvas.drawText("Transaction Report - " + selectedMonth + "/" + selectedYear, 150, 50, paint);

        // Table Header
        paint.setTextSize(14);
        int y = yStart;

        canvas.drawText("Date", xStart, y, paint);
        canvas.drawText("Type", xStart + colWidth, y, paint);
        canvas.drawText("Amount", xStart + 2 * colWidth, y, paint);
        canvas.drawText("Note", xStart + 3 * colWidth, y, paint);

        y += rowHeight;
        canvas.drawLine(xStart, y, xStart + 4 * colWidth, y, linePaint); // Header separator

        // Table Rows
        paint.setTextSize(12);
        for (Transaction transaction : transactions) {
            y += rowHeight;
            if (y > 750) break; // Prevents text from overflowing the page

            canvas.drawText(transaction.getDate(), xStart, y, paint);
            canvas.drawText(transaction.getType(), xStart + colWidth, y, paint);
            canvas.drawText("₹" + transaction.getAmount(), xStart + 2 * colWidth, y, paint);
            canvas.drawText(transaction.getNote(), xStart + 3 * colWidth, y, paint);

            canvas.drawLine(xStart, y + 10, xStart + 4 * colWidth, y + 10, linePaint); // Row separator
        }

        pdfDocument.finishPage(page);

        // Save PDF
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PennyPlanReports");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, "Report_" + selectedMonth + "_" + selectedYear + ".pdf");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();
            Toast.makeText(this, "PDF saved to Documents/PennyPlanReports", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
        }
    }


    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Request permission
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Storage permission is required to save the PDF.", Toast.LENGTH_LONG).show();
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    // Handle the permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePDF();
            } else {
                Toast.makeText(this, "Storage permission denied! Cannot save PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    }