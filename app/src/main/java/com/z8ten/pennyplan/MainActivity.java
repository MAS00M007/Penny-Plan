package com.z8ten.pennyplan;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<Object> items = new ArrayList<>();
    private Button btnAddSaving, btnAddExpense;
    private TextView tvBalance;
    private ImageView downloadActivity;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        // Initialize Database
        dbHelper = new DatabaseHelper(this);

        // Initialize Views
        recyclerView = findViewById(R.id.rvTransactions);
        btnAddSaving = findViewById(R.id.btnAddSaving);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        tvBalance = findViewById(R.id.tvBalance);
        downloadActivity = findViewById(R.id.downloadActivityButton);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, items, dbHelper);
        recyclerView.setAdapter(adapter);

        // Initialize Mobile Ads
        MobileAds.initialize(this, initializationStatus -> {
            // Load data after ads initialization
            loadDataInBackground();
        });

        // Setup Click Listeners
        downloadActivity.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, DownloadReportActivity.class))
        );

        btnAddSaving.setOnClickListener(v -> showTransactionDialog("Saving"));
        btnAddExpense.setOnClickListener(v -> showTransactionDialog("Expense"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the activity
        loadDataInBackground();
    }

    // Load transactions from DB in background thread
    private void loadDataInBackground() {
        // Show loading indicator if needed

        executor.execute(() -> {
            // Get transactions in background
            final List<Transaction> transactions = dbHelper.getAllTransactions();
            final double balance = dbHelper.getBalance();

            // Update UI on main thread
            runOnUiThread(() -> {
                // Update balance
                tvBalance.setText("Balance:" + String.format("%,.2f", balance));

                // Update transaction list
                items.clear();
                items.addAll(transactions);
                adapter.notifyDataSetChanged();

                // Load ad after showing content for better user experience
                loadNativeAd();
            });
        });
    }

    private void loadNativeAd() {
        AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
                .forNativeAd(nativeAd -> {
                    // Remove any existing ads first
                    for (int i = items.size() - 1; i >= 0; i--) {
                        if (!(items.get(i) instanceof Transaction)) {
                            items.remove(i);
                        }
                    }

                    // Add the new ad
                    items.add(nativeAd);
                    adapter.notifyDataSetChanged();
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e("AdLoadError", "Failed to load native ad: " + adError.getMessage());
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    // Show Dialog Box for Adding Transaction
    private void showTransactionDialog(String type) {
        Dialog dialog = new Dialog(this);

        if (type.equals("Expense")) {
            dialog.setContentView(R.layout.dialog_add_expense);
        } else {
            dialog.setContentView(R.layout.dialog_add_saving);
        }

        // Set dialog width to match_parent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etAmount = dialog.findViewById(type.equals("Expense") ? R.id.etExpenseAmount : R.id.etSavingAmount);
        EditText etNote = dialog.findViewById(type.equals("Expense") ? R.id.etExpenseNote : R.id.etSavingNote);
        Button btnSave = dialog.findViewById(type.equals("Expense") ? R.id.btnSaveExpense : R.id.btnSaveSaving);

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }

            BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(amountStr));
            String date = java.text.DateFormat.getDateInstance().format(new java.util.Date());

            // Save transaction in background
            executor.execute(() -> {
                // Save to database
                final long result = dbHelper.insertTransaction(new Transaction(0, amount, type, note, date));

                // Update UI on main thread
                runOnUiThread(() -> {
                    // Show Toast message
                    if (result != -1) {
                        Toast.makeText(MainActivity.this, type + " added successfully!", Toast.LENGTH_SHORT).show();
                        // Refresh data
                        loadDataInBackground();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to add " + type, Toast.LENGTH_SHORT).show();
                    }
                });
            });

            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
    public void updateBalance() {
        double balance = dbHelper.getBalance();
        TextView tvBalance = findViewById(R.id.tvBalance);

        runOnUiThread(() -> tvBalance.setText("Balance:" + String.format("%,.2f", balance)));
    }


}