package com.cagatayergunes.library.service;

import com.cagatayergunes.library.exception.handler.OperationNotPermittedException;
import com.cagatayergunes.library.model.Book;
import com.cagatayergunes.library.model.BookTransactionHistory;
import com.cagatayergunes.library.model.User;
import com.cagatayergunes.library.model.mapper.BookMapper;
import com.cagatayergunes.library.model.request.BookRequest;
import com.cagatayergunes.library.model.response.BookResponse;
import com.cagatayergunes.library.model.response.BorrowedBookResponse;
import com.cagatayergunes.library.model.response.PageResponse;
import com.cagatayergunes.library.repository.BookRepository;
import com.cagatayergunes.library.repository.BookTransactionHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;

    public Long save(BookRequest request) {
        log.info("Saving new book: {}", request.title());
        Book book = bookMapper.toBook(request);
        Long id = bookRepository.save(book).getId();
        log.info("Book saved with ID: {}", id);
        return id;
    }

    public BookResponse findById(Long bookId) {
        log.info("Finding book by ID: {}", bookId);
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> {
                    log.error("Book not found with ID: {}", bookId);
                    return new EntityNotFoundException("No book found with the id");
                });
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size) {
        log.info("Fetching all books, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable);
        List<BookResponse> bookResponse = books.stream().map(bookMapper::toBookResponse).toList();
        log.info("Fetched {} books", bookResponse.size());
        return new PageResponse<>(bookResponse, books.getNumber(), books.getSize(), books.getTotalElements(), books.getTotalPages(), books.isFirst(), books.isLast());
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = getAuthenticatedUser(connectedUser);
        log.info("Fetching borrowed books for user: {}", user.getUsername());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> result = bookTransactionHistoryRepository.findAllBorrowedBooks(pageable, user.getId());
        List<BorrowedBookResponse> responses = result.stream().map(bookMapper::toBorrowedBookResponse).toList();
        log.info("User {} has {} borrowed books", user.getUsername(), responses.size());
        return new PageResponse<>(responses, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        User user = getAuthenticatedUser(connectedUser);
        log.info("Fetching returned books for user: {}", user.getUsername());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> result = bookTransactionHistoryRepository.findAllReturnedBooks(pageable, user.getId());
        List<BorrowedBookResponse> responses = result.stream().map(bookMapper::toBorrowedBookResponse).toList();
        log.info("User {} has {} returned books", user.getUsername(), responses.size());
        return new PageResponse<>(responses, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }

    public BookResponse updateBook(Long bookId, BookRequest request) {
        log.info("Updating book with ID: {}", bookId);
        Book book = getBookByIdOrThrow(bookId);
        book.setTitle(request.title());
        book.setAuthorName(request.authorName());
        book.setIsbn(request.isbn());
        book.setSynopsis(request.synopsis());
        book.setShareable(request.shareable());
        Book updatedBook = bookRepository.save(book);
        log.info("Book with ID {} updated successfully", bookId);
        return bookMapper.toBookResponse(updatedBook);
    }

    public BookResponse updateShareableStatus(Long bookId) {
        log.info("Toggling shareable status for book ID: {}", bookId);
        Book book = getBookByIdOrThrow(bookId);
        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        log.info("Shareable status updated to {} for book ID {}", book.isShareable(), bookId);
        return bookMapper.toBookResponse(book);
    }

    public BorrowedBookResponse borrowBook(Long bookId, Authentication connectedUser) {
        log.info("User attempting to borrow book ID: {}", bookId);
        Book book = getBookByIdOrThrow(bookId);

        if (!book.isShareable()) {
            log.warn("Book is not shareable: {}", bookId);
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is not shareable");
        }

        User user = getAuthenticatedUser(connectedUser);
        if (bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId)) {
            log.warn("Book ID {} is already borrowed", bookId);
            throw new OperationNotPermittedException("The requested book is already borrowed.");
        }

        BookTransactionHistory history = BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusWeeks(2))
                .returned(false)
                .returnApproved(false)
                .build();

        bookTransactionHistoryRepository.save(history);
        log.info("Book borrowed successfully: bookId={}, user={}", bookId, user.getUsername());
        return bookMapper.toBorrowedBookResponse(history);
    }

    public BorrowedBookResponse returnBorrowedBook(Long bookId) {
        log.info("Returning borrowed book: {}", bookId);
        Book book = getBookByIdOrThrow(bookId);

        if (!book.isShareable()) {
            log.warn("Book not shareable for return: {}", bookId);
            throw new OperationNotPermittedException("The requested book cannot be returned since it is not shareable");
        }

        BookTransactionHistory history = bookTransactionHistoryRepository.findByBookAndReturnApprovedFalseAndReturnedFalse(book)
                .orElseThrow(() -> {
                    log.error("No active borrow transaction for book {}", bookId);
                    return new EntityNotFoundException("No active borrow transaction found for this book");
                });

        history.setReturned(true);
        bookTransactionHistoryRepository.save(history);
        long lateDays = calculateLateDays(history.getDueDate());

        log.info("Book returned: {}, lateDays={}", bookId, lateDays);
        BorrowedBookResponse response = bookMapper.toBorrowedBookResponse(history);
        response.setLateDays(lateDays);
        return response;
    }

    public BorrowedBookResponse approveReturnBorrowedBook(Long bookId) {
        log.info("Approving return of book: {}", bookId);
        Book book = getBookByIdOrThrow(bookId);

        if (!book.isShareable()) {
            log.warn("Book not shareable: {}", bookId);
            throw new OperationNotPermittedException("The requested book cannot be returned since it is not shareable");
        }

        BookTransactionHistory history = bookTransactionHistoryRepository.findByBookAndReturnApprovedFalseAndReturnedTrue(book)
                .orElseThrow(() -> {
                    log.error("No pending return approval for book: {}", bookId);
                    return new EntityNotFoundException("No active borrow transaction found or this book already approved.");
                });

        history.setReturnApproved(true);
        history.setReturnDate(LocalDateTime.now());
        bookTransactionHistoryRepository.save(history);

        long lateDays = 0;
        if (history.getDueDate() != null && LocalDateTime.now().isAfter(history.getDueDate())) {
            lateDays = ChronoUnit.DAYS.between(history.getDueDate(), LocalDateTime.now());
        }

        log.info("Book return approved: {}, lateDays={}", bookId, lateDays);
        BorrowedBookResponse response = bookMapper.toBorrowedBookResponse(history);
        response.setLateDays(lateDays);
        return response;
    }

    public PageResponse<BookResponse> searchBooks(int page, int size, String title, String authorName, String isbn, String genre) {
        log.info("Searching books: title='{}', author='{}', isbn='{}', genre='{}'", title, authorName, isbn, genre);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<Book> books = bookRepository.findByTitleContainingIgnoreCaseAndAuthorNameContainingIgnoreCaseAndIsbnContainingIgnoreCaseAndGenreContainingIgnoreCase(title, authorName, isbn, genre, pageable);
        List<BookResponse> responses = books.getContent().stream().map(bookMapper::toBookResponse).toList();

        log.info("Found {} books matching search criteria", responses.size());
        return new PageResponse<>(responses, books.getNumber(), books.getSize(), books.getTotalElements(), books.getTotalPages(), books.isFirst(), books.isLast());
    }

    public void deleteBook(Long bookId) {
        log.info("Deleting book ID: {}", bookId);
        Book book = getBookByIdOrThrow(bookId);

        bookRepository.delete(book);
        log.info("Book deleted: {}", bookId);
    }

    public String generateOverdueBooksReport() {
        log.info("Generating overdue books report...");
        List<BookTransactionHistory> overdueHistories = bookTransactionHistoryRepository.findAll().stream()
                .filter(history -> !history.isReturned() && history.getDueDate().isBefore(LocalDateTime.now()))
                .toList();

        if (overdueHistories.isEmpty()) {
            log.info("No overdue books found");
            return "No overdue books found.";
        }

        StringBuilder reportBuilder = new StringBuilder("Overdue Books Report\n====================\n\n");
        for (BookTransactionHistory history : overdueHistories) {
            Book book = history.getBook();
            User user = history.getUser();
            reportBuilder.append("Title: ").append(book.getTitle()).append("\n");
            reportBuilder.append("Author: ").append(book.getAuthorName()).append("\n");
            reportBuilder.append("ISBN: ").append(book.getIsbn()).append("\n");
            reportBuilder.append("Due Date: ").append(history.getDueDate()).append("\n");
            reportBuilder.append("Currently with: ").append(user.getFirstName()).append(" ").append(user.getLastName())
                    .append(" (").append(user.getEmail()).append(")\n");
            reportBuilder.append("----------------------------\n");
        }

        log.info("Overdue books report generated with {} entries", overdueHistories.size());
        return reportBuilder.toString();
    }

    private Book getBookByIdOrThrow(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the id " + bookId));
    }

    private User getAuthenticatedUser(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    private long calculateLateDays(LocalDateTime dueDate) {
        return (dueDate != null && LocalDateTime.now().isAfter(dueDate))
                ? ChronoUnit.DAYS.between(dueDate, LocalDateTime.now())
                : 0;
    }
}
