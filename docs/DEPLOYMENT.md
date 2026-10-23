# Render and Neon Deployment

This guide deploys the Dockerized API to Render and uses a Neon PostgreSQL
database. The public demo must contain synthetic data only.

## 1. Create the Neon database

1. Create a Neon project in a region close to Render's Frankfurt region.
2. Keep the default database or create a database named `antifraud`.
3. Open **Connect** and select the pooled connection.
4. Record the host, database, username, and password separately.

Render needs the JDBC form of the connection URL:

```text
jdbc:postgresql://YOUR-POOLER-HOST/YOUR-DATABASE?sslmode=require
```

Do not commit the username, password, or complete connection string.

## 2. Deploy the Render Blueprint

1. Merge the deployment configuration into `main`.
2. In Render, choose **New > Blueprint**.
3. Connect `adhambatlouni/anti-fraud-system` and select `render.yaml`.
4. Supply the three secret values requested by the Blueprint:

| Variable | Value |
|---|---|
| `DB_URL` | Neon JDBC URL shown above |
| `DB_USERNAME` | Neon database role |
| `DB_PASSWORD` | Neon role password |

The Blueprint activates the `prod` profile, binds Spring Boot to Render's
`PORT`, keeps API documentation disabled, and configures
`/actuator/health` as the health check. Automatic deployments wait for the
GitHub CI checks on `main` to pass.

Flyway applies migrations V1 through V5 during the first application startup.
Do not run migrations manually or edit an applied migration.

## 3. Verify the deployment

Replace `YOUR-SERVICE` with the assigned Render hostname:

```powershell
Invoke-RestMethod https://YOUR-SERVICE.onrender.com/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

Verify that production API documentation is unavailable:

```powershell
try {
    Invoke-WebRequest https://YOUR-SERVICE.onrender.com/v3/api-docs
} catch {
    $_.Exception.Response.StatusCode.value__
}
```

Then run the synthetic workflow from the README: register the administrator,
merchant, and support accounts; submit transactions; inspect decisions; and
submit feedback. Never use a real card number.

## 4. After verification

Add the verified service URL to the README. Do not add a live badge or URL
before the health check and synthetic workflow succeed.
