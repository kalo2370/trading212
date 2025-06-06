package com.cryptosim.trading212.daos;

import com.cryptosim.trading212.daos.contracts.TransactionDao;
import com.cryptosim.trading212.models.Transaction;
import com.cryptosim.trading212.models.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

/**
 * DAO for managing Transaction entities.
 * Handles database operations for the 'transactions' table.
 */
@Repository
public class TransactionDaoImpl implements TransactionDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TransactionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class TransactionRowMapper implements RowMapper<Transaction> {
        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            Transaction transaction = new Transaction();
            transaction.setTransactionId(rs.getInt("transaction_id"));
            transaction.setAccountId(rs.getInt("account_id"));
            transaction.setAssetSymbol(rs.getString("asset_symbol"));
            transaction.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type").toUpperCase()));
            transaction.setQuantity(rs.getBigDecimal("quantity"));
            transaction.setPricePerUnit(rs.getBigDecimal("price_per_unit"));
            transaction.setTotalTransactionValue(rs.getBigDecimal("total_transaction_value"));
            Timestamp transactionTimestamp = rs.getTimestamp("transaction_timestamp");
            if (transactionTimestamp != null) {
                transaction.setTransactionTimestamp(transactionTimestamp.toLocalDateTime());
            }
            transaction.setRealizedProfitLoss(rs.getBigDecimal("realized_profit_loss")); // Can be null
            return transaction;
        }
    }

    /**
     * Logs a new transaction into the database.
     *
     * @param transaction The Transaction object to log.
     * @return The logged Transaction object with its generated ID.
     */
    @Override
    public Transaction logTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (account_id, asset_symbol, transaction_type, quantity, " +
                "price_per_unit, total_transaction_value, transaction_timestamp, realized_profit_loss) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, transaction.getAccountId());
            ps.setString(2, transaction.getAssetSymbol());
            ps.setString(3, transaction.getTransactionType().name());
            ps.setBigDecimal(4, transaction.getQuantity());
            ps.setBigDecimal(5, transaction.getPricePerUnit());
            ps.setBigDecimal(6, transaction.getTotalTransactionValue());

            if (transaction.getTransactionTimestamp() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(transaction.getTransactionTimestamp()));
            } else {
                ps.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now())); // Default to now if not set
            }

            if (transaction.getRealizedProfitLoss() != null) {
                ps.setBigDecimal(8, transaction.getRealizedProfitLoss());
            } else {
                ps.setNull(8, java.sql.Types.DECIMAL);
            }
            return ps;
        }, keyHolder);

        int newTransactionId = keyHolder.getKey() != null ? keyHolder.getKey().intValue() : -1;
        if (newTransactionId == -1) {
            throw new RuntimeException("Failed to log transaction, no ID obtained for asset: " + transaction.getAssetSymbol());
        }
        transaction.setTransactionId(newTransactionId);
        return transaction;
    }

    /**
     * Finds all transactions for a given account ID, ordered by timestamp descending.
     *
     * @param accountId The ID of the account.
     * @return A list of Transaction objects.
     */
    @Override
    public List<Transaction> findByAccountId(int accountId) {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY transaction_timestamp DESC";
        return jdbcTemplate.query(sql, new Object[]{accountId}, new TransactionRowMapper());
    }
}

