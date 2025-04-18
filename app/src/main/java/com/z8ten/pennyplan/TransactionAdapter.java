package com.z8ten.pennyplan;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_TRANSACTION = 0;
    private static final int VIEW_TYPE_AD = 1;
    private final List<Object> items;  // Holds both transactions & ads
    private final Context context;
    private final DatabaseHelper dbHelper;

    public TransactionAdapter(Context context, List<Object> items, DatabaseHelper dbHelper) {
        this.context = context;
        this.items = items;
        this.dbHelper = dbHelper;
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof Transaction) ? VIEW_TYPE_TRANSACTION : VIEW_TYPE_AD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_TRANSACTION) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_native_ad, parent, false);
            return new AdViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_TRANSACTION) {
            TransactionViewHolder transactionHolder = (TransactionViewHolder) holder;
            Transaction transaction = (Transaction) items.get(position);

            BigDecimal amount = transaction.getAmount();
            String formattedAmount = new DecimalFormat("#,##0.00").format(amount);

            transactionHolder.amountTextView.setText("" + formattedAmount);
            transactionHolder.typeTextView.setText(transaction.getType());
            transactionHolder.noteTextView.setText(transaction.getNote());
            transactionHolder.dateTextView.setText(transaction.getDate());
            transactionHolder.timeTextView.setText(transaction.getTime()); // Display time

            // Color based on transaction type
            if ("Saving".equalsIgnoreCase(transaction.getType())) {
                transactionHolder.amountTextView.setTextColor(context.getResources().getColor(R.color.accent_color));
            } else if ("Expense".equalsIgnoreCase(transaction.getType())) {
                transactionHolder.amountTextView.setTextColor(context.getResources().getColor(R.color.expense_color));
            } else {
                transactionHolder.amountTextView.setTextColor(Color.BLACK);
            }

            // Long press to Edit/Delete
            transactionHolder.itemView.setOnLongClickListener(v -> {
                showEditDeleteDialog(transaction, position);
                return true;
            });

        } else {
            AdViewHolder adHolder = (AdViewHolder) holder;
            NativeAd nativeAd = (NativeAd) items.get(position);
            populateNativeAdView(nativeAd, adHolder.adView);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView amountTextView, typeTextView, noteTextView, dateTextView, timeTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            amountTextView = itemView.findViewById(R.id.tvAmount);
            typeTextView = itemView.findViewById(R.id.tvType);
            noteTextView = itemView.findViewById(R.id.tvNote);
            dateTextView = itemView.findViewById(R.id.tvDate);
            timeTextView = itemView.findViewById(R.id.tvTime); // Added time display
        }
    }

    public static class AdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView adView;

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = itemView.findViewById(R.id.native_ad_view);
        }
    }

    // Edit/Delete Dialog
    private void showEditDeleteDialog(Transaction transaction, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Action")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(transaction, position);
                    } else {
                        deleteTransaction(transaction, position);
                    }
                })
                .show();
    }

    // Edit Transaction
    private void showEditDialog(Transaction transaction, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_transaction, null);
        builder.setView(view);

        EditText etAmount = view.findViewById(R.id.etEditAmount);
        EditText etNote = view.findViewById(R.id.etEditNote);
        etAmount.setText(String.valueOf(transaction.getAmount()));
        etNote.setText(transaction.getNote());

        builder.setPositiveButton("Update", (dialog, which) -> {
            double newAmount = Double.parseDouble(etAmount.getText().toString());
            String newNote = etNote.getText().toString();

            transaction.setAmount(BigDecimal.valueOf(newAmount));
            transaction.setNote(newNote);

            int result = dbHelper.updateTransaction(transaction);
            if (result > 0) {
                items.set(position, transaction);
                notifyItemChanged(position);
                Toast.makeText(context, "Transaction updated!", Toast.LENGTH_SHORT).show();

                if (context instanceof MainActivity) {
                    ((MainActivity) context).updateBalance();
                }
            } else {
                Toast.makeText(context, "Update failed!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Delete Transaction
    private void deleteTransaction(Transaction transaction, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dbHelper.deleteTransaction(transaction.getId());
                    items.remove(position);
                    notifyItemRemoved(position);

                    if (items.isEmpty()) {
                        notifyDataSetChanged(); // Ensure RecyclerView updates if empty
                        Toast.makeText(context, "No transactions available", Toast.LENGTH_SHORT).show();
                    } else {
                        notifyItemRangeChanged(position, items.size()); // Update remaining items
                    }

                    if (context instanceof MainActivity) {
                        ((MainActivity) context).updateBalance();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Load Native Ad
    private void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        TextView headlineView = adView.findViewById(R.id.ad_headline);
        headlineView.setText(nativeAd.getHeadline());
        adView.setHeadlineView(headlineView);

        TextView bodyView = adView.findViewById(R.id.ad_body);
        if (nativeAd.getBody() != null) {
            bodyView.setText(nativeAd.getBody());
            adView.setBodyView(bodyView);
        } else {
            bodyView.setVisibility(View.GONE);
        }

        Button ctaButton = adView.findViewById(R.id.ad_call_to_action);
        if (nativeAd.getCallToAction() != null) {
            ctaButton.setText(nativeAd.getCallToAction());
            adView.setCallToActionView(ctaButton);
        } else {
            ctaButton.setVisibility(View.GONE);
        }

        adView.setNativeAd(nativeAd);
    }
}
