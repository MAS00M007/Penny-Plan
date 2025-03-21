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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private static final String TAG = "MainActivity";
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"; // Test Ad Unit ID

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.rvTransactions);
        btnAddSaving = findViewById(R.id.btnAddSaving);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        tvBalance = findViewById(R.id.tvBalance);
        downloadActivity = findViewById(R.id.downloadActivityButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, items, dbHelper);
        recyclerView.setAdapter(adapter);

        //
        dbHelper.debugDatabase();


        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Ads initialized");
            loadDataInBackground();
            loadNativeAd();
        });

        downloadActivity.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, DownloadReportActivity.class))
        );

        btnAddSaving.setOnClickListener(v -> showTransactionDialog("Saving"));
        btnAddExpense.setOnClickListener(v -> showTransactionDialog("Expense"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDataInBackground();
    }

    private void loadDataInBackground() {
        executor.execute(() -> {
            List<Transaction> transactions = dbHelper.getAllTransactions();
            double balance = dbHelper.getBalance();

            runOnUiThread(() -> {
                tvBalance.setText("Balance: " + String.format("%,.2f", balance));
                items.clear();
                items.addAll(transactions);
                if (items.isEmpty()) {
                    Toast.makeText(this, "No transactions available", Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
                loadNativeAd();
            });
        });
    }

    private void loadNativeAd() {
        AdLoader adLoader = new AdLoader.Builder(this, AD_UNIT_ID)
                .forNativeAd(nativeAd -> {
                    for (int i = items.size() - 1; i >= 0; i--) {
                        if (!(items.get(i) instanceof Transaction)) {
                            items.remove(i);
                        }
                    }
                    items.add(nativeAd);
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Native ad loaded successfully");
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        Log.e(TAG, "Ad failed to load: " + adError.getMessage());
                        Toast.makeText(MainActivity.this, "Ad failed: " + adError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void showTransactionDialog(String type) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(type.equals("Expense") ? R.layout.dialog_add_expense : R.layout.dialog_add_saving);

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

            BigDecimal amount = new BigDecimal(amountStr);
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            executor.execute(() -> {
                long result = dbHelper.insertTransaction(new Transaction(0, amount, type, note, date, time));

                runOnUiThread(() -> {
                    if (result != -1) {
                        Toast.makeText(MainActivity.this, type + " added successfully!", Toast.LENGTH_SHORT).show();
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
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    public void updateBalance() {
        double balance = dbHelper.getBalance();
        runOnUiThread(() -> tvBalance.setText("Balance: " + String.format("%,.2f", balance)));
    }
}
