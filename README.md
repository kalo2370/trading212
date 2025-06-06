# Crypto Trading Simulator
## Project Goal
Develop a web application that simulates a cryptocurrency trading platform, allowing users to:

View real-time prices of cryptocurrencies (simulated via Kraken API integration in the backend).

Maintain a virtual account balance for buying and selling crypto.

View a history of all transactions made.

Reset their account balance to a starting value.

## Technical Stack
Backend: Java with Spring Boot

Frontend: HTML, CSS (using Tailwind CSS for styling)

API Integration (Backend): Kraken V2 WebSocket API (for fetching real-time cryptocurrency prices)

Data Storage: MySQL or MariaDB

Build Tool: Gradle

## Setup and Running the Application
1. Database Setup (MySQL)

Prerequisites:   
- MySQL Server is installed and running.
- 
-A MySQL client (e.g., MySQL Workbench, DBeaver, or command line) to execute scripts.

Steps:

Create a new database for the application (e.g., crypto_simulator_db).

CREATE DATABASE crypto_simulator_db;

Connect to your MySQL server using your client and select the newly created database.

Execute the DDL script (schema.sql) to create the necessary tables (accounts, portfolio_assets, transactions) and insert the initial default_user.

2. Backend Setup (Spring Boot)
   Prerequisites:

Java Development Kit (JDK) 17 or later installed.

Gradle installed (or use the Gradle wrapper gradlew provided with the project).

Configuration:

Navigate to the backend/ directory of the project.

Open the src/main/resources/application.properties file.

Ensure the database connection properties are correctly configured for your MySQL setup:

spring.datasource.url=jdbc:mysql://localhost:3306/crypto_simulator_db # Replace crypto_simulator_db if you used a different name
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

Port for the Spring Boot application's web server
server.port=8080

Replace your_mysql_username and your_mysql_password with your actual MySQL credentials.

Running the Backend:

Open a terminal or command prompt.

Navigate to the root directory of the backend/ project.

Run the application using Trading212Application.java class

The backend application should start, and you'll see logs in the console. Look for messages indicating:

--Connection to the Kraken WebSocket API.

--Database connection pool initialization.

3. Frontend Setup & Running
Configuration:

Open the main application file (App.js).

Verify the API_BASE_URL constant is pointing to your running backend:

const API_BASE_URL = 'http://localhost:8080/api'; or const API_BASE_URL = '';

4. Accessing the Application

Serving frontend from Spring Boot's static resources (backend/src/main/resources/static/index.html):

Open your browser and navigate to http://localhost:8080/ (assuming your backend is on port 8080).

## How to Use
Once the application is running, the main page will display:

-Live cryptocurrency prices.

-Your account balance (initially $10,000 or as per database setup).

-Trading forms to buy and sell crypto.

-Your portfolio of held assets.

-A history of your transactions.

-Select a Crypto: Click "Select" next to a cryptocurrency in the "Live Cryptocurrency Prices" table. This will populate it in the trading form.

-Buy/Sell: Enter the desired quantity in the trading form and click "Buy" or "Sell".

-View Updates: Your account balance, portfolio, and transaction history will update automatically.

-Reset Account: Click the "Reset Account" button in the header to restore your initial balance and clear your portfolio and transactions.

Enjoy simulating your crypto trades!
