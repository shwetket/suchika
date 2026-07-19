Welcome to Suchika!
===================

To run the application, follow these simple steps:

1. Prerequisite: PostgreSQL
   You must have PostgreSQL installed on your computer. 
   If you do not, download it from https://www.postgresql.org/download/
   
   Once installed, create a new empty database named "app_db".
   (You can run the provided `setup-db.sql` file in pgAdmin or psql).

2. Configuration
   Open the `.env` file in this folder using Notepad.
   Fill in your PostgreSQL password:
   QUARKUS_DATASOURCE_PASSWORD=your_password

3. Start Suchika
   Double-click the `start.bat` file! 
   It will automatically install Java if you don't have it, launch the backend services in the background, and open the Suchika application in your web browser at http://localhost:8080.
