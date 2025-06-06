DROP TABLE IF EXISTS `transactions`;
DROP TABLE IF EXISTS `portfolio_assets`;
DROP TABLE IF EXISTS `accounts`;

CREATE TABLE `accounts` (
                            `account_id` INT AUTO_INCREMENT PRIMARY KEY,
                            `user_identifier` VARCHAR(255) UNIQUE NOT NULL COMMENT 'A unique identifier for the user, "default_user"',
                            `balance` DECIMAL(20, 8) NOT NULL COMMENT 'Current fiat balance',
                            `initial_balance` DECIMAL(20, 8) NOT NULL COMMENT 'Initial fiat balance for reset functionality',
                            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            INDEX `idx_user_identifier` (`user_identifier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores user account information and balances';


CREATE TABLE `portfolio_assets` (
                                    `asset_id` INT AUTO_INCREMENT PRIMARY KEY,
                                    `account_id` INT NOT NULL,
                                    `asset_symbol` VARCHAR(30) NOT NULL COMMENT 'Cryptocurrency pair symbol from the exchange',
                                    `quantity` DECIMAL(24, 10) NOT NULL COMMENT 'Quantity of the crypto asset held',
                                    `average_purchase_price` DECIMAL(20, 8) NOT NULL COMMENT 'Weighted average price at which this asset was acquired (in fiat)',
                                    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    FOREIGN KEY (`account_id`) REFERENCES `accounts`(`account_id`) ON DELETE CASCADE,
                                    UNIQUE KEY `uk_account_asset` (`account_id`, `asset_symbol`) COMMENT 'Ensures one entry per asset type per account'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stores crypto assets held by users';

CREATE TABLE `transactions` (
                                `transaction_id` INT AUTO_INCREMENT PRIMARY KEY,
                                `account_id` INT NOT NULL,
                                `asset_symbol` VARCHAR(30) NOT NULL COMMENT 'Cryptocurrency pair symbol',
                                `transaction_type` ENUM('BUY', 'SELL') NOT NULL COMMENT 'Type of transaction',
                                `quantity` DECIMAL(24, 10) NOT NULL COMMENT 'Quantity of crypto transacted',
                                `price_per_unit` DECIMAL(20, 8) NOT NULL COMMENT 'Price per unit of crypto at the time of transaction (in fiat)',
                                `total_transaction_value` DECIMAL(38, 10) NOT NULL COMMENT 'Total fiat value of the transaction (quantity * price_per_unit)',
                                `transaction_timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                `realized_profit_loss` DECIMAL(20, 8) DEFAULT NULL COMMENT 'Profit or loss realized on this transaction',
                                FOREIGN KEY (`account_id`) REFERENCES `accounts`(`account_id`) ON DELETE CASCADE,
                                INDEX `idx_account_timestamp` (`account_id`, `transaction_timestamp` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Logs all buy and sell transactions';

INSERT INTO `accounts` (`user_identifier`, `balance`, `initial_balance`)
VALUES ('default_user', 10000.00000000, 10000.00000000);
