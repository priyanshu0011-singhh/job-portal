# 🧩 Job Portal System

A **backend-focused Job Portal System** built using **Spring Boot and Spring Security (JWT)** that enables recruiters to publish job openings and candidates to apply with resumes.

This project demonstrates **secure REST API design, role-based access control, file handling, and layered backend architecture**, similar to what backend engineers implement in real-world job platforms.

---

# 📌 System Overview

The platform supports **three types of users**:

| Role | Description |
|-----|-------------|
| Candidate | Search jobs and submit applications |
| Recruiter | Create and manage job postings |
| Admin | Manage users and system roles |

### Core Capabilities

- Secure login using **JWT Authentication**
- Job posting and management
- Job application workflow
- Resume upload and download
- Keyword-based job search
- Role-based access control

---

# 🏗 Architecture Overview

The system follows a **layered backend architecture**, commonly used in Spring Boot applications.

Client  
↓  
Controllers (REST APIs)  
↓  
Service Layer (Business Logic)  
↓  
Repository Layer (Data Access)  
↓
H2 (local) / PostgreSQL (production)

---

## 📊 Architecture Diagram

            +-------------------+
            |      Client       |
            | (Postman / UI)    |
            +---------+---------+
                      |
                      v
            +-------------------+
            |  REST Controllers |
            |   Spring Boot     |
            +---------+---------+
                      |
                      v
            +-------------------+
            |   Service Layer   |
            |  Business Logic   |
            +---------+---------+
                      |
                      v
            +-------------------+
            | Repository Layer  |
            | Spring Data JPA   |
            +---------+---------+
                      |
                      v
            +-------------------+
            |  PostgreSQL / H2  |
            +-------------------+


           
---

# 🚀 Key Features

## 🔐 Secure Authentication

Authentication is implemented using **Spring Security with JWT tokens**.

### Authentication Flow

1. User logs in  
2. Server validates credentials  
3. JWT token is generated  
4. Client sends token with every request  

Header example:

Authorization: Bearer <JWT_TOKEN>


This allows **stateless and secure API communication**.

---

## 👥 Role-Based Access Control

The system supports three roles:

- Candidate
- Recruiter
- Admin

Authorization ensures only permitted users can perform certain actions.

| Role | Allowed Actions |
|-----|----------------|
| Candidate | Apply to jobs |
| Recruiter | Create & manage jobs |
| Admin | Manage users and roles |

---

## 💼 Job Management

Recruiters can manage job postings through REST APIs.

Features include:

- Create job listings
- Update job details
- Delete job postings
- View job information

---

## 📄 Job Applications

Candidates can apply for jobs and track applications.

Application records contain:

- User information
- Job reference
- Application status
- Resume file reference

Recruiters can update application status.

---

## 📂 Resume File Upload

Candidates can upload resumes during job applications.

Features:

- Resume upload
- Resume download
- File storage using Spring FileSystem

This demonstrates **handling multipart file uploads in Spring Boot APIs**.

---

## 🔎 Job Search

Jobs can be searched using keyword-based search.

Example endpoint:

GET /api/jobs/search/{keyword}


---

# 🧰 Tech Stack

| Category | Technology |
|--------|-------------|
| Backend | Java 17, Spring Boot 3 |
| Persistence | Spring Data JPA, Hibernate |
| Security | Spring Security, JWT |
| Database | H2 for local development, PostgreSQL for production |
| Migrations | Flyway |
| API Documentation | Swagger / OpenAPI |
| Build Tool | Maven |
| API Testing | Postman |
| Version Control | Git & GitHub |

---

# 📁 Project Structure

Job-Portal-System
│
├── controllers # REST API endpoints
├── service # Business logic layer
├── repository # Database access layer
├── entities # JPA entities
├── dto # Data transfer objects
├── security # JWT utilities and filters
├── configurations # Spring Security configuration
│
├── application.properties
├── data.sql
├── pom.xml
└── README.md


---

# 📡 API Endpoints Overview

## Authentication

| Method | Endpoint | Description |
|------|-----------|-------------|
| POST | /api/auth/login | User login and JWT generation |
| POST | /api/users/register | Register new user |

---

## User Management

| Method | Endpoint | Description |
|------|-----------|-------------|
| GET | /api/users/me | Get current user |
| PATCH | /api/users/{id}/roles | Update user roles (Admin only) |
| DELETE | /api/users/{id} | Delete user |

---

## Job Management

| Method | Endpoint | Description |
|------|-----------|-------------|
| POST | /api/jobs | Create job |
| PUT | /api/jobs/{id} | Update job |
| GET | /api/jobs/{id} | Get job by ID |
| GET | /api/jobs/search/{keyword} | Search jobs |
| DELETE | /api/jobs/{id} | Delete job |

---

## Applications

