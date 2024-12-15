**How to Run the Project**
Clone the Repository:
git clone https://github.com/imalrathnalage/service-transit-calculator.git
cd service-transit-calculator

**Build and Run:**
Ensure Java 17+ is installed.
Use Maven to build and run:
mvn clean install
mvn spring-boot:run

**API Endpoints**
Upload CSV

URL: POST /transit/upload
Request: Form-data with a file named file.
Response: A success message.
Download CSV

URL: GET /transit/download
Response: A CSV file with processed trip data.

**Key Logs**
The application logs key activities such as:

Parsing errors in the uploaded file.
Warnings for missing fare mappings.
Details about processed trips.

**How to Use the Sample File**
Upload the File:

Location: src/main/resources/trips.csv

Start the application and use Postman or any REST client.
Send a POST request to http://localhost:9090/transit/upload with the trips.csv file attached as file.
Download Processed Results:

Send a GET request to http://localhost:9090/transit/download to download the processed results.

  

