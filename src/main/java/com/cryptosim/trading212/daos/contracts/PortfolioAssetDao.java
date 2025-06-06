package com.cryptosim.trading212.daos.contracts;

import com.cryptosim.trading212.models.PortfolioAsset;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Interface for PortfolioAsset Data Access Object.
 * Defines the contract for database operations related to the 'portfolio_assets' table.
 */
public interface PortfolioAssetDao {

    /**
     * Finds all portfolio assets for a given account ID.
     *
     * @param accountId The ID of the account.
     * @return A list of PortfolioAsset objects. Returns an empty list if no assets are found.
     */
    List<PortfolioAsset> findByAccountId(int accountId);

    /**
     * Finds a specific portfolio asset by account ID and asset symbol.
     *
     * @param accountId   The ID of the account.
     * @param assetSymbol The symbol of the asset (e.g., "XBT/USD").
     * @return An Optional containing the PortfolioAsset if found, otherwise empty.
     */
    Optional<PortfolioAsset> findByAccountIdAndAssetSymbol(int accountId, String assetSymbol);

    /**
     * Adds a new asset to an account's portfolio.
     *
     * @param asset The PortfolioAsset object to add. The accountId, assetSymbol, quantity,
     * and averagePurchasePrice should be set in this object.
     * @return The added PortfolioAsset object, typically including its generated ID and persisted timestamps.
     * @throws RuntimeException if the asset creation fails (e.g., no ID obtained).
     */
    PortfolioAsset addAsset(PortfolioAsset asset);

    /**
     * Updates the quantity and average purchase price of an existing asset.
     *
     * @param assetId            The ID of the asset to update.
     * @param newQuantity        The new quantity of the asset.
     * @param newAveragePrice    The new average purchase price of the asset.
     * @return true if the update was successful (e.g., 1 row affected), false otherwise.
     */
    boolean updateAsset(int assetId, BigDecimal newQuantity, BigDecimal newAveragePrice);

    /**
     * Deletes an asset from the portfolio (e.g., when quantity becomes zero).
     *
     * @param assetId The ID of the asset to delete.
     * @return true if the deletion was successful (e.g., 1 row affected), false otherwise.
     */
    boolean deleteAsset(int assetId);

    /**
     * Deletes all assets associated with a specific account ID.
     * This is typically used for account reset functionality.
     *
     * @param accountId The ID of the account whose assets are to be deleted.
     * @return The number of assets (rows) deleted.
     */
    int deleteAllAssetsByAccountId(int accountId);
}
