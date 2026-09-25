import { useState, useEffect } from "react";
import { DateTime } from 'luxon';
import "./App.css";
import yayLogo from "./assets/yay.png";

const API_BASE = "/api";
const pageSize = 10;
const TRANSACTION_STATUSES = ["On loan", "Overdue", "Available"];

// Talks to the Java backend (see /backend). Throws with the server's error
// message on non-2xx responses so callers can surface it directly.
async function apiFetch(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || `Request failed (${response.status})`);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

function formatDate(isoDate) {
  if (!isoDate) return "—";
  return DateTime.fromISO(isoDate).toFormat("dd LLL yyyy");
}

const emptyBookForm = {
  title: "",
  author: "",
  isbn: "",
  publisher: "",
  category: "",
  yearPublished: "",
  description: "",
};

function App() {
  const [activePage, setActivePage] = useState("manage");
  const [books, setBooks] = useState([]);
  const [booksError, setBooksError] = useState("");
  const [selectedBookId, setSelectedBookId] = useState(null);
  const [searchText, setSearchText] = useState("");
  const [catalogStatus, setCatalogStatus] = useState("all");
  const [showBookForm, setShowBookForm] = useState(false);
  const [newBook, setNewBook] = useState(emptyBookForm);
  const [editBook, setEditBook] = useState(null);
  const [issueTarget, setIssueTarget] = useState(null);
  const [issueForm, setIssueForm] = useState({ member: "", studentId: "" });
  const [catalogPage, setCatalogPage] = useState(1);
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    apiFetch("/books/categories").then(setCategories).catch(() => {});
  }, []);

  const loadBooks = async () => {
    setBooksError("");
    try {
      const params = new URLSearchParams();
      if (searchText) params.set("search", searchText);
      if (catalogStatus !== "all") params.set("status", catalogStatus);
      const query = params.toString();
      const data = await apiFetch(`/books${query ? `?${query}` : ""}`);
      setBooks(data);
      setSelectedBookId((currentId) =>
        data.some((book) => book.id === currentId) ? currentId : data[0]?.id ?? null,
      );
    } catch (err) {
      setBooksError(err.message);
    }
  };

  useEffect(() => {
    const timeout = setTimeout(loadBooks, 250);
    return () => clearTimeout(timeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchText, catalogStatus]);

  const selectedBook = books.find((book) => book.id === selectedBookId) || null;
  const catalogPageCount = Math.max(1, Math.ceil(books.length / pageSize));
  const visibleBooks = books.slice(
    (catalogPage - 1) * pageSize,
    catalogPage * pageSize,
  );

  const handleCreateBook = () => setShowBookForm(true);
  const handleCloseForm = () => setShowBookForm(false);
  const handleSaveNewBook = async (event) => {
    event.preventDefault();
    try {
      const created = await apiFetch("/books", {
        method: "POST",
        body: JSON.stringify({
          ...newBook,
          yearPublished: Number(newBook.yearPublished) || 0,
        }),
      });
      setSelectedBookId(created.id);
      setShowBookForm(false);
      setNewBook(emptyBookForm);
      await loadBooks();
    } catch (err) {
      window.alert(err.message);
    }
  };
  const handleEditBook = () => setEditBook({ ...selectedBook });
  const handleCloseEditForm = () => setEditBook(null);
  const handleSaveBook = async (event) => {
    event.preventDefault();
    try {
      await apiFetch(`/books/${editBook.id}`, {
        method: "PUT",
        body: JSON.stringify({
          ...editBook,
          yearPublished: Number(editBook.yearPublished) || 0,
        }),
      });
      handleCloseEditForm();
      await loadBooks();
    } catch (err) {
      window.alert(err.message);
    }
  };
  const handleIssueBook = (book) => {
    setIssueTarget(book);
    setIssueForm({ member: "", studentId: "" });
  };
  const handleCloseIssueForm = () => setIssueTarget(null);
  const handleSubmitIssue = async (event) => {
    event.preventDefault();
    try {
      await apiFetch(`/books/${issueTarget.id}/issue`, {
        method: "POST",
        body: JSON.stringify(issueForm),
      });
      setIssueTarget(null);
      await loadBooks();
    } catch (err) {
      window.alert(err.message);
    }
  };
  const handleDeleteBook = async () => {
    if (!selectedBook) return;
    if (!window.confirm(`Delete "${selectedBook.title}"? This cannot be undone.`)) {
      return;
    }
    try {
      await apiFetch(`/books/${selectedBook.id}`, { method: "DELETE" });
      await loadBooks();
    } catch (err) {
      window.alert(err.message);
    }
  };
  const handleReturnedBookDetails = (title) => {
    setCatalogPage(1);
    setSearchText(title);
    setActivePage("manage");
  };

  return (
    <div className="app-shell">
      {/* Top navigation and account area */}
      <header className="topbar">
        <div className="brand-name">LIBRARY MANAGEMENT SYSTEM</div>
        <img className="library-mark" src={yayLogo} alt="Library logo" />
        <TimeZoneDisplay />
      </header>
      <div className="workspace">
        {/* Main page navigation */}
        <aside className="sidebar">
          <p className="sidebar-label">MANAGEMENT</p>
          <button
            className={`nav-item ${activePage === "manage" ? "active" : ""}`}
            type="button"
            onClick={() => setActivePage("manage")}
          >
            <span className="nav-icon">x</span> Manage Library
          </button>
          <button
            className={`nav-item ${activePage === "issued" ? "active" : ""}`}
            type="button"
            onClick={() => setActivePage("issued")}
          >
            <span className="nav-icon">□</span> Issued Books
          </button>
          <button
            className={`nav-item ${activePage === "returned" ? "active" : ""}`}
            type="button"
            onClick={() => setActivePage("returned")}
          >
            <span className="nav-icon">✓</span> Returned Books
          </button>
        </aside>
        <main className="main-content">
          {activePage === "manage" ? (
            <>
              <section className="page-heading">
                <div>
                  <h1>Manage Library</h1>
                  <p>
                    Create, read, update, and delete book records in one
                    workspace while keeping the library catalog organized and up
                    to date.
                  </p>
                </div>
                <div className="heading-actions">
                  <button
                    className="primary-button"
                    type="button"
                    onClick={handleCreateBook}
                  >
                    + &nbsp; Add Book
                  </button>
                </div>
              </section>
              <section className="toolbar">
                <label className="search-box">
                  <span>⌕</span>
                  <input
                    value={searchText}
                    onChange={(event) => {
                      setCatalogPage(1);
                      setSearchText(event.target.value);
                    }}
                    placeholder="Search books, author, or ISBN"
                  />
                </label>
                <select
                  className="filter-select"
                  value={catalogStatus}
                  onChange={(event) => {
                    setCatalogPage(1);
                    setCatalogStatus(event.target.value);
                  }}
                  aria-label="Filter books by status"
                >
                  <option value="all">All statuses</option>
                  <option value="Available">Available</option>
                  <option value="On loan">On loan</option>
                </select>
              </section>
              {booksError && <p className="form-error">{booksError}</p>}
              <section className="catalog-layout">
                <div className="catalog-panel panel">
                  <div className="panel-heading">
                    <div>
                      <h2>Book Catalog</h2>
                      <p>Browse your collection</p>
                    </div>
                  </div>
                  <div className="catalog-table table-scroll">
                    <div className="table-row table-header">
                      <span>BOOK</span>
                      <span>AUTHOR</span>
                      <span>STATUS</span>
                      <span></span>
                    </div>
                    {visibleBooks.map((book) => (
                      <button
                        className={`table-row book-row ${selectedBookId === book.id ? "selected" : ""}`}
                        type="button"
                        key={book.id}
                        onClick={() => setSelectedBookId(book.id)}
                      >
                        <span>
                          <strong>{book.title}</strong>
                          <small>{book.isbn}</small>
                        </span>
                        <span>{book.author}</span>
                        <span>
                          <em
                            className={`status ${book.status === "On loan" ? "loan" : ""}`}
                          >
                            ● {book.status}
                          </em>
                        </span>
                        <span>
                          <button
                            className={`table-view issue-book-button ${book.status === "Available" ? "available" : "unavailable"}`}
                            type="button"
                            disabled={book.status !== "Available"}
                            onClick={(event) => {
                              event.stopPropagation();
                              setSelectedBookId(book.id);
                              handleIssueBook(book);
                            }}
                          >
                            Issue book
                          </button>
                        </span>
                      </button>
                    ))}
                  </div>
                  <Pagination
                    currentPage={catalogPage}
                    pageCount={catalogPageCount}
                    totalItems={books.length}
                    onPageChange={setCatalogPage}
                  />
                </div>
                {selectedBook ? (
                  <BookDetails
                    book={selectedBook}
                    onEdit={handleEditBook}
                    onDelete={handleDeleteBook}
                  />
                ) : (
                  <aside className="details-panel panel">
                    <div className="panel-heading">
                      <div>
                        <h2>Record details</h2>
                        <p>No book selected.</p>
                      </div>
                    </div>
                  </aside>
                )}
              </section>
            </>
          ) : activePage === "issued" ? (
            <IssuedBooks
              searchText={searchText}
              setSearchText={setSearchText}
            />
          ) : (
            <ReturnedBooks
              searchText={searchText}
              setSearchText={setSearchText}
              onViewDetails={handleReturnedBookDetails}
            />
          )}
        </main>
      </div>
      {showBookForm && (
        <div
          className="modal-backdrop"
          role="presentation"
          onClick={handleCloseForm}
        >
          <form
            className="modal edit-modal"
            role="dialog"
            aria-modal="true"
            onClick={(event) => event.stopPropagation()}
            onSubmit={handleSaveNewBook}
          >
            <h2>Create a book</h2>
            <p>Add the details for the new book record.</p>
            <label>
              Title <span className="required-mark">*</span>
              <input
                required
                value={newBook.title}
                onChange={(event) => setNewBook({ ...newBook, title: event.target.value })}
              />
            </label>
            <label>
              Author <span className="required-mark">*</span>
              <input
                required
                value={newBook.author}
                onChange={(event) => setNewBook({ ...newBook, author: event.target.value })}
              />
            </label>
            <label>
              ISBN
              <input
                value={newBook.isbn}
                onChange={(event) => setNewBook({ ...newBook, isbn: event.target.value })}
              />
            </label>
            <label>
              Publisher
              <input
                value={newBook.publisher}
                onChange={(event) => setNewBook({ ...newBook, publisher: event.target.value })}
              />
            </label>
            <label>
              Category <span className="required-mark">*</span>
              <select
                required
                value={newBook.category}
                onChange={(event) => setNewBook({ ...newBook, category: event.target.value })}
              >
                <option value="" disabled>Select a Computer Science subject area</option>
                {categories.map((category) => (
                  <option value={category} key={category}>{category}</option>
                ))}
              </select>
            </label>
            <label>
              Year published
              <input
                type="number"
                value={newBook.yearPublished}
                onChange={(event) => setNewBook({ ...newBook, yearPublished: event.target.value })}
              />
            </label>
            <label>
              Description
              <textarea
                value={newBook.description}
                onChange={(event) => setNewBook({ ...newBook, description: event.target.value })}
              />
            </label>
            <div>
              <button
                className="secondary-button"
                type="button"
                onClick={handleCloseForm}
              >
                Cancel
              </button>
              <button
                className="primary-button"
                type="submit"
              >
                Add book
              </button>
            </div>
          </form>
        </div>
      )}
      {editBook && (
        <div
          className="modal-backdrop"
          role="presentation"
          onClick={handleCloseEditForm}
        >
          <form
            className="modal edit-modal"
            role="dialog"
            aria-modal="true"
            onClick={(event) => event.stopPropagation()}
            onSubmit={handleSaveBook}
          >
            <h2>Edit book</h2>
            <p>Replace the current book values, then save the record.</p>
            <label>
              Title
              <input
                value={editBook.title}
                onChange={(event) => setEditBook({ ...editBook, title: event.target.value })}
              />
            </label>
            <label>
              Author
              <input
                value={editBook.author}
                onChange={(event) => setEditBook({ ...editBook, author: event.target.value })}
              />
            </label>
            <label>
              ISBN
              <input
                value={editBook.isbn}
                onChange={(event) => setEditBook({ ...editBook, isbn: event.target.value })}
              />
            </label>
            <label>
              Publisher
              <input
                value={editBook.publisher}
                onChange={(event) => setEditBook({ ...editBook, publisher: event.target.value })}
              />
            </label>
            <label>
              Category <span className="required-mark">*</span>
              <select
                required
                value={editBook.category}
                onChange={(event) => setEditBook({ ...editBook, category: event.target.value })}
              >
                <option value="" disabled>Select a Computer Science subject area</option>
                {categories.map((category) => (
                  <option value={category} key={category}>{category}</option>
                ))}
              </select>
            </label>
            <label>
              Year published
              <input
                type="number"
                value={editBook.yearPublished}
                onChange={(event) => setEditBook({ ...editBook, yearPublished: event.target.value })}
              />
            </label>
            <label>
              Status
              <select
                value={editBook.status}
                onChange={(event) => setEditBook({ ...editBook, status: event.target.value })}
              >
                <option value="Available">Available</option>
                <option value="On loan">On loan</option>
              </select>
            </label>
            <label>
              Description
              <textarea
                value={editBook.description}
                onChange={(event) => setEditBook({ ...editBook, description: event.target.value })}
              />
            </label>
            <div>
              <button className="secondary-button" type="button" onClick={handleCloseEditForm}>
                Cancel
              </button>
              <button className="primary-button" type="submit">
                Save changes
              </button>
            </div>
          </form>
        </div>
      )}
      {issueTarget && (
        <div
          className="modal-backdrop"
          role="presentation"
          onClick={handleCloseIssueForm}
        >
          <form
            className="modal edit-modal"
            role="dialog"
            aria-modal="true"
            onClick={(event) => event.stopPropagation()}
            onSubmit={handleSubmitIssue}
          >
            <h2>Issue book</h2>
            <p>Record who is borrowing &ldquo;{issueTarget.title}&rdquo;.</p>
            <label>
              Member name <span className="required-mark">*</span>
              <input
                required
                value={issueForm.member}
                onChange={(event) => setIssueForm({ ...issueForm, member: event.target.value })}
              />
            </label>
            <label>
              Student ID <span className="required-mark">*</span>
              <input
                required
                value={issueForm.studentId}
                onChange={(event) => setIssueForm({ ...issueForm, studentId: event.target.value })}
              />
            </label>
            <div>
              <button className="secondary-button" type="button" onClick={handleCloseIssueForm}>
                Cancel
              </button>
              <button className="primary-button" type="submit">
                Issue book
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

// Date Time for Header
function TimeZoneDisplay() {
    const [phTime, setPhTime] = useState(() =>
      DateTime.now().setZone('Asia/Manila').toFormat('hh:mm:ss a'),
    );
    const [phDate, setPhDate] = useState(() =>
      DateTime.now().setZone('Asia/Manila').toFormat('MM-dd-yyyy'),
    );


    useEffect(() => {
      const timer = setInterval (() => {
        const now = DateTime.now().setZone('Asia/Manila');
        setPhTime(now.toFormat('hh:mm:ss a'));
        setPhDate(now.toFormat('MM-dd-yyyy'));
      }, 1000);

    return () => clearInterval(timer);
    }, []);

  return (
    <div className="timezone-display">
      <span className="timezone-offset">GMT+8</span>
      <span className="timezone-values">
        <span className="timezone-time">{phTime}</span>
        <span className="timezone-date">{phDate}</span>
      </span>
    </div>
  );
}

// Selected record details and future CRUD actions.
function BookDetails({ book, onEdit, onDelete }) {
  return (
    <aside className="details-panel panel">
      <div className="panel-heading">
        <div>
          <h2>Record details</h2>
          <p>Review the selected book record.</p>
        </div>
      </div>
      <div className="book-preview">
        <div className="book-cover">BK</div>
        <div>
          <h3>{book.title}</h3>
          <p>{book.author}</p>
          <em className={`status ${book.status === "On loan" ? "loan":""}`}>● {book.status}</em>
          <small>ISBN {book.isbn}</small>
        </div>
      </div>
      <h3 className="metadata-heading">Metadata</h3>
      <div className="metadata-grid">
        {[
          ["Title", "title"],
          ["Author", "author"],
          ["ISBN", "isbn"],
          ["Publisher", "publisher"],
          ["Category", "category"],
          ["Year Published", "yearPublished"],
        ].map(([label, field]) => (
            <label key={label}>
              {label}
              <input value={book[field]} readOnly />
            </label>
        ))}
      </div>
      <label className="description-field">
        Description
        <textarea value={book.description} readOnly />
      </label>
      <div className="detail-actions">
        <button className="primary-button" type="button" onClick={onEdit}>
          ✎ &nbsp; Edit
        </button>
        <button className="delete-button" type="button" onClick={onDelete}>
          ▢ &nbsp; Delete
        </button>
      </div>
    </aside>
  );
}

function isDateInRange(isoDate, dateRange) {
  return (
    (!dateRange.start || isoDate >= dateRange.start) &&
    (!dateRange.end || isoDate <= dateRange.end)
  );
}

function DateRangeFilter({ dateRange, onChange, label }) {
  const [isOpen, setIsOpen] = useState(false);
  const rangeLabel = "🗓";

  return (
    <div className="date-range-filter">
      <button
        className="filter-select date-range-trigger"
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
        aria-label={label}
      >
        {rangeLabel}
      </button>
      {isOpen && (
        <div className="date-range-popup" role="dialog" aria-label={label}>
          <label>
            Start date
            <input
              type="date"
              value={dateRange.start}
              max={dateRange.end || undefined}
              onChange={(event) => onChange({ ...dateRange, start: event.target.value })}
            />
          </label>
          <label>
            End date
            <input
              type="date"
              value={dateRange.end}
              min={dateRange.start || undefined}
              onChange={(event) => onChange({ ...dateRange, end: event.target.value })}
            />
          </label>
        </div>
      )}
    </div>
  );
}

// Issued book records mirror the second reference screen.
function IssuedBooks({ searchText, setSearchText }) {
  const [currentPage, setCurrentPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState("all");
  const [dateRange, setDateRange] = useState({ start: "", end: "" });
  const [issuedBooks, setIssuedBooks] = useState([]);
  const [error, setError] = useState("");

  const loadIssuedBooks = async () => {
    setError("");
    try {
      setIssuedBooks(await apiFetch("/transactions/issued"));
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    (async () => {
      await loadIssuedBooks();
    })();
  }, []);

  const handleMarkAsReturned = async (transactionId) => {
    try {
      await apiFetch(`/transactions/${transactionId}/return`, { method: "POST" });
      await loadIssuedBooks();
    } catch (err) {
      window.alert(err.message);
    }
  };

  const filteredIssuedBooks = issuedBooks.filter((book) =>
    `${book.title} ${book.member} ${book.studentId}`
      .toLowerCase()
      .includes(searchText.toLowerCase()) &&
    (statusFilter === "all" || book.status === statusFilter) &&
    isDateInRange(book.issuedOn, dateRange),
  );
  const pageCount = Math.max(1, Math.ceil(filteredIssuedBooks.length / pageSize));
  const visibleIssuedBooks = filteredIssuedBooks.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  return (
    <section className="issued-page">
      <section className="page-heading">
        <div>
          <h1>Issued Books</h1>
          <p>Track books currently on loan and their return dates.</p>
        </div>
      </section>
      <div className="filter-bar">
        <label className="search-box issued-search">
          <span>⌕</span>
          <input
            value={searchText}
            onChange={(event) => {
              setCurrentPage(1);
              setSearchText(event.target.value);
            }}
            placeholder="Search book, member, or ID"
          />
        </label>
        <select
          className="filter-select"
          value={statusFilter}
          onChange={(event) => {
            setCurrentPage(1);
            setStatusFilter(event.target.value);
          }}
          aria-label="Filter issued books by status"
        >
          <option value="all">All statuses</option>
          {TRANSACTION_STATUSES.map((status) => (
            <option value={status} key={status}>{status}</option>
          ))}
        </select>
        <DateRangeFilter
          dateRange={dateRange}
          onChange={(range) => {
            setCurrentPage(1);
            setDateRange(range);
          }}
          label="Filter issued books by date range"
        />
      </div>
      {error && <p className="form-error">{error}</p>}
      <div className="issued-table panel">
        <div className="table-row table-header">
          <span>BOOK</span>
          <span>MEMBER</span>
          <span>STUDENT ID</span>
          <span>ISSUED ON</span>
          <span>DUE DATE</span>
          <span>STATUS</span>

        </div>
        {visibleIssuedBooks.map((book) => (
          <div className="table-row issued-row" key={book.id}>
            <span>
              <strong>{book.title}</strong>
              <small>{book.author}</small>
            </span>
            <span>{book.member}</span>
            <span>{book.studentId}</span>
            <span>{formatDate(book.issuedOn)}</span>
            <span>{formatDate(book.dueDate)}</span>
            <span>
              <em
                className={`status ${book.status === "Overdue" ? "overdue" : ""}`}
              >
                ● {book.status}
              </em>
            </span>
            <span>
              <button
                className="secondary-button"
                type="button"
                onClick={() => handleMarkAsReturned(book.id)}
              >
                ✓ Mark As Returned
              </button>
            </span>
          </div>
        ))}
        <Pagination
          currentPage={currentPage}
          pageCount={pageCount}
          totalItems={filteredIssuedBooks.length}
          onPageChange={setCurrentPage}
        />
      </div>
    </section>
  );
}

// Returned book records use the issued-book layout with return-specific fields.
function ReturnedBooks({ searchText, setSearchText, onViewDetails }) {
  const [currentPage, setCurrentPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState("all");
  const [dateRange, setDateRange] = useState({ start: "", end: "" });
  const [returnedBooks, setReturnedBooks] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      setError("");
      try {
        setReturnedBooks(await apiFetch("/transactions/returned"));
      } catch (err) {
        setError(err.message);
      }
    })();
  }, []);

  const filteredReturnedBooks = returnedBooks.filter((book) =>
    `${book.title} ${book.member} ${book.studentId}`
      .toLowerCase()
      .includes(searchText.toLowerCase()) &&
    (statusFilter === "all" || book.status === statusFilter) &&
    isDateInRange(book.returnedOn, dateRange),
  );
  const pageCount = Math.max(1, Math.ceil(filteredReturnedBooks.length / pageSize));
  const visibleReturnedBooks = filteredReturnedBooks.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  return (
    <section className="issued-page">
      <section className="page-heading">
        <div>
          <h1>Returned Books</h1>
          <p>
            Review books that have been returned.
          </p>
        </div>
      </section>
      <div className="filter-bar">
        <label className="search-box issued-search">
          <span>⌕</span>
          <input
            value={searchText}
            onChange={(event) => {
              setCurrentPage(1);
              setSearchText(event.target.value);
            }}
            placeholder="Search book, member, or ID"
          />
        </label>
        <select
          className="filter-select"
          value={statusFilter}
          onChange={(event) => {
            setCurrentPage(1);
            setStatusFilter(event.target.value);
          }}
          aria-label="Filter returned books by status"
        >
          <option value="all">All statuses</option>
          {TRANSACTION_STATUSES.map((status) => (
            <option value={status} key={status}>{status}</option>
          ))}
        </select>
        <DateRangeFilter
          dateRange={dateRange}
          onChange={(range) => {
            setCurrentPage(1);
            setDateRange(range);
          }}
          label="Filter returned books by date range"
        />
      </div>
      {error && <p className="form-error">{error}</p>}
      <div className="issued-table returned-table panel">
        <div className="table-row table-header">
          <span>BOOK</span>
          <span>MEMBER</span>
          <span>STUDENT ID</span>
          <span>RETURNED ON</span>
          <span>STATUS</span>
        </div>
        {visibleReturnedBooks.map((book) => (
          <div className="table-row issued-row" key={book.id}>
            <span>
              <strong>{book.title}</strong>
              <small>{book.author}</small>
            </span>
            <span>{book.member}</span>
            <span>{book.studentId}</span>
            <span>{formatDate(book.returnedOn)}</span>
            <span><em className="status">● {book.status}</em></span>
            <span>
              <button
                className="secondary-button"
                type="button"
                onClick={() => onViewDetails(book.title)}
              >
                👁 View Details
              </button>
            </span>
          </div>
        ))}
        <Pagination
          currentPage={currentPage}
          pageCount={pageCount}
          totalItems={filteredReturnedBooks.length}
          onPageChange={setCurrentPage}
        />
      </div>
    </section>
  );
}

function Pagination({ currentPage, pageCount, totalItems, onPageChange }) {
  const firstItem = totalItems === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const lastItem = Math.min(currentPage * pageSize, totalItems);

  return (
    <div className="panel-footer">
      <span>Showing {firstItem}-{lastItem} of {totalItems}</span>
      <span className="pagination-controls">
        <button
          type="button"
          disabled={currentPage === 1}
          onClick={() => onPageChange(currentPage - 1)}
        >
          Previous
        </button>
        <b>{currentPage}</b>
        <button
          type="button"
          disabled={currentPage === pageCount}
          onClick={() => onPageChange(currentPage + 1)}
        >
          Next
        </button>
      </span>
    </div>
  );
}

export default App;
