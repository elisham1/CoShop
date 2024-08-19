Co-Shop Application
Version: 1.0
Date: July 29, 2024
Authors: Yael Borochov, Elisheva Klapisch
Academic Supervisor: Professor Rachel Ben Eliyahu Zahari

Co-Shop is an innovative Android application that connects people for joint online purchases, helping to reduce costs through group orders. The platform enables users to open new orders, join existing ones, and communicate effectively with other participants.

Installation Instructions
Prerequisites: Ensure you have Android Studio installed on your machine.
Clone the Repository:

Copy code
git clone https://github.com/elisham1/CoShop.git
Open in Android Studio: Open the cloned project in Android Studio.
Build the Project: Use Android Studio to build the project.
Run the Application: Deploy the application to an Android emulator or a physical device.
Features
User Authentication: Secure login using Google authentication.
Order Management: Create, join, and manage orders.
Chat System: In-app chat for coordinating orders.
Location-Based Recommendations: Suggests orders based on user location.
Secure Payments: Links to external payment services like PayBox for secure transactions.
Technologies Used
Programming Languages: Java, XML
Development Environment: Android Studio
Database: Firebase
APIs: Google Maps API for location services
System Architecture
The application is structured into three main layers:

User Layer: Handles user interactions and UI.
Application Layer: Manages business logic, authentication, and order processing.
Data Layer: Firebase is used for data storage and management.
Usage Instructions
Register/Login: Users can register or log in using their Google accounts.
Create an Order: Users can create a new order by specifying the items and limiting the number of participants.
Join an Order: Browse and join existing orders based on preferences and location.
Communicate: Use the chat feature to discuss order details with other participants.
Complete Transactions: Use external payment services to finalize purchases.
Known Issues
No In-App Payments: Payments are handled externally due to security considerations.
Limited UI Support: UI may not be fully optimized for all screen sizes.
Future Improvements
In-App Payment Integration: Potential future inclusion of a secure in-app payment gateway.
Enhanced Notifications: More refined and user-specific notifications.
