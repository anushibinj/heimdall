# Heimdall

Heimdall is a database backup and restore management application.

## Prerequisites

Before setting up the application, ensure you have the following installed on your machine:

- **Java 17**: Required for the Spring Boot backend.
- **Node.js**: Required for the React frontend.
- **pnpm**: Package manager for the frontend (`npm install -g pnpm`).
- **Docker**: (Optional) For containerized execution.

### Native Host Binaries (`pg_dump`)

The application requires `pg_dump` to perform PostgreSQL backups. You must have PostgreSQL client tools installed on your host machine to run the application locally.

**macOS (via Homebrew):**
```bash
brew install postgresql
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql-client
```

**Windows:**
Download the PostgreSQL installer from the [official website](https://www.postgresql.org/download/windows/) and ensure the `bin` directory is added to your system's PATH.

## Local Development Setup

### 1. Backend Setup

The backend is a Spring Boot application built with Gradle.

1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Run the application using the Gradle wrapper:
   ```bash
   ./gradlew bootRun
   ```

The backend server will start on `http://localhost:8080`.

### 2. Frontend Setup

The frontend is a React application built with Vite and TypeScript.

1. Navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```
2. Install the dependencies using `pnpm`:
   ```bash
   pnpm install
   ```
3. Start the development server:
   ```bash
   pnpm run dev
   ```

The frontend will be available at `http://localhost:5173`.

## Docker Setup

To run the backend via Docker (which includes the necessary `pg_dump` tools within the container), you can use the provided Docker compose configuration:

```bash
docker compose up -d
```
