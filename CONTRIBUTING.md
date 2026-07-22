# Contributing

## Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Protected release branch |
| `develop` | Integration branch |
| `feature/*` | New feature work |
| `fix/*` | Non-critical bug fixes |
| `hotfix/*` | Urgent production fixes |
| `release/*` | Release stabilization |

Do not push directly to `main`.

## Commit Convention

Use concise conventional commits:

```text
feat: add QR login flow
fix: prevent duplicate active session
test: add wallet refund coverage
docs: update deployment guide
chore: configure docker compose
refactor: simplify order state validation
```

## Pull Request Template

```md
## Summary
- 

## Scope
- Backend:
- Frontend:
- Database migration:
- Documentation:

## API Contract Changes
- None, or describe exact endpoint/schema changes.

## Tests
- Backend:
- Frontend:
- Manual:

## Risks
- 

## Checklist
- [ ] No direct entity response from controllers.
- [ ] No business logic in controllers.
- [ ] No duplicate class/enum/DTO/repository/service/controller/config/migration.
- [ ] Existing API contract preserved or documented.
- [ ] New migration added only as a new version.
- [ ] No `.env`, secret, token, password, or build output committed.
- [ ] Swagger/Postman/frontend updated when API contract changes.
- [ ] Build and relevant tests pass.
```

## Review Checklist

- Requirements traced to SRS/Scope.
- Auth, RBAC, branch scope, IDOR, CORS/CSRF, WebSocket auth reviewed.
- Input validation and error mapping verified.
- Transactions and locks reviewed for race-prone flows.
- Pagination used for list APIs.
- No sensitive data in DTOs, logs, cache, PWA service worker, or documentation.
- Tests cover success, failure, boundary, permission, and state-machine cases.

## Repository Hygiene

- Never commit `.env`.
- Never commit real secrets, tokens, passwords, station credentials, webhook signatures, or card data.
- Never commit build output: `server/build`, `client/dist`, `coverage`, `node_modules`.
- Do not edit applied Flyway migrations; add a new version.
- Keep documentation synchronized with code and tests.
