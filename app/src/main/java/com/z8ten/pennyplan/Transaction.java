package com.z8ten.pennyplan;

import java.math.BigDecimal;

public class Transaction {
    private int id;
    private BigDecimal amount;
    private String type; // "Income" or "Expense"
    private String note;
    private String date;
    private String time; // Added time field

    // Constructor with ID
    public Transaction(int id, BigDecimal amount, String type, String note, String date, String time) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.note = note;
        this.date = date;
        this.time = time;
    }

    // Constructor without ID (for new transactions before inserting into DB)
    public Transaction(BigDecimal amount, String type, String note, String date, String time) {
        this.amount = amount;
        this.type = type;
        this.note = note;
        this.date = date;
        this.time = time;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount != null ? amount : BigDecimal.ZERO; // Prevent null issues
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type.equalsIgnoreCase("Income") || type.equalsIgnoreCase("Expense")) {
            this.type = type;
        } else {
            throw new IllegalArgumentException("Invalid transaction type. Use 'Income' or 'Expense'.");
        }
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    // ToString Method for Debugging
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                ", note='" + note + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
