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

To run the backend and local S3 storage simulator (SeaweedFS) via Docker, use the provided Docker compose configuration:

```bash
docker compose up -d
```

This starts:
- **SeaweedFS**: S3-compatible object storage on `http://localhost:8333` (master on `:9333`, filer on `:8888`).
- **Heimdall Backend**: Spring Boot backend on `http://localhost:8080` configured to use the SeaweedFS S3 endpoint.

## Storage Configuration (AWS S3 & SeaweedFS)

Heimdall supports storing database backups in any S3-compatible object storage (including AWS S3 and SeaweedFS locally).

- **Local Development**: Runs out-of-the-box with SeaweedFS (`http://localhost:8333`).
- **Production AWS S3**: Set `HEIMDALL_S3_ENDPOINT=` (empty or standard AWS endpoint), `HEIMDALL_S3_PATH_STYLE_ACCESS=false`, and provide valid `HEIMDALL_S3_ACCESS_KEY` & `HEIMDALL_S3_SECRET_KEY` credentials.

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
| `HEIMDALL_STORAGE_TYPE` | Storage type (`s3` or `local`) | `s3` |
| `HEIMDALL_S3_ENDPOINT` | S3 API endpoint URL (for SeaweedFS or custom S3) | `http://localhost:8333` |
| `HEIMDALL_S3_REGION` | S3 Region | `us-east-1` |
| `HEIMDALL_S3_BUCKET_NAME` | S3 Bucket name for backups | `heimdall-backups` |
| `HEIMDALL_S3_ACCESS_KEY` | S3 Access Key / AWS Access Key ID | `dummy-access-key` |
| `HEIMDALL_S3_SECRET_KEY` | S3 Secret Key / AWS Secret Access Key | `dummy-secret-key` |
| `HEIMDALL_S3_PATH_STYLE_ACCESS` | Use path-style bucket access (required for SeaweedFS/MinIO) | `true` |
| `HEIMDALL_S3_AUTO_CREATE_BUCKET` | Automatically create S3 bucket on startup if missing | `true` |


