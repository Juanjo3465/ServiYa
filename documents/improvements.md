# ServiYa - Improvements to meet Software Architecture Prototype 1 requirements

Based on the requirements in `documents/[SwArch_2025ii] - Project - Prototype 1.pdf` and the analysis of the current backend & frontend projects.

## Requirements from the PDF

- Distributed architecture
- At least one presentation-type component (a web front-end)
- At least two logic-type components
- At least two data-type components (a relational database and a NoSQL database)
- At least two different types of HTTP-based connectors
- Built using at least two different general-purpose programming languages
- Container-oriented deployment

## Current state vs requirements

| Requirement | Status | Notes |
|-------------|--------|-------|
| Web front-end (presentation) | Satisfied | React (Vite) in `frontend/serviya` |
| >= 2 programming languages | Satisfied | Java 21 (Spring Boot) + JavaScript/JSX (React) |
| Container-oriented deployment | Satisfied | `docker-compose.yml` runs backend, mysql, frontend with Dockerfiles |
| Two logic-type components | Not satisfied | Single Spring Boot monolith (`backend/serviya`) |
| Relational + NoSQL data components | Not satisfied | Only MySQL (`mysql/` scripts) |
| Two different HTTP-based connectors | Not satisfied | Only REST (Spring MVC) |
| Distributed architecture | Not satisfied | Monolithic backend |

## Proposed change: NoSQL for Profile Documents (user's idea)

The profile section must handle both client and offerer views and data.
Store profile data as a JSON-like document in a NoSQL store (MongoDB).

This maps perfectly to the domain: profiles vary by type, and a flexible
document (rather than a rigid relational join) fits client/offerer views.

Example document:

```json
{
  "userId": 1,
  "profileType": "OFFERER",
  "clientView": { },
  "offererView": {
    "whatsappNumber": "+573001112233",
    "specialty": "General Services",
    "publicDescription": "Experienced service provider..."
  },
  "addresses": [
    { "addressLine": "...", "city": "...", "latitude": ..., "longitude": ... }
  ]
}
```

Or store as separate documents per view (client_view, offerer_view).

## Improvement options

### Option A - Minimal changes (leverages the profile idea)

- Add **MongoDB** to `docker-compose.yml` + `.env`
- Add `spring-boot-starter-data-mongodb` to `pom.xml`
- Create a MongoDB `ProfileDocument` + `MongoRepository` + adapter in `profiles.infrastructure.mongo`
- Expose read/write of the profile snapshot from MongoDB (denormalized view),
  keep normalized MySQL for relations
- Add **WebSocket (STOMP)** for real-time notifications (different HTTP connector type)
- Keep monolith but document the logical split (Profiles, Requests/Services)
- Low risk; preserves existing JPA

### Option B - Stronger distributed split (best fit for "distributed architecture")

- Split backend into **two services**:
  - `core-service`: users, auth, services, requests (MySQL)
  - `profile-service`: profiles, addresses, offerer profiles (MongoDB)
- Inter-service REST over HTTP + frontend-to-service REST
- Add WebSocket/STOMP for real-time notifications in one service
- Two logic components, two data components, two HTTP connector types, distributed

### Option C - Profiles in MongoDB + GraphQL instead of WebSocket

- Same as A, but add **GraphQL** (`/graphql`) alongside REST as the second
  HTTP-based connector type
- No real-time needed; still satisfies "two different types of HTTP-based connectors"

## Connector notes

- REST (Spring MVC) -> first HTTP connector
- WebSocket (STOMP, HTTP upgrade) or GraphQL (over HTTP) -> second HTTP connector
- If splitting services, service-to-service REST also counts as HTTP connector

## Recommendation

- **Option B** best satisfies "distributed architecture" with two logic components,
  two data components, and two HTTP connector types.
- **Option A** is acceptable and lower risk if time is a concern; it still meets
  all stated requirements when documented well.

## Concrete implementation steps (Option A as starting point)

1. `docker-compose.yml`: add MongoDB service + env variables
2. Backend:
   - Add `spring-boot-starter-data-mongodb` dependency
   - Configure MongoDB connection in `application.yaml`
   - Create `ProfileDocument` (MongoDB @Document) + Mongo repository + adapter
3. Add WebSocket configuration + notification endpoint (STOMP over WS)
4. Create submission structure for the course:
   - Branch `prototype_1`
   - Folder `project/prototype_1/<teamX>/` (X in [1a, 1b, ..., 2a, 2b, ...])
   - `README.md` with: team info, software system (name/logo/description),
     Component-and-Connector (C&C) structure (view, styles, elements & relations),
     and prototype deployment instructions
   - Source code and configuration files
5. Update deployment instructions in the README

## Useful references (current files)

- `docker-compose.yml`, `.env`
- `backend/serviya/pom.xml`
- `backend/serviya/src/main/resources/application.yaml`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/domain/`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/entities/`
- `backend/serviya/src/main/java/com/parosurvivors/serviya/profiles/infrastructure/repositories/`