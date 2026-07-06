# Keen ERP

Unified inventory, warehouse, and shop POS system based on `keen.MD`.

## Included

- Spring Boot backend with JWT auth, RBAC, locations, products, inventory, stock requests, transfers, sales, notifications, and audit logs
- React frontend with role-based navigation, dashboard, inventory, operations, POS, reports, and audit views
- Docker files for backend, frontend, and PostgreSQL

## Secrets and bootstrap

Copy [`.env.example`](./.env.example) to `.env` and fill in real values before running Docker or production deployments.

The backend reads `JWT_SECRET` and the database credentials from the environment. Demo bootstrap data is disabled by default; enable `BOOTSTRAP_DEMO_DATA=true` only if you want sample records during development and provide `BOOTSTRAP_SAMPLE_PASSWORD`.

## Local run

Backend:

```powershell
cd backend
mvn spring-boot:run
```

The backend expects the root `.env` file or matching environment variables to be present before startup.

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

If you want the React app to read the shared root `.env`, keep `frontend/vite.config.js` as committed. Vite is configured to load env files from the repository root.

## Docker

```powershell
docker compose up --build
```

Backend runs on `http://localhost:8080`, frontend on `http://localhost:5173`, and PostgreSQL on `localhost:5432`.
