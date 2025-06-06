package com.cryptosim.trading212.daos;

import com.cryptosim.trading212.daos.contracts.PortfolioAssetDao;
import com.cryptosim.trading212.models.PortfolioAsset;
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
import java.util.List;
import java.util.Optional;

/**
 * DAO for managing PortfolioAsset entities.
 * Handles database operations for the 'portfolio_assets' table.
 */
@Repository
public class PortfolioAssetDaoImpl implements PortfolioAssetDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PortfolioAssetDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class PortfolioAssetRowMapper implements RowMapper<PortfolioAsset> {
        @Override
        public PortfolioAsset mapRow(ResultSet rs, int rowNum) throws SQLException {
            PortfolioAsset asset = new PortfolioAsset();
            asset.setAssetId(rs.getInt("asset_id"));
            asset.setAccountId(rs.getInt("account_id"));
            asset.setAssetSymbol(rs.getString("asset_symbol"));
            asset.setQuantity(rs.getBigDecimal("quantity"));
            asset.setAveragePurchasePrice(rs.getBigDecimal("average_purchase_price"));
            Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
            if (createdAtTimestamp != null) {
                asset.setCreatedAt(createdAtTimestamp.toLocalDateTime());
            }
            Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");
            if (updatedAtTimestamp != null) {
                asset.setUpdatedAt(updatedAtTimestamp.toLocalDateTime());
            }
            return asset;
        }
    }

    /**
     * Finds all portfolio assets for a given account ID.
     *
     * @param accountId The ID of the account.
     * @return A list of PortfolioAsset objects.
     */
    @Override
    public List<PortfolioAsset> findByAccountId(int accountId) {
        String sql = "SELECT * FROM portfolio_assets WHERE account_id = ?";
        return jdbcTemplate.query(sql, new Object[]{accountId}, new PortfolioAssetRowMapper());
    }

    /**
     * Finds a specific portfolio asset by account ID and asset symbol.
     *
     * @param accountId   The ID of the account.
     * @param assetSymbol The symbol of the asset (e.g., "XBT/USD").
     * @return An Optional containing the PortfolioAsset if found, otherwise empty.
     */
    @Override
    public Optional<PortfolioAsset> findByAccountIdAndAssetSymbol(int accountId, String assetSymbol) {
        String sql = "SELECT * FROM portfolio_assets WHERE account_id = ? AND asset_symbol = ?";
        try {
            PortfolioAsset asset = jdbcTemplate.queryForObject(sql, new Object[]{accountId, assetSymbol}, new PortfolioAssetRowMapper());
            return Optional.ofNullable(asset);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Adds a new asset to an account's portfolio.
     *
     * @param asset The PortfolioAsset object to add.
     * @return The added PortfolioAsset object with its generated ID.
     */
    @Override
    public PortfolioAsset addAsset(PortfolioAsset asset) {
        String sql = "INSERT INTO portfolio_assets (account_id, asset_symbol, quantity, average_purchase_price, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, asset.getAccountId());
            ps.setString(2, asset.getAssetSymbol());
            ps.setBigDecimal(3, asset.getQuantity());
            ps.setBigDecimal(4, asset.getAveragePurchasePrice());
            return ps;
        }, keyHolder);

        int newAssetId = keyHolder.getKey() != null ? keyHolder.getKey().intValue() : -1;
        if (newAssetId == -1) {
            throw new RuntimeException("Failed to add asset, no ID obtained for asset: " + asset.getAssetSymbol());
        }
        asset.setAssetId(newAssetId);
        return asset;
    }

    /**
     * Updates the quantity and average purchase price of an existing asset.
     *
     * @param assetId            The ID of the asset to update.
     * @param newQuantity        The new quantity of the asset.
     * @param newAveragePrice    The new average purchase price of the asset.
     * @return true if the update was successful, false otherwise.
     */
    @Override
    public boolean updateAsset(int assetId, BigDecimal newQuantity, BigDecimal newAveragePrice) {
        String sql = "UPDATE portfolio_assets SET quantity = ?, average_purchase_price = ?, updated_at = CURRENT_TIMESTAMP WHERE asset_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, newQuantity, newAveragePrice, assetId);
        return rowsAffected > 0;
    }


    /**
     * Deletes an asset from the portfolio (e.g., when quantity becomes zero).
     *
     * @param assetId The ID of the asset to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    @Override
    public boolean deleteAsset(int assetId) {
        String sql = "DELETE FROM portfolio_assets WHERE asset_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, assetId);
        return rowsAffected > 0;
    }

    /**
     * Deletes all assets associated with a specific account ID.
     * Used for account reset functionality.
     *
     * @param accountId The ID of the account whose assets are to be deleted.
     * @return The number of assets deleted.
     */
    @Override
    public int deleteAllAssetsByAccountId(int accountId) {
        String sql = "DELETE FROM portfolio_assets WHERE account_id = ?";
        return jdbcTemplate.update(sql, accountId);
    }
}

