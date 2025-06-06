package com.cryptosim.trading212.daos;

import com.cryptosim.trading212.daos.contracts.AccountDao;
import com.cryptosim.trading212.models.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * DAO for managing Account entities.
 * Handles database operations for the 'accounts' table.
 */
@Repository
public class AccountDaoImpl implements AccountDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AccountDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class AccountRowMapper implements RowMapper<Account> {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            Account account = new Account();
            account.setAccountId(rs.getInt("account_id"));
            account.setUserIdentifier(rs.getString("user_identifier"));
            account.setBalance(rs.getBigDecimal("balance"));
            account.setInitialBalance(rs.getBigDecimal("initial_balance"));
            Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
            if (createdAtTimestamp != null) {
                account.setCreatedAt(createdAtTimestamp.toLocalDateTime());
            }
            Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");
            if (updatedAtTimestamp != null) {
                account.setUpdatedAt(updatedAtTimestamp.toLocalDateTime());
            }
            return account;
        }
    }

    /**
     * Finds an account by its user identifier.
     *
     * @param userIdentifier The unique identifier for the user.
     * @return An Optional containing the Account if found, otherwise empty.
     */
    @Override
    public Optional<Account> findByUserIdentifier(String userIdentifier) {
        String sql = "SELECT * FROM accounts WHERE user_identifier = ?";
        try {
            Account account = jdbcTemplate.queryForObject(sql, new Object[]{userIdentifier}, new AccountRowMapper());
            return Optional.ofNullable(account);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Updates the balance of a specific account.
     *
     * @param accountId  The ID of the account to update.
     * @param newBalance The new balance for the account.
     * @return true if the update was successful (1 row affected), false otherwise.
     */
    @Override
    public boolean updateBalance(int accountId, BigDecimal newBalance) {
        String sql = "UPDATE accounts SET balance = ?, updated_at = CURRENT_TIMESTAMP WHERE account_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, newBalance, accountId);
        return rowsAffected > 0;
    }

    /**
     * Retrieves the initial balance for a given account.
     *
     * @param accountId The ID of the account.
     * @return An Optional containing the initial balance if the account exists, otherwise empty.
     */
    @Override
    public Optional<BigDecimal> getInitialBalance(int accountId) {
        String sql = "SELECT initial_balance FROM accounts WHERE account_id = ?";
        try {
            BigDecimal initialBalance = jdbcTemplate.queryForObject(sql, new Object[]{accountId}, BigDecimal.class);
            return Optional.ofNullable(initialBalance);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new account.
     *
     * @param userIdentifier The unique identifier for the user.
     * @param initialBalance The initial balance for the account.
     * @return The created Account object with its generated ID.
     */
    //TODO: If you have time make a login
    @Override
    public Account createAccount(String userIdentifier, BigDecimal initialBalance) {
        String sql = "INSERT INTO accounts (user_identifier, balance, initial_balance, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, userIdentifier);
            ps.setBigDecimal(2, initialBalance);
            ps.setBigDecimal(3, initialBalance); // Balance is initially the same as initialBalance
            return ps;
        }, keyHolder);

        int newAccountId = keyHolder.getKey() != null ? keyHolder.getKey().intValue() : -1;
        if (newAccountId == -1) {
            throw new RuntimeException("Failed to create account, no ID obtained.");
        }
        Account newAccount = new Account();
        newAccount.setAccountId(newAccountId);
        newAccount.setUserIdentifier(userIdentifier);
        newAccount.setBalance(initialBalance);
        newAccount.setInitialBalance(initialBalance);
        return newAccount;
    }

    /**
     * Retrieves an account by its ID.
     *
     * @param accountId The ID of the account.
     * @return An Optional containing the Account if found, otherwise empty.
     */
    @Override
    public Optional<Account> findById(int accountId) {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";
        try {
            Account account = jdbcTemplate.queryForObject(sql, new Object[]{accountId}, new AccountRowMapper());
            return Optional.ofNullable(account);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
