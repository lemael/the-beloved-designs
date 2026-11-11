# The Beloved Designs

Site e-commerce mode & beauté (Cameroun). Monorepo :

- `backend/` — API REST Spring Boot 4 (Java 21), PostgreSQL (Railway en prod), JWT.
- `frontend/` — Vue 3 + TypeScript, Pinia, Vue Router.

## Démarrer en local

```bash
docker compose up --build
```

- Backend : http://localhost:8080 (docs : http://localhost:8080/swagger-ui.html)
- Frontend : http://localhost:5173
- PostgreSQL : localhost:5432 (db `beloved_designs`, user/password `postgres`)

## Développement sans Docker

```bash
# Backend
cd backend && ./mvnw spring-boot:run

# Frontend
cd frontend && npm install --legacy-peer-deps && npm run dev
```

## Documentation

Le contexte métier, l'architecture détaillée et les conventions de chaque domaine sont maintenus dans le dépôt `ai-memory` (séparé de ce dépôt de code).
