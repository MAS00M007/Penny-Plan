package com.z8ten.pennyplan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PennyPlan.db";
    private static final int DATABASE_VERSION = 3;

    // Table and Columns
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_TIME = "time"; // New column for time

    // SQL Query to Create Table
    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_AMOUNT + " TEXT, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_NOTE + " TEXT, " +
                    COLUMN_DATE + " TEXT, " +
                    COLUMN_TIME + " TEXT)"; // Added time column

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) { // Upgrading from version 2 to 3
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COLUMN_TIME + " TEXT DEFAULT ''");
        }
    }

    // Insert a Transaction
    public long insertTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_AMOUNT, transaction.getAmount().toPlainString());
            values.put(COLUMN_TYPE, transaction.getType());
            values.put(COLUMN_NOTE, transaction.getNote());
            values.put(COLUMN_DATE, transaction.getDate());
            values.put(COLUMN_TIME, transaction.getTime());

            id = db.insert(TABLE_TRANSACTIONS, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return id;
    }

    // Get All Transactions
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY id DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    Transaction transaction = new Transaction(
                            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT))),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME))
                    );
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return transactions;
    }

    // Update a Transaction
    public int updateTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_AMOUNT, transaction.getAmount().toPlainString());
            values.put(COLUMN_TYPE, transaction.getType());
            values.put(COLUMN_NOTE, transaction.getNote());
            values.put(COLUMN_DATE, transaction.getDate());
            values.put(COLUMN_TIME, transaction.getTime());

            rowsAffected = db.update(TABLE_TRANSACTIONS, values, COLUMN_ID + "=?", new String[]{String.valueOf(transaction.getId())});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return rowsAffected;
    }

    // Delete a Transaction
    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.delete(TABLE_TRANSACTIONS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public double getBalance() {
        double balance = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM transactions", null);
        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(0);
        }
        cursor.close();
        return balance;
    }

    public List<Transaction> getTransactionsForAllMonths() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Order transactions by date (YYYY-MM-DD) and time (HH:MM:SS)
        String query = "SELECT * FROM transactions ORDER BY date DESC, time DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));  // Get time column
                    String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                    BigDecimal amount = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow("amount")));
                    String note = cursor.getString(cursor.getColumnIndexOrThrow("note"));

                    transactions.add(new Transaction(id,amount,type,note,date,time));
                    Log.d("DB_FETCH", "Transaction: " + date + " " + time + " | " + type + " | ₹" + amount);
                } catch (Exception e) {
                    Log.e("DB_ERROR", "Error fetching transactions: " + e.getMessage());
                }
            }
            cursor.close();
        } else {
            Log.e("DB_ERROR", "Cursor is null. No transactions found.");
        }
        db.close();
        return transactions;
    }

    public List<Transaction> getTransactionsForMonth(int month, int year) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
                " WHERE strftime('%m', date) = ? AND strftime('%Y', date) = ?" +
                " ORDER BY date DESC, time DESC";

        Cursor cursor = db.rawQuery(query, new String[]{
                String.format(Locale.US, "%02d", month), String.valueOf(year)});

        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                    String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                    BigDecimal amount = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                    String note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE));

                    transactions.add(new Transaction(id, amount, type, note, date, time));
                } catch (Exception e) {
                    Log.e("DB_ERROR", "Error fetching transactions: " + e.getMessage());
                }
            }
            cursor.close();
        }
        db.close();
        return transactions;
    }

    public void debugDatabase() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT type, amount FROM transactions", null);

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(0);
                String amount = cursor.getString(1);
                Log.d("DB_CHECK", "Stored Type: " + type + ", Amount: " + amount);
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

}

