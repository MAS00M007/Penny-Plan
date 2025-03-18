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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<Transaction> transactionList;
    private Button btnAddSaving, btnAddExpense;
    List<Object> items = new ArrayList<>();



    public ImageView downloadActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Initialize Database
        dbHelper = new DatabaseHelper(this);

        // Setup RecyclerView
        recyclerView = findViewById(R.id.rvTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch Transactions and Set Adapter
        MobileAds.initialize(this, initializationStatus -> {});

        loadTransactions();
updateBalance();

        // Buttons
        btnAddSaving = findViewById(R.id.btnAddSaving);
        btnAddExpense = findViewById(R.id.btnAddExpense);

        downloadActivity=findViewById(R.id.downloadActivityButton);

        downloadActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
startActivity(new Intent(MainActivity.this, DownloadReportActivity.class));
            }
        });
        // Add Saving Button Click
        btnAddSaving.setOnClickListener(v -> showTransactionDialog("Saving"));

        // Add Expense Button Click
        btnAddExpense.setOnClickListener(v -> showTransactionDialog("Expense"));
    }

    // Load transactions from DB and update RecyclerView
    private void loadTransactions() {
        List<Object> items = new ArrayList<>();
        List<Transaction> transactions = dbHelper.getAllTransactions();

        for (int i = 0; i < transactions.size(); i++) {
            items.add(transactions.get(i));

            // Insert an Ad every 5th transaction
            if ((i + 1) % 5 == 0) {
                loadNativeAd(items);
            }
        }

        adapter = new TransactionAdapter(this, items, dbHelper);
        recyclerView.setAdapter(adapter);
    }

    private void loadNativeAd(List<Object> items) {
        AdLoader adLoader = new AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
                .forNativeAd(nativeAd -> {
                    items.add(nativeAd);
                    if (adapter != null) {
                        adapter.notifyItemInserted(items.size() - 1);
                    }
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

            // Save to database
            long result = dbHelper.insertTransaction(new Transaction(0, amount, type, note, date));

            // Show Toast message
            if (result != -1) {
                Toast.makeText(this, type + " added successfully!", Toast.LENGTH_SHORT).show();
                updateBalance();
            } else {
                Toast.makeText(this, "Failed to add " + type, Toast.LENGTH_SHORT).show();
            }

            // Refresh RecyclerView
            loadTransactions();

            dialog.dismiss();
        });

        dialog.show();
    }


    public void updateBalance() {
        double balance = dbHelper.getBalance();
        TextView tvBalance = findViewById(R.id.tvBalance);

        runOnUiThread(() -> tvBalance.setText("Balance:" + String.format("%,.2f", balance)));
    }



}
