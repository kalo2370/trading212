package com.cryptosim.trading212.daos.contracts;

import com.cryptosim.trading212.models.Transaction;
import java.util.List;

/**
 * Interface for Transaction Data Access Object.
 * Defines the contract for database operations related to the 'transactions' table.
 */
public interface TransactionDao {

    /**
     * Logs a new transaction into the database.
     *
     * @param transaction The Transaction object to log. This object should have its
     * accountId, assetSymbol, transactionType, quantity, pricePerUnit,
     * totalTransactionValue, and optionally realizedProfitLoss and
     * transactionTimestamp set.
     * @return The logged Transaction object, typically including its generated ID.
     * @throws RuntimeException if logging the transaction fails (e.g., no ID obtained).
     */
    Transaction logTransaction(Transaction transaction);

    /**
     * Finds all transactions for a given account ID, ordered by transaction_timestamp descending.
     *
     * @param accountId The ID of the account for which to retrieve transactions.
     * @return A list of Transaction objects. Returns an empty list if no transactions are found.
     */
    List<Transaction> findByAccountId(int accountId);
}
