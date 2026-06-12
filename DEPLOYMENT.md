# Deployment Notes

Recommended lightweight target: Render.

The project is ready for a Docker-backed Render web service with Render Postgres and Render Key Value. Keep all secrets in Render environment variables. Do not commit `.env`.

You can deploy either with `render.yaml` from the repository root or create the same services manually in the Render dashboard.

## Required Services

- Web service built from this repository's `Dockerfile`
- PostgreSQL database
- Render Key Value instance
- AWS S3 bucket for resume files
- AI provider API key compatible with the configured OpenAI/OpenRouter endpoint

## Required Environment Variables

```env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgresql://<user>:<password>@<host>:<port>/<database>
SPRING_DATA_REDIS_HOST=<redis-host>
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=
SPRING_DATA_REDIS_SSL_ENABLED=false
AWS_S3_BUCKET_NAME=<bucket-name>
AWS_REGION=ap-south-1
AWS_ACCESS_KEY_ID=<aws-access-key>
AWS_SECRET_ACCESS_KEY=<aws-secret-key>
OPENAI_API_KEY=<ai-provider-key>
OPENAI_API_URL=https://openrouter.ai/api/v1/chat/completions
OPENAI_MODEL=meta-llama/llama-3.3-70b-instruct:free
JWT_SECRET=<at-least-32-random-characters>
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
CORS_ALLOWED_ORIGINS=<frontend-origin>
APP_URL=https://<deployed-api-host>
SWAGGER_ENABLED=false
PORT=8080
```

## Deployment Checklist

1. Push the repository to GitHub.
2. In Render, create a Blueprint from this repository or create services manually.
3. If using the Blueprint, Render reads `render.yaml` and provisions:
   - Docker web service
   - PostgreSQL database
   - Key Value instance
4. Fill every `sync: false` secret prompted by Render.
5. After the first deploy, set `APP_URL` to the Render service URL.
6. Confirm `/actuator/health` returns a healthy response.
7. Temporarily enable Swagger only for a controlled demo if needed by setting `SWAGGER_ENABLED=true`.
8. Run the demo flow from `docs/demo-flow.http` or import the examples into Postman.

## Manual Render Setup

If you do not use the Blueprint:

1. Create a Render Postgres database.
2. Create a Render Key Value instance in the same region as the API.
3. Create a Web Service from this repository.
4. Select Docker as the runtime; the repository `Dockerfile` is already configured.
5. Set `DATABASE_URL` to the Render Postgres internal database URL.
6. Set Redis values from the Render Key Value internal URL:
   - `SPRING_DATA_REDIS_HOST`
   - `SPRING_DATA_REDIS_PORT`
   - `SPRING_DATA_REDIS_SSL_ENABLED=false`
7. Add all AWS, AI, JWT, CORS, and app URL variables listed above.

## Notes

- Flyway runs migrations automatically in the `prod` profile.
- Hibernate uses `ddl-auto: validate` in production, so schema changes must be added as migrations.
- The app accepts Render-style `DATABASE_URL` and converts it to a PostgreSQL JDBC URL at startup.
- The app also accepts `REDIS_URL` for manual setups, but `render.yaml` wires Redis host/port directly.
- Store real credentials only in the deployment platform, never in Git.
