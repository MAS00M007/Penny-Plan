package com.z8ten.pennyplan;


import java.math.BigDecimal;
import java.util.List;

public class TransactionSummary {
    private List<Transaction> transactions;
    private BigDecimal totalSavings;
    private BigDecimal totalExpenses;

    // Constructor
    public TransactionSummary(List<Transaction> transactions, BigDecimal totalSavings, BigDecimal totalExpenses) {
        this.transactions = transactions;
        this.totalSavings = totalSavings;
        this.totalExpenses = totalExpenses;
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
