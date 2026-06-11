# java-suanming

Spring Boot backend for the `vue-suanming` AI assistant. It validates a fixed login, accepts the frontend bazi payload, and proxies DeepSeek V4 chat requests without exposing the API key to the browser.

## Modules

- `sjfy-common`: shared response, exception, and CORS support
- `sjfy-service`: auth session and DeepSeek chat service
- `sjfy-app`: Spring Boot app and HTTP controllers

## Local Run

```bash
export SUANMING_AUTH_PASSWORD='your-login-password'
export DEEPSEEK_API_KEY='your-deepseek-api-key'

mvn -q clean package -DskipTests
java -jar sjfy-app/target/sjfy-app-1.0-SNAPSHOT.jar
```

Default API base:

```text
http://localhost:8092/api
```

## Main APIs

- `POST /api/auth/login`
- `GET /api/auth/session`
- `POST /api/ai/chat`

The chat API supports:

- `model`: `deepseek-v4-pro` or `deepseek-v4-flash`
- `thinkingEnabled`: `true` or `false`
- `reasoningEffort`: `high` or `max`

## Production Notes

Do not commit real API keys. Configure them through environment variables:

```bash
export SUANMING_AUTH_USERNAME='小新'
export SUANMING_AUTH_PASSWORD='your-login-password'
export DEEPSEEK_API_KEY='your-deepseek-api-key'
export DEEPSEEK_MODEL='deepseek-v4-pro'
export SUANMING_PORT=8092
```
