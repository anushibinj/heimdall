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

## Environment Configuration

The application can be configured using environment variables (see [.env.example](.env.example)):

| Variable | Description | Default |
| --- | --- | --- |
| `GOOGLE_OAUTH_CLIENT_ID` | Google OAuth2 Client ID | `sample-client-id-needs-to-be-changed` |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Google OAuth2 Client Secret | `sample-client-secret-needs-to-be-changed` |
| `HEIMDALL_FRONTEND_URL` | Frontend URL for OAuth redirects & logout | `http://localhost:5173` |
| `HEIMDALL_CORS_ALLOWED_ORIGINS` | Comma-separated CORS allowed origin patterns | `http://localhost:5173,http://127.0.0.1:5173` |
| `HEIMDALL_ADMIN_EMAIL` | Super admin user email address | `admin@example.com` |
| `HEIMDALL_ALLOWED_DOMAINS` | Allowed Google domains (comma-separated, empty allows all) | *(empty)* |
| `HEIMDALL_SECRET_KEY` | AES encryption key for stored database passwords | `ThisIsADefaultSecretKeyForDevEnv` |

