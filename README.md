# YEY

React frontend + Java backend for a Computer Science library catalog. A Python
backend against the same API is planned for language comparison. The Java
backend is written object-oriented (encapsulation, inheritance, polymorphism,
interfaces, typed exceptions); see OOP Practices below for specifics.

## Structure

```text
backend/    Java REST API, plain JDK (no Maven, no third-party deps)
frontend/   React + Vite SPA
```

See [backend/README.md](backend/README.md) for the API contract and design notes.

## Requirements

- JDK 17+
- Node 18+

## Run

```bash
# terminal 1
cd backend && ./run.sh      # ./run.ps1 on Windows — serves http://localhost:8080

# terminal 2
cd frontend && npm install && npm run dev
```

Vite proxies `/api` to the backend (see `frontend/vite.config.js`); no CORS setup needed.

## Frontend commands

```bash
npm run dev
npm run build
npm run lint
npm run preview
```

## OOP Practices

| Concept | Where |
|---|---|
| Encapsulation | Private fields, public getters/setters on `LibraryItem`, `Book`, `Transaction` |
| Inheritance | `Book extends LibraryItem` (abstract base) |
| Polymorphism | `LibraryItem.getItemType()`/`toJson()` overridden by `Book`; `LibraryException.statusCode()` overridden per subclass |
| Interfaces | `Repository<T, ID>` (generic), implemented by `InMemoryBookRepository`, `InMemoryTransactionRepository` |
| Exception handling | Typed hierarchy (`LibraryException` → `BookNotFoundException`, `DuplicateIsbnException`, `BookNotAvailableException`, `ValidationException`, ...), each mapped to an HTTP status in the handler layer |
| Enums | `ItemStatus`, `TransactionStatus` model finite domain states instead of raw strings |
| Modular organization | Package-per-layer: `model/`, `repository/`, `service/`, `http/` |

Details and file paths: [backend/README.md](backend/README.md).

## Status

Working: catalog CRUD, search/filter, issue/return, overdue detection — all
served by the Java backend, in-memory storage.

Not done: Python comparative backend, persistent database, auth.
