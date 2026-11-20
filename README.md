# Лабораторная 5 — JWT и сессии

Продолжение RBPO-Lab4. Java 17 и PostgreSQL.

1. Выполните `.\setup-db.ps1`. Скрипт создаст БД `translations_lab5` и запишет случайный `JWT_SECRET` в `.env`.
2. Для входа редактором добавьте в `.env` строку `EDITOR_PASSWORD=ваш_пароль`. Пароль должен содержать 10+ символов, заглавную и строчную латинские буквы и цифру. Логин редактора — `editor`.
3. Запустите `.\mvnw.cmd spring-boot:run` и выполните `requests.postman_collection.json` в Postman сверху вниз.

Для Docker скопируйте `.env.example` в `.env`, задайте пароли БД и случайный JWT-секрет от 32 байт, затем запустите `docker compose up -d --wait`.

| POST | Тело JSON | Ответ |
|---|---|---|
| /auth/register | username, email, password, deviceId (опционально) | 201 |
| /auth/login | username, password, deviceId (опционально) | 200 |
| /auth/refresh | refreshToken | 200 |

Ответ содержит `accessToken` и `refreshToken`. Для `/api/**` передавайте access в `Authorization: Bearer ...`. Оба токена подписаны HS256. Сроки задаются `JWT_ACCESS_SECONDS` (900) и `JWT_REFRESH_SECONDS` (604800). Формат JWT описан в [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519.html).

После входа сессия получает ACTIVE. При обновлении старая становится USED, новая — ACTIVE. Повтор старого refresh даёт 401 и сохраняет REVOKED. После прохождения коллекции найдите последний логин `translator_…` и подставьте его в запрос:

```sql
SELECT status, access_token_expiry, refresh_token_expiry
FROM user_sessions
WHERE username = 'translator_…' AND device_id = 'demo';
```

Ожидаются две строки: REVOKED и ACTIVE. `.\mvnw.cmd test` запускает тесты на H2.
