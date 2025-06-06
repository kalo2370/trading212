document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = ''; // Assuming backend is running on the same origin, or specify full URL e.g. http://localhost:8080
    const USER_IDENTIFIER = 'default_user'; // As per your DDL script

    // UI Elements
    const cryptoPricesTableBody = document.getElementById('cryptoPricesTableBody');
    const buyCryptoSymbolSelect = document.getElementById('buyCryptoSymbol');
    const sellCryptoSymbolSelect = document.getElementById('sellCryptoSymbol');
    const accountBalanceDisplay = document.getElementById('accountBalanceDisplay');
    const userIdentifierDisplay = document.getElementById('userIdentifierDisplay');
    const portfolioTableBody = document.getElementById('portfolioTableBody');
    const transactionHistoryTableBody = document.getElementById('transactionHistoryTableBody');
    const buyForm = document.getElementById('buyForm');
    const sellForm = document.getElementById('sellForm');
    const resetAccountBtn = document.getElementById('resetAccountBtn');
    const notificationArea = document.getElementById('notificationArea');

    let availableCryptoForTrading = []; // To store symbols like "XBT/USD" for dropdowns

    // --- UTILITY FUNCTIONS ---
    function showNotification(message, type = 'info', duration = 4000) {
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        notificationArea.appendChild(notification);

        setTimeout(() => {
            notification.style.opacity = '0';
            setTimeout(() => notification.remove(), 300); // Remove after fade out
        }, duration);
    }

    function formatCurrency(amount, currency = 'USD') {
        if (amount === null || amount === undefined || typeof amount === 'string' && amount.toLowerCase() === 'n/a') {
            return 'N/A';
        }
        const numericAmount = Number(amount);
        if (isNaN(numericAmount)) {
            return 'N/A';
        }
        return numericAmount.toLocaleString('en-US', { style: 'currency', currency: currency, minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function formatCryptoQuantity(quantity) {
        if (quantity === null || quantity === undefined) return 'N/A';
        const numericQuantity = Number(quantity);
        if (isNaN(numericQuantity)) return 'N/A';
        // Show more precision for crypto
        return numericQuantity.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 8 });
    }

    function formatDate(dateString) {
        if (!dateString) return 'N/A';
        try {
            const date = new Date(dateString);
            return date.toLocaleString();
        } catch (e) {
            return dateString; // fallback
        }
    }

    // --- API CALLS ---
    async function fetchData(endpoint, options = {}) {
        const url = `${API_BASE_URL}/api${endpoint}`;
        try {
            const response = await fetch(url, options);
            if (!response.ok) {
                const errorBody = await response.text(); // Try to get error message from backend
                throw new Error(`HTTP error! Status: ${response.status} - ${errorBody || response.statusText}`);
            }
            // Check if response has content before parsing as JSON
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                return await response.json();
            }
            return null; // No JSON content
        } catch (error) {
            console.error(`Error fetching ${url}:`, error);
            showNotification(`Error fetching data: ${error.message}`, 'error');
            throw error;
        }
    }

    async function fetchAndDisplayPrices() {
        try {
            const prices = await fetchData('/prices');
            if (!prices) {
                cryptoPricesTableBody.innerHTML = '<tr><td colspan="3" class="text-center py-4">Could not load prices.</td></tr>';
                return;
            }

            availableCryptoForTrading = Object.keys(prices); // Store symbols like "XBT/USD"

            if (availableCryptoForTrading.length === 0) {
                cryptoPricesTableBody.innerHTML = '<tr><td colspan="3" class="text-center py-4">No cryptocurrency prices available currently.</td></tr>';
                updateTradeFormSymbols([]); // Clear dropdowns
                return;
            }

            cryptoPricesTableBody.innerHTML = ''; // Clear existing rows or loader
            availableCryptoForTrading.forEach(symbol => {
                const price = prices[symbol];
                const [base, quote] = symbol.split('/'); // e.g., XBT from XBT/USD
                const row = `
                    <tr>
                        <td class="font-medium text-sky-400">${symbol}</td>
                        <td>${base}</td>
                        <td class="font-semibold text-emerald-400">${formatCurrency(price, quote)}</td>
                    </tr>
                `;
                cryptoPricesTableBody.insertAdjacentHTML('beforeend', row);
            });
            updateTradeFormSymbols(availableCryptoForTrading);
        } catch (error) {
            cryptoPricesTableBody.innerHTML = '<tr><td colspan="3" class="text-center py-4">Failed to load prices. Check console.</td></tr>';
        }
    }

    async function fetchAndDisplayAccountDetails() {
        try {
            const account = await fetchData(`/account/${USER_IDENTIFIER}`);
            if (account) {
                accountBalanceDisplay.textContent = formatCurrency(account.balance);
                if (userIdentifierDisplay) userIdentifierDisplay.textContent = account.userIdentifier;
            } else {
                accountBalanceDisplay.textContent = formatCurrency(0); // Default if error
                if (userIdentifierDisplay) userIdentifierDisplay.textContent = USER_IDENTIFIER;
            }
        } catch (error) {
            accountBalanceDisplay.textContent = 'Error';
        }
    }

    async function fetchAndDisplayPortfolio() {
        try {
            const portfolio = await fetchData(`/account/${USER_IDENTIFIER}/portfolio`);
            sellCryptoSymbolSelect.innerHTML = '<option value="">Select Crypto</option>'; // Clear and add default

            if (portfolio && portfolio.length > 0) {
                portfolioTableBody.innerHTML = ''; // Clear existing
                portfolio.forEach(asset => {
                    const row = `
                        <tr>
                            <td class="font-medium text-sky-400">${asset.assetSymbol}</td>
                            <td>${formatCryptoQuantity(asset.quantity)}</td>
                            <td>${formatCurrency(asset.averagePurchasePrice)}</td>
                            <td class="font-semibold text-emerald-400">${formatCurrency(asset.currentMarketValue)}</td>
                        </tr>
                    `;
                    portfolioTableBody.insertAdjacentHTML('beforeend', row);

                    // Populate sell dropdown
                    const option = document.createElement('option');
                    option.value = asset.assetSymbol;
                    option.textContent = `${asset.assetSymbol} (Qty: ${formatCryptoQuantity(asset.quantity)})`;
                    sellCryptoSymbolSelect.appendChild(option);
                });
            } else {
                portfolioTableBody.innerHTML = '<tr><td colspan="4" class="text-center py-4">Your portfolio is empty.</td></tr>';
            }
        } catch (error) {
            portfolioTableBody.innerHTML = '<tr><td colspan="4" class="text-center py-4">Failed to load portfolio.</td></tr>';
        }
    }

    async function fetchAndDisplayTransactions() {
        try {
            const transactions = await fetchData(`/account/${USER_IDENTIFIER}/transactions`);
            if (transactions && transactions.length > 0) {
                transactionHistoryTableBody.innerHTML = ''; // Clear existing
                transactions.forEach(tx => {
                    let profitLossDisplay = 'N/A';
                    let profitLossClass = 'badge-neutral';
                    if (tx.transactionType === 'SELL' && tx.realizedProfitLoss !== null && tx.realizedProfitLoss !== undefined) {
                        const pnl = Number(tx.realizedProfitLoss);
                        if (pnl > 0) {
                            profitLossDisplay = `+${formatCurrency(pnl)}`;
                            profitLossClass = 'badge-success';
                        } else if (pnl < 0) {
                            profitLossDisplay = formatCurrency(pnl);
                            profitLossClass = 'badge-danger';
                        } else {
                            profitLossDisplay = formatCurrency(pnl); // Zero P/L
                        }
                    }

                    const row = `
                        <tr>
                            <td>${formatDate(tx.transactionTimestamp)}</td>
                            <td><span class="badge ${tx.transactionType === 'BUY' ? 'badge-success' : 'badge-danger'}">${tx.transactionType}</span></td>
                            <td class="font-medium text-sky-400">${tx.assetSymbol}</td>
                            <td>${formatCryptoQuantity(tx.quantity)}</td>
                            <td>${formatCurrency(tx.pricePerUnit)}</td>
                            <td>${formatCurrency(tx.totalTransactionValue)}</td>
                            <td><span class="badge ${profitLossClass}">${profitLossDisplay}</span></td>
                        </tr>
                    `;
                    transactionHistoryTableBody.insertAdjacentHTML('beforeend', row);
                });
            } else {
                transactionHistoryTableBody.innerHTML = '<tr><td colspan="7" class="text-center py-4">No transactions yet.</td></tr>';
            }
        } catch (error) {
            transactionHistoryTableBody.innerHTML = '<tr><td colspan="7" class="text-center py-4">Failed to load transaction history.</td></tr>';
        }
    }

    function updateTradeFormSymbols(symbols) {
        buyCryptoSymbolSelect.innerHTML = '<option value="">Select Crypto</option>'; // Clear and add default
        symbols.forEach(symbol => {
            const option = document.createElement('option');
            option.value = symbol;
            option.textContent = symbol;
            buyCryptoSymbolSelect.appendChild(option.cloneNode(true)); // Use cloneNode for buy, sell list is from portfolio
        });
    }


    // --- EVENT HANDLERS ---
    buyForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const assetSymbol = buyForm.buyCryptoSymbol.value;
        const quantity = buyForm.buyQuantity.value;

        if (!assetSymbol || !quantity || parseFloat(quantity) <= 0) {
            showNotification('Please select a cryptocurrency and enter a valid positive quantity to buy.', 'error');
            return;
        }

        try {
            const tradeRequest = { userIdentifier: USER_IDENTIFIER, assetSymbol, quantity: parseFloat(quantity) };
            const result = await fetchData('/trade/buy', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(tradeRequest)
            });
            if (result) {
                showNotification(`Successfully bought ${formatCryptoQuantity(quantity)} ${assetSymbol}!`, 'success');
                buyForm.reset();
                fetchAllData(); // Refresh all data
            }
        } catch (error) {
            // Error already shown by fetchData
            console.error("Buy operation failed:", error);
        }
    });

    sellForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const assetSymbol = sellForm.sellCryptoSymbol.value;
        const quantity = sellForm.sellQuantity.value;

        if (!assetSymbol || !quantity || parseFloat(quantity) <= 0) {
            showNotification('Please select a cryptocurrency from your portfolio and enter a valid positive quantity to sell.', 'error');
            return;
        }

        try {
            const tradeRequest = { userIdentifier: USER_IDENTIFIER, assetSymbol, quantity: parseFloat(quantity) };
            const result = await fetchData('/trade/sell', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(tradeRequest)
            });
            if (result) {
                showNotification(`Successfully sold ${formatCryptoQuantity(quantity)} ${assetSymbol}!`, 'success');
                sellForm.reset();
                fetchAllData(); // Refresh all data
            }
        } catch (error) {
            // Error already shown by fetchData
            console.error("Sell operation failed:", error);
        }
    });

    resetAccountBtn.addEventListener('click', async () => {
        if (confirm('Are you sure you want to reset your account? This will restore your initial balance and clear your portfolio.')) {
            try {
                const result = await fetchData(`/account/${USER_IDENTIFIER}/reset`, { method: 'POST' });
                if (result) {
                    showNotification('Account reset successfully!', 'success');
                    fetchAllData(); // Refresh all data
                }
            } catch (error) {
                // Error already shown by fetchData
                console.error("Reset account failed:", error);
            }
        }
    });

    // --- INITIALIZATION ---
    async function fetchAllData() {
        // Show loaders or placeholders before fetching
        if (cryptoPricesTableBody) cryptoPricesTableBody.innerHTML = '<tr><td colspan="3" class="text-center py-4"><div class="loader"></div> Loading prices...</td></tr>';
        if (portfolioTableBody) portfolioTableBody.innerHTML = '<tr><td colspan="4" class="text-center py-4"><div class="loader"></div> Loading portfolio...</td></tr>';
        if (transactionHistoryTableBody) transactionHistoryTableBody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><div class="loader"></div> Loading transactions...</td></tr>';


        await fetchAndDisplayAccountDetails(); // Fetch balance first
        await fetchAndDisplayPrices(); // Prices needed for trade forms and portfolio enrichment
        await fetchAndDisplayPortfolio(); // Portfolio needed for sell form
        await fetchAndDisplayTransactions();
    }

    // Initial data load
    fetchAllData();

    // Periodically refresh prices (e.g., every 10 seconds)
    // The Kraken WebSocket on the backend should keep prices up-to-date,
    // so this polling might be for reflecting those latest cached prices.
    const PRICE_REFRESH_INTERVAL = 10000; // 10 seconds
    setInterval(async () => {
        try {
            const prices = await fetchData('/prices');
            if (prices) {
                availableCryptoForTrading = Object.keys(prices);
                if (cryptoPricesTableBody.querySelector('.loader')) { // If it's still showing initial loader
                    fetchAndDisplayPrices(); // Full redraw
                } else {
                    // More granular update if needed to avoid full table redraw
                    availableCryptoForTrading.forEach(symbol => {
                        const price = prices[symbol];
                        const [base, quote] = symbol.split('/');
                        // Find the row and update price cell if it exists
                        const rows = cryptoPricesTableBody.querySelectorAll('tr');
                        for (const row of rows) {
                            const symbolCell = row.cells[0];
                            if (symbolCell && symbolCell.textContent === symbol) {
                                row.cells[2].textContent = formatCurrency(price, quote);
                                break;
                            }
                        }
                    });
                }
                updateTradeFormSymbols(availableCryptoForTrading);
                // Also refresh portfolio for current market values
                fetchAndDisplayPortfolio();
            }
        } catch (error) {
            console.warn("Periodic price refresh failed:", error.message);
        }
    }, PRICE_REFRESH_INTERVAL);

});