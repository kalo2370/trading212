Crypto Trading Simulator
Project Goal
Develop a web application that simulates a cryptocurrency trading platform, allowing users to:

View real-time prices of cryptocurrencies (simulated via Kraken API integration in the backend).

Maintain a virtual account balance for buying and selling crypto.

View a history of all transactions made.

Reset their account balance to a starting value.

Technical Stack
Backend: Java with Spring Boot

Frontend: React (using Tailwind CSS for styling)

API Integration (Backend): Kraken V2 WebSocket API (for fetching real-time cryptocurrency prices)

Data Storage: MySQL (or any relational database compatible with the provided DDL)

Build Tool: Gradle

Project Structure (Simplified)
crypto-trading-simulator/
├── backend/ (Spring Boot Project)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cryptosim/trading212/
│   │   │   │   ├── controllers/
│   │   │   │   ├── daos/
│   │   │   │   ├── dtos/
│   │   │   │   ├── models/
│   │   │   │   └── services/
│   │   │   ├── resources/
│   │   │   │   ├── static/      <-- Frontend files (index.html, app.js if served by Spring)
│   │   │   │   └── application.properties
│   └── build.gradle
├── frontend/ (React Project - if developed separately)
│   ├── public/
│   │   └── index.html
│   ├── src/
│   │   ├── App.js
│   │   ├── index.js
│   │   └── ... (other components)
│   └── package.json
└── README.md

(Adjust the structure above if your React frontend is being served by Spring Boot from src/main/resources/static directly within the backend project).

Setup and Running the Application
1. Database Setup (MySQL)
   Prerequisites:

MySQL Server installed and running.

A MySQL client (e.g., MySQL Workbench, DBeaver, or command line) to execute scripts.

Steps:

Create a new database for the application (e.g., crypto_simulator_db).

CREATE DATABASE crypto_simulator_db;

Connect to your MySQL server using your client and select the newly created database.

Execute the DDL script provided (crypto_simulator_ddl.sql - the one you shared earlier) to create the necessary tables (accounts, portfolio_assets, transactions) and insert the initial default_user.

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

# Port for the Spring Boot application's web server
server.port=8080

Replace your_mysql_username and your_mysql_password with your actual MySQL credentials.

Running the Backend:

Open a terminal or command prompt.

Navigate to the root directory of the backend/ project.

Run the application using Gradle:

On macOS/Linux: ./gradlew bootRun

On Windows: gradlew.bat bootRun

Alternatively, you can build the project (./gradlew build) and then run the JAR file from the build/libs/ directory (java -jar your-app-name.jar).

The backend application should start, and you'll see logs in the console. Look for messages indicating:

Tomcat started on port 8080 (or your configured port).

Connection to the Kraken WebSocket API.

Database connection pool initialization.

3. Frontend Setup & Running (React)
   This section assumes you have a separate React project for the frontend. If your frontend HTML/JS is being served directly by Spring Boot from src/main/resources/static/ within the backend project, you can skip the React development server steps and directly access the application as described in "Accessing the Application".

Prerequisites:

Node.js and npm (or yarn) installed.

Configuration:

Navigate to the frontend/ directory (your React project).

Open the main application file (e.g., src/App.js).

Verify the API_BASE_URL constant is pointing to your running backend:

const API_BASE_URL = 'http://localhost:8080/api'; // Ensure this matches your backend's URL and port

Installing Dependencies:

Open a terminal in the frontend/ directory.

Run npm install (or yarn install) to download the necessary packages.

Running the Frontend Development Server:

In the same terminal (in frontend/), run:

npm start (for Create React App)

npm run dev (for Vite or other setups)

This will typically open the application in your default web browser at a URL like http://localhost:3000 or http://localhost:5173.

4. Accessing the Application
   If serving React frontend from its own development server:

Open the URL provided by the React development server (e.g., http://localhost:3000).

If serving frontend from Spring Boot's static resources (backend/src/main/resources/static/index.html):

Open your browser and navigate to http://localhost:8080/ (assuming your backend is on port 8080).

5. CORS Configuration (Important!)
   If your frontend is served from a different origin (e.g., http://localhost:3000) than your backend (http://localhost:8080), you must ensure Cross-Origin Resource Sharing (CORS) is enabled on the backend.

Your Spring Boot application should have a WebConfig.java (or similar @Configuration class) that allows requests from your frontend's origin. Example:

// In com.cryptosim.trading212.config.WebConfig.java (or similar)
@Configuration
public class WebConfig implements WebMvcConfigurer {
@Override
public void addCorsMappings(CorsRegistry registry) {
registry.addMapping("/api/**")
.allowedOrigins(
"http://localhost:3000", // For typical React dev server
"http://localhost:5173"  // For typical Vite dev server
// Add any other origins as needed
)
.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
.allowedHeaders("*")
.allowCredentials(false);
}
}

If you modify CORS settings, restart your Spring Boot backend.

How to Use
Once the application is running, the main page will display:

Live cryptocurrency prices.

Your account balance (initially $10,000 or as per database setup).

Trading forms to buy and sell crypto.

Your portfolio of held assets.

A history of your transactions.

Select a Crypto: Click "Select" next to a cryptocurrency in the "Live Cryptocurrency Prices" table. This will populate it in the trading form.

Buy/Sell: Enter the desired quantity in the trading form and click "Buy" or "Sell".

View Updates: Your account balance, portfolio, and transaction history will update automatically.

Reset Account: Click the "Reset Account" button in the header to restore your initial balance and clear your portfolio and transactions.

Troubleshooting
Backend Fails to Start:

Check application.properties for correct database URL, username, password.

Ensure your MySQL server is running.

Check for port conflicts (e.g., if port 8080 is already in use).

Frontend Doesn't Load Data / API Errors:

Open your browser's Developer Console (F12).

Look at the "Console" tab for JavaScript errors.

Look at the "Network" tab for failed API requests.

CORS errors: Verify backend CORS configuration and frontend API_BASE_URL.

404 Not Found: API endpoint might be incorrect or backend not running/reachable.

500 Internal Server Error: Check backend logs for Java exceptions.

Kraken API Errors (in backend logs):

Errors like "Currency pair not supported" mean the symbols in KrakenDataService.java need to be updated to match currently supported pairs on Kraken's V2 WebSocket.

Enjoy simulating your crypto trades!