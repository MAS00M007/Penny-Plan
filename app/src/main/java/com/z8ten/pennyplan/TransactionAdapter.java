package com.z8ten.pennyplan;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactionList;
    private DatabaseHelper dbHelper;
    private Context context;

    public TransactionAdapter(Context context, List<Transaction> transactionList, DatabaseHelper dbHelper) {
        this.context = context;
        this.transactionList = transactionList;
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.amountTextView.setText("₹" + transaction.getAmount());
        holder.typeTextView.setText(transaction.getType());
        holder.noteTextView.setText(transaction.getNote());
        holder.dateTextView.setText(transaction.getDate());

        // Long Press Listener for Edit/Delete
        holder.itemView.setOnLongClickListener(v -> {
            showEditDeleteDialog(transaction, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView amountTextView, typeTextView, noteTextView, dateTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            amountTextView = itemView.findViewById(R.id.tvAmount);
            typeTextView = itemView.findViewById(R.id.tvType);
            noteTextView = itemView.findViewById(R.id.tvNote);
            dateTextView = itemView.findViewById(R.id.tvDate);
        }
    }

    // Show Edit/Delete Dialog
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

    // Show Edit Dialog
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

            // Update Transaction Object
            transaction.setAmount(newAmount);
            transaction.setNote(newNote);

            // Update in Database
            int result = dbHelper.updateTransaction(transaction);
            if (result > 0) {
                transactionList.set(position, transaction);
                notifyItemChanged(position);
                Toast.makeText(context, "Transaction updated!", Toast.LENGTH_SHORT).show();

                // ✅ UPDATE BALANCE AFTER EDITING
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
                    transactionList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Transaction deleted!", Toast.LENGTH_SHORT).show();

                    // ✅ UPDATE BALANCE AFTER DELETING
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).updateBalance();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}