| Method | Endpoint | Description |
|------|-----------|-------------|
| POST | /api/applications | Apply for job |
| PUT | /api/applications/{id} | Update application status |
| GET | /api/applications/user/{id} | Applications by user |
| GET | /api/applications/job/{id} | Applications for job |
| DELETE | /api/applications/{id} | Delete application |

---

## Resume Management

Upload Resume

POST /api/files/upload-resume

Form Data

file: <resume file>

Download Resume

GET /api/files/resumes/{filename}


---

# ⚙️ Running the Project Locally

### Clone Repository

git clone https://github.com/NikStack20/Job-Portal-System.git

cd Job-Portal-System


---

### Run Application

Run from the directory containing `pom.xml`. Local development uses an in-memory H2 database and applies the schema through Flyway:

```powershell
.\mvnw.cmd spring-boot:run
```

The local database resets when the app stops. Swagger is available at `/swagger-ui/index.html`.

Application will start at
http://localhost:8099

---

### Production Deployment

The Docker Compose setup runs PostgreSQL and the API with persistent database/resume volumes. The API binds to localhost only; the optional Caddy edge profile publishes HTTPS and obtains certificates automatically for a real DNS name.

### Free Render Demo

The repository includes a Render Blueprint (`render.yaml`) for a free Docker web service. It uses the `render` Spring profile, an automatically generated JWT secret, and in-memory H2 rather than a billable managed database. The service may sleep when idle, and its database and uploaded resumes are temporary: they can disappear after a restart, redeploy, or sleep cycle. Use this only as a recruiter demo with synthetic data, not for real users or production records. Do not upgrade the service or add paid resources if a zero-cost deployment is required.

The public root URL serves a recruiter-facing project landing page. The interactive API documentation is at `/swagger-ui/index.html`.

Create a local environment file. The script prompts for the first admin details and generates random database/JWT secrets; it stores the file under `%LOCALAPPDATA%\JobPortal`, outside this OneDrive-synced project, and restricts its Windows ACL to your account and SYSTEM:

```powershell
.\scripts\New-ProductionEnv.ps1
$envFile = Join-Path $env:LOCALAPPDATA 'JobPortal\.env.production'
docker compose --env-file $envFile up --build -d
```

For public HTTPS, point a DNS name at the server, set `DOMAIN` in `.env.production`, allow inbound ports 80 and 443, then start the edge profile:

```powershell
docker compose --env-file $envFile --profile edge up --build -d
```

The app is still reachable locally at `http://localhost:8099/`; Swagger is disabled in production. After confirming the initial admin can log in, set `ADMIN_BOOTSTRAP_ENABLED=false` and clear `ADMIN_PASSWORD` in `%LOCALAPPDATA%\JobPortal\.env.production`, then recreate the app container. Keep the database and resume volumes in durable storage. Create a PostgreSQL dump with `JOBPORTAL_ENV_FILE=/secure/path/.env.production bash ./scripts/backup-postgres.sh`; schedule it daily on the host, copy dumps to encrypted off-host storage, set a retention policy, and test restores before real users depend on the service.

The `prod` profile uses PostgreSQL, applies Flyway migrations, validates the schema, requires a JWT secret, and disables the H2 console and Swagger. For managed hosting, inject variables through its secret manager instead of copying `.env.production`.

Required environment variables:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Set to `prod` |
| `DB_URL` | PostgreSQL JDBC URL, for example `jdbc:postgresql://db-host:5432/jobportal` |
| `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL credentials |
| `JWT_SECRET` | Base64-encoded 64 random bytes or longer |
| `APP_UPLOAD_DIR` | Persistent directory/volume for uploaded resumes |

The Compose environment file supplies the required variables: `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `APP_UPLOAD_DIR`. Admin bootstrap is opt-in and is enabled by the setup script only for the first startup. Public registration always creates candidate accounts; admins can promote recruiter accounts.

The first migration expects an empty PostgreSQL schema. Back up and plan a separate migration before pointing it at any database that already contains job-portal data. Configure backups, monitoring, and recovery in the selected hosting environment before production traffic.


---

# 🧪 API Testing

APIs can be tested using **Postman**.

Steps:

1. Login to obtain JWT token
2. Add token in request header

Authorization: Bearer <JWT_TOKEN>

---

# 📸 Demo

Project demo screenshots and test results are available in:

/demo

This folder contains:

- Postman API tests
- Entity screenshots
- API execution examples

---

# 🔮 Future Improvements

- Integrate React frontend
- Add email notifications
- Add a user-facing frontend
- Deploy to a selected cloud platform
- Add pagination and advanced search filtering

---

# 👨‍💻 Author

**Nikhil Chauhan**

B.Tech Student | Backend Developer

Email  
codingnik20@gmail.com

GitHub  
https://github.com/NikStack20

END OF README.md
