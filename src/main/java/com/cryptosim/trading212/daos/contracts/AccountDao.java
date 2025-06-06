package com.cryptosim.trading212.daos.contracts;

import com.cryptosim.trading212.models.Account;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Interface for Account Data Access Object.
 * Defines the contract for database operations related to the 'accounts' table.
 */
public interface AccountDao {

    /**
     * Finds an account by its user identifier.
     *
     * @param userIdentifier The unique identifier for the user.
     * @return An Optional containing the Account if found, otherwise empty.
     */
    Optional<Account> findByUserIdentifier(String userIdentifier);

    /**
     * Updates the balance of a specific account.
     *
     * @param accountId  The ID of the account to update.
     * @param newBalance The new balance for the account.
     * @return true if the update was successful (e.g., 1 row affected), false otherwise.
     */
    boolean updateBalance(int accountId, BigDecimal newBalance);

    /**
     * Retrieves the initial balance for a given account.
     *
     * @param accountId The ID of the account.
     * @return An Optional containing the initial balance if the account exists, otherwise empty.
     */
    Optional<BigDecimal> getInitialBalance(int accountId);

    /**
     * Creates a new account in the database.
     *
     * @param userIdentifier The unique identifier for the user.
     * @param initialBalance The initial balance for the account. This will also be set as the current balance.
     * @return The created Account object, typically including its generated ID and persisted timestamps.
     * The exact details of the returned object (e.g., whether timestamps are immediately populated
     * or if a subsequent fetch is needed) might depend on the implementation.
     */
    Account createAccount(String userIdentifier, BigDecimal initialBalance);

    /**
     * Retrieves an account by its primary key ID.
     *
     * @param accountId The ID of the account.
     * @return An Optional containing the Account if found, otherwise empty.
     */
    Optional<Account> findById(int accountId);
}
