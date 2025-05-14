# Library Management App

In this project, a library magament app was develop that provides RESTful web service. Users can authenticate the library and login the system. Librarians can query and save books then everyone can see this books and Users can also barrow this books, returns them, give feedbacks about them. Also librarians can manage the books and users just like real library.

## Features

Users can create account. Can active their account with token sent via email.
Librarians can manage these users, show their details and update and delete their informations.
Librarians can create books and update these books.
Users can barrow books and return them after return process done librarians approve this process.
Librarians can see all users barrow and return history.
Users only can see their barrow and return history.
Everyone can give feedback about books and give notes about them between 1-5.
Librarians can create report for overdue books.

## Technologies Used

Spring Boot: Used for application development and providing RESTful services.
Spring Security: Provides authentication and authorization mechanism for securing the application.
JWT Tokne Authentication: Ensures secure communication between client and server.
Spring Data Jpa: Simplifies data access and persistence using the Java Persistence Api.
JSR-303 and Spring Validation: Enables valdidation of objects based on annotations.
OpenAPI and Swagger UI Documentation: Generates documentation for the API endpoints.
Docker: Facilitates containerization of the backend application, database and mail server.

# Installation

1. Clone the repository

git clone https://github.com/kato386/LibraryManagementSystem

2. Install and create the package for the JAR file.

mvn package -DskipTests

3. You can also do this step by using the MAVEN console.
   Disable the test during package creation by the 8th button on the top.
   Clean
   Package

4. Create docker images and run the application.
   docker-compose up --build

Once the application is succesfully started, you can access the application by navigation to http://localhost:8088

You can access the mail server application by navigation to
http://localhost:1080/#/

5. Access the API documentation using Swagger UI:
   Open a web browser and go to http://localhost:8088/swagger-ui/index.html.

## Postman Documentation
Set enviroments to local enviroments which is api_url = http://localhost - api_port = 8088

[<img src="https://run.pstmn.io/button.svg" alt="Run In Postman" style="width: 128px; height: 32px;">](https://app.getpostman.com/run-collection/25297446-41ab68a0-ebbe-4d00-80bc-d84e25d5040a?action=collection%2Ffork&source=rip_markdown&collection-url=entityId%3D25297446-41ab68a0-ebbe-4d00-80bc-d84e25d5040a%26entityType%3Dcollection%26workspaceId%3Da763cde3-7fc5-4b3f-9a93-0ced81f06590)
