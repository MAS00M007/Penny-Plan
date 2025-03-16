package com.z8ten.pennyplan;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private int selectedYear, selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_report);

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        btnSelectMonth = findViewById(R.id.btnSelectMonth);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        dbHelper = new DatabaseHelper(this);

        btnSelectMonth.setOnClickListener(v -> showMonthPickerDialog());

        btnGeneratePDF.setOnClickListener(v -> generatePDF());
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

        // Save PDF using MediaStore API
        savePDF(pdfDocument);
    }

    private void savePDF(PdfDocument pdfDocument) {
        String fileName = "Report_" + selectedMonth + "_" + selectedYear + ".pdf";
        OutputStream outputStream = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped Storage (Android 10+)
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PennyPlanReports");

            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri != null) {
                try {
                    outputStream = getContentResolver().openOutputStream(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } else {
            // Legacy storage (Android 9 and below)
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PennyPlanReports");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File(directory, fileName);
            try {
                outputStream = new FileOutputStream(file);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (outputStream != null) {
            try {
                pdfDocument.writeTo(outputStream);
                pdfDocument.close();
                outputStream.close();
                Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
