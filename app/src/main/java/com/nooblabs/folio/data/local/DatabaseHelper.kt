package com.nooblabs.folio.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "folio.db"
        const val DATABASE_VERSION = 7
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE bank_accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                bankName TEXT NOT NULL,
                accountNumber TEXT NOT NULL,
                balance REAL NOT NULL,
                currency TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE stock_investments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tickerSymbol TEXT NOT NULL,
                quantity REAL NOT NULL,
                averageBuyPrice REAL NOT NULL,
                purchaseDate INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("""
            CREATE TABLE credit_cards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cardName TEXT NOT NULL,
                cardNumber TEXT NOT NULL,
                creditLimit REAL NOT NULL,
                currentOutstanding REAL NOT NULL,
                dueDate INTEGER NOT NULL,
                expiry TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL NOT NULL,
                date INTEGER NOT NULL,
                description TEXT NOT NULL,
                type TEXT NOT NULL,
                category TEXT NOT NULL,
                sourceId INTEGER NOT NULL,
                sourceType TEXT NOT NULL DEFAULT 'BANK',
                currency TEXT NOT NULL DEFAULT 'USD'
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS stock_prices_cache (
                tickerSymbol TEXT PRIMARY KEY,
                price REAL NOT NULL,
                lastUpdated INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var version = oldVersion
        if (version < 2) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS stock_prices_cache (
                    tickerSymbol TEXT PRIMARY KEY,
                    price REAL NOT NULL,
                    lastUpdated INTEGER NOT NULL
                )
            """)
            version = 2
        }
        if (version < 3) {
            // Migrate stock_investments to remove currentPrice column which is NOT NULL
            db.execSQL("ALTER TABLE stock_investments RENAME TO temp_stock_investments")
            db.execSQL("""
                CREATE TABLE stock_investments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tickerSymbol TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    averageBuyPrice REAL NOT NULL
                )
            """)
            db.execSQL("""
                INSERT INTO stock_investments (id, tickerSymbol, quantity, averageBuyPrice)
                SELECT id, tickerSymbol, quantity, averageBuyPrice FROM temp_stock_investments
            """)
            db.execSQL("DROP TABLE temp_stock_investments")
            version = 3
        }
        if (version < 4) {
            try {
                db.execSQL("ALTER TABLE stock_investments ADD COLUMN purchaseDate INTEGER NOT NULL DEFAULT 0")
            } catch (e: Exception) {
                // Column already exists — safe to ignore
            }
            version = 4
        }
        if (version < 5) {
            try {
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'BANK'")
            } catch (e: Exception) {
                // Column already exists — safe to ignore
            }
            version = 5
        }
        if (version < 6) {
            try {
                db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
            } catch (e: Exception) {
                // Column already exists — safe to ignore
            }
            version = 6
        }
        if (version < 7) {
            // Remove CVV column — recreate credit_cards table without it
            db.execSQL("ALTER TABLE credit_cards RENAME TO temp_credit_cards")
            db.execSQL("""
                CREATE TABLE credit_cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cardName TEXT NOT NULL,
                    cardNumber TEXT NOT NULL,
                    creditLimit REAL NOT NULL,
                    currentOutstanding REAL NOT NULL,
                    dueDate INTEGER NOT NULL,
                    expiry TEXT NOT NULL
                )
            """)
            db.execSQL("""
                INSERT INTO credit_cards (id, cardName, cardNumber, creditLimit, currentOutstanding, dueDate, expiry)
                SELECT id, cardName, cardNumber, creditLimit, currentOutstanding, dueDate, expiry
                FROM temp_credit_cards
            """)
            db.execSQL("DROP TABLE temp_credit_cards")
            version = 7
        }
    }
}
