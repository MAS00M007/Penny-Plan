package com.z8ten.pennyplan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PennyPlan.db";
    private static final int DATABASE_VERSION = 2;

    // Table and Columns
    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_DATE = "date";

    // SQL Query to Create Table
    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_AMOUNT + " REAL, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_NOTE + " TEXT, " +
                    COLUMN_DATE + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) { // If upgrading from version 1 to 2
            db.execSQL("ALTER TABLE " + TABLE_TRANSACTIONS + " ADD COLUMN " + COLUMN_NOTE + " TEXT DEFAULT ''");
        }
    }


    // Insert a Transaction
    public long insertTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_AMOUNT, transaction.getAmount());
            values.put(COLUMN_TYPE, transaction.getType());
            values.put(COLUMN_NOTE, transaction.getNote());
            values.put(COLUMN_DATE, transaction.getDate());

            id = db.insert(TABLE_TRANSACTIONS, null, values);

            if (id == -1) {
                Log.e("DB_ERROR", "Failed to insert transaction: " + values.toString());
            } else {
                Log.d("DB_SUCCESS", "Transaction added: " + values.toString());
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Exception while inserting transaction", e);
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
            cursor = db.rawQuery("SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + COLUMN_DATE + " DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    Transaction transaction = new Transaction(
                            cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
                    );
                    transactions.add(transaction);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error fetching transactions", e);
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
            values.put(COLUMN_AMOUNT, transaction.getAmount());
            values.put(COLUMN_TYPE, transaction.getType());
            values.put(COLUMN_NOTE, transaction.getNote());
            values.put(COLUMN_DATE, transaction.getDate());

            rowsAffected = db.update(TABLE_TRANSACTIONS, values, COLUMN_ID + "=?", new String[]{String.valueOf(transaction.getId())});
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error updating transaction", e);
        } finally {
            db.close();
        }

        return rowsAffected;
    }

    // Delete a Transaction
    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            int rowsDeleted = db.delete(TABLE_TRANSACTIONS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
            if (rowsDeleted == 0) {
                Log.e("DB_ERROR", "No transaction deleted with ID: " + id);
            } else {
                Log.d("DB_SUCCESS", "Transaction deleted with ID: " + id);
            }
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error deleting transaction", e);
        } finally {
            db.close();
        }
    }

    public double getBalance() {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalSavings = 0, totalExpenses = 0;

        Cursor cursor = db.rawQuery("SELECT SUM(amount) FROM transactions WHERE type='Saving'", null);
        if (cursor.moveToFirst()) {
            totalSavings = cursor.getDouble(0);
        }
        cursor.close();

        cursor = db.rawQuery("SELECT SUM(amount) FROM transactions WHERE type='Expense'", null);
        if (cursor.moveToFirst()) {
            totalExpenses = cursor.getDouble(0);
        }
        cursor.close();
        db.close();

        return totalSavings - totalExpenses; // Balance = Savings - Expenses
    }

}
