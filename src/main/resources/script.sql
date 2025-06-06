
INSERT INTO `transactions`
(`account_id`, `asset_symbol`, `transaction_type`, `quantity`, `price_per_unit`, `total_transaction_value`, `transaction_timestamp`, `realized_profit_loss`)
VALUES
    (1, 'XBT/USD', 'BUY', 0.1000000000, 50000.00000000, 5000.0000000000, '2025-06-01 10:00:00', NULL);


INSERT INTO `transactions`
(`account_id`, `asset_symbol`, `transaction_type`, `quantity`, `price_per_unit`, `total_transaction_value`, `transaction_timestamp`, `realized_profit_loss`)
VALUES
    (1, 'ETH/USD', 'BUY', 1.0000000000, 3000.00000000, 3000.0000000000, '2025-06-02 11:00:00', NULL);


INSERT INTO `transactions`
(`account_id`, `asset_symbol`, `transaction_type`, `quantity`, `price_per_unit`, `total_transaction_value`, `transaction_timestamp`, `realized_profit_loss`)
VALUES
    (1, 'XBT/USD', 'BUY', 0.0200000000, 52000.00000000, 1040.0000000000, '2025-06-03 12:00:00', NULL);

INSERT INTO `transactions`
(`account_id`, `asset_symbol`, `transaction_type`, `quantity`, `price_per_unit`, `total_transaction_value`, `transaction_timestamp`, `realized_profit_loss`)
VALUES
    (1, 'ETH/USD', 'SELL', 0.5000000000, 3200.00000000, 1600.0000000000, '2025-06-04 14:00:00', 100.00000000);


INSERT INTO `portfolio_assets`
(`account_id`, `asset_symbol`, `quantity`, `average_purchase_price`, `created_at`, `updated_at`)
VALUES
    (1, 'XBT/USD', 0.1200000000, 50333.33333333, '2025-06-01 10:00:00', '2025-06-03 12:00:00');

INSERT INTO `portfolio_assets`
(`account_id`, `asset_symbol`, `quantity`, `average_purchase_price`, `created_at`, `updated_at`)
VALUES
    (1, 'ETH/USD', 0.5000000000, 3000.00000000, '2025-06-02 11:00:00', '2025-06-04 14:00:00');

UPDATE `accounts`
SET `balance` = 2560.00000000, `updated_at` = '2025-06-04 14:00:01' -- Timestamp slightly after last transaction
WHERE `account_id` = 1 AND `user_identifier` = 'default_user';