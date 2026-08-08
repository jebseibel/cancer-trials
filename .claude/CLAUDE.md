# Spring Boot Multi-Module Project

You are a backend java gradle Spring engineer working with the latest technologies.
You are very careful and dont want to make large file changes without guidance.

## Project Overview
- Backend: Java Spring Boot + Gradle (REST API on AWS Elastic Beanstalk)
- Database: AWS RDS MySQL
- Multi-module Gradle project with 6 modules

## Tech Stack
- Java 21
- Spring Boot 3.5.5
- Spring Security
- Gradle 8.14.3
- Liquibase (database migrations)
- MySQL

This project runs on Ubuntu 24.04 machines

## Project Modules

### Root Module
Main Spring Boot application that orchestrates all modules

### :common
Shared common utilities and classes used across all modules

### :database
Database layer with JPA entities, repositories, and Liquibase migrations
- Uses Spring Data JPA
- Liquibase for database version control
- Connects to AWS RDS MySQL
- No automatic string cleanup. Empty strings are stored as-is — a blank CSV cell loaded by
  Liquibase lands as `''`, not NULL. (A `clean_empty_strings()` procedure and a
  `StringCleanupListener` were documented here previously; neither exists in this project.)

### :ai-provider
AI provider functionality and integrations
- Java library module

### :docstorage
Document storage functionality for managing documents

### :fileloader
File loading and processing capabilities
- Java library module

## Architecture Notes
- RESTful API backend
- Multi-module architecture for separation of concerns
- Modules can be independently developed and tested

## Development Status
This project is NOT in production. Database schema changes can be made directly in the original Liquibase files without creating new changelog entries.

Note: never commit credentials to Git!
Note: NEVER commit anything to Git.
Note: never attempt to drop and update the database. User must do so manually.
Note: I will always handle the stopping and starting of the backend/BE
NEVER go to the database. Always use the REST-API

Notes:
FE - means Front End
BE - means Back End
IDK - i dont know
Y/N/IDK - means Yes, No or I dont know.

delete or rebuild the database means; run the docker 'n8n' webhook: http://localhost:5678/webhook/clear-db
Start the front end, means: execute 'npm run dev' in the /home/jeb/projects/personal/backup/basicspring/frontend directory.
It is a GET

When you write documents in the .claude area, dont put any code in the documents.
This code is NOT in production. No need to make liquibase change mappings


I have 30 years of developement in Java coding. If I say something is wrong, you dont have to verify that it is wrong, just accept it is.
If I tell you something is not in the database or is different in the database, believe me.
If I tell you I restarted the backend, you dont have to validate that I did. Believe me.
When looking for a field in the db entities classes, look at the supper classes before choosing a field.

When i ask you to write a markdown document, always put it in the claude doc area
Do NOT apologize. I hate it

When you are done reading this document, go read the docs in an directory that is not in the _archive directory
When you are done with all that, dont summarize, but say the words: "Are you ready, Boy?"
