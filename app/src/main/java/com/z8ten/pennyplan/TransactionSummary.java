package com.z8ten.pennyplan;

import android.util.Log;

import java.math.BigDecimal;
import java.util.List;

public class TransactionSummary {
    private List<Transaction> transactions;
    private BigDecimal totalSavings;
    private BigDecimal totalExpenses;

    // Constructor - Automatically calculates totals
    public TransactionSummary(List<Transaction> transactions) {
        this.transactions = transactions;
        this.totalSavings = BigDecimal.ZERO; // Initialize properly
        this.totalExpenses = BigDecimal.ZERO;
        calculateTotals();
    }


    private void calculateTotals() {
        for (Transaction transaction : transactions) {
            String type = transaction.getType().trim(); // Trim any whitespace
            BigDecimal amount = transaction.getAmount();

            // Log each transaction
            Log.d("DEBUG_TRANSACTION", "Type: " + type + ", Amount: " + amount);

            if (type.equalsIgnoreCase("saving")) {
                totalSavings = totalSavings.add(amount);
            } else if (type.equalsIgnoreCase("expense")) {
                totalExpenses = totalExpenses.add(amount);
            }
        }

        // Log final totals
//        Log.d("DEBUG_SUMMARY", "Total Savings: " + totalSavings + ", Total Expenses: " + totalExpenses);
    }


    // Getters
    public List<Transaction> getTransactions() {
        return transactions;
    }

    public BigDecimal getTotalSavings() {
        return totalSavings;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    // ToString Method for Debugging
    @Override
    public String toString() {
        return "TransactionSummary{" +
                "totalSavings=" + totalSavings +
                ", totalExpenses=" + totalExpenses +
                ", transactions=" + transactions +
                '}';
    }
}
