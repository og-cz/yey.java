# Library Management — Java Backend

A plain-JDK REST backend for the Computer Science library catalog. No Maven/Gradle,
no third-party libraries — just `javac`/`java` and the built-in `com.sun.net.httpserver`.

## OOP design, by file

| Concept | File(s) |
|---|---|
| Encapsulation | `model/LibraryItem.java`, `model/Book.java`, `model/Transaction.java` — private fields, public accessors |
| Inheritance | `model/Book.java extends model/LibraryItem.java` |
| Polymorphism | `LibraryItem.getItemType()` / `toJson()` overridden in `Book`; `exception/LibraryException.java#statusCode()` overridden by each subclass |
| Interfaces + generics | `repository/Repository.java` (`Repository<T, ID>`), implemented by `repository/InMemoryBookRepository.java`, `repository/InMemoryTransactionRepository.java` |
| Exception hierarchy | `exception/LibraryException.java` → `BookNotFoundException`, `DuplicateIsbnException`, `BookNotAvailableException`, `TransactionNotFoundException`, `TransactionAlreadyReturnedException`, `ValidationException` |
| Enums for domain state | `model/ItemStatus.java`, `model/TransactionStatus.java` |
| Modular layering | `model/` (domain) → `repository/` (storage) → `service/LibraryService.java` (business rules) → `http/` (REST handlers) |

## Requirements

- JDK 17+ (developed against JDK 21). No Maven, no internet access needed.

## Run

From PowerShell:

```powershell
./run.ps1
```

From bash / git-bash:

```bash
./run.sh
```

Both scripts compile everything under `src/` into `out/` and start the server on
`http://localhost:8080`. The frontend's Vite dev server proxies `/api` requests here
(see `frontend/vite.config.js`), so just start the backend and then `npm run dev`
in `frontend/`.

## API

| Method | Path                          | Description                                   |
|--------|-------------------------------|------------------------------------------------|
| GET    | `/api/books?search=&status=`  | List/search/filter the catalog                 |
| POST   | `/api/books`                  | Add a book                                      |
| PUT    | `/api/books/{id}`              | Update a book                                   |
| DELETE | `/api/books/{id}`              | Remove a book                                   |
| POST   | `/api/books/{id}/issue`        | Issue a copy to a member, body `{member, studentId}` |
| GET    | `/api/transactions/issued`    | Currently borrowed items (status is computed, so overdue items flip automatically) |
| GET    | `/api/transactions/returned`  | Closed loans                                    |
| POST   | `/api/transactions/{id}/return` | Mark a loan returned                          |

All data is in-memory and reseeded with a handful of sample titles each time the
server restarts.
