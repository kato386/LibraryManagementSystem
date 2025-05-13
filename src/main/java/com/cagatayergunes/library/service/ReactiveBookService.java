package com.cagatayergunes.library.service;

import com.cagatayergunes.library.exception.handler.OperationNotPermittedException;
import com.cagatayergunes.library.model.Book;
import com.cagatayergunes.library.model.BookTransactionHistory;
import com.cagatayergunes.library.model.User;
import com.cagatayergunes.library.model.mapper.BookMapper;
import com.cagatayergunes.library.model.response.BookResponse;
import com.cagatayergunes.library.model.response.BorrowedBookResponse;
import com.cagatayergunes.library.repository.BookRepository;
import com.cagatayergunes.library.repository.BookTransactionHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveBookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;


    public Mono<BookResponse> updateShareableStatus(Long bookId) {
        return Mono.fromCallable(() -> {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new EntityNotFoundException("Book not found"));
            book.setShareable(!book.isShareable());
            return bookRepository.save(book);
        }).map(bookMapper::toBookResponse);
    }

    public Mono<BorrowedBookResponse> borrowBook(Long bookId, Authentication connectedUser) {
        log.info("User attempting to borrow book ID: {}", bookId);
        return Mono.justOrEmpty(getBookByIdOrThrow(bookId))
                .flatMap(book -> {
                    if (!book.isShareable()) {
                        log.warn("Book is not shareable: {}", bookId);
                        return Mono.error(new OperationNotPermittedException("The requested book cannot be borrowed since it is not shareable"));
                    }
                    User user = getAuthenticatedUser(connectedUser);
                    return isAlreadyBorrowedByUser(bookId)
                            .flatMap(isAlreadyBorrowed -> {
                                if (isAlreadyBorrowed) {
                                    log.warn("Book ID {} is already borrowed", bookId);
                                    return Mono.error(new OperationNotPermittedException("The requested book is already borrowed."));
                                }

                                BookTransactionHistory history = BookTransactionHistory.builder()
                                        .user(user)
                                        .book(book)
                                        .borrowDate(LocalDateTime.now())
                                        .dueDate(LocalDateTime.now().plusWeeks(2))
                                        .returned(false)
                                        .returnApproved(false)
                                        .build();
                                return Mono.fromCallable(() -> bookTransactionHistoryRepository.save(history))
                                        .map(savedHistory -> bookMapper.toBorrowedBookResponse(savedHistory));
                            });
                });
    }

    public Mono<BorrowedBookResponse> returnBorrowedBook(Long bookId) {
        log.info("Returning borrowed book: {}", bookId);

        return Mono.justOrEmpty(getBookByIdOrThrow(bookId))
                .flatMap(book -> {
                    if (!book.isShareable()) {
                        log.warn("Book not shareable for return: {}", bookId);
                        return Mono.error(new OperationNotPermittedException("The requested book cannot be returned since it is not shareable"));
                    }

                    return Mono.justOrEmpty(bookTransactionHistoryRepository.findByBookAndReturnApprovedFalseAndReturnedFalse(book))
                            .switchIfEmpty(Mono.error(new EntityNotFoundException("No active borrow transaction found for this book")))
                            .flatMap(history -> {
                                history.setReturned(true);

                                return Mono.fromCallable(() -> bookTransactionHistoryRepository.save(history))
                                        .map(savedHistory -> {
                                            long lateDays = calculateLateDays(savedHistory.getDueDate());
                                            log.info("Book returned: {}, lateDays={}", bookId, lateDays);
                                            BorrowedBookResponse response = bookMapper.toBorrowedBookResponse(savedHistory);
                                            response.setLateDays(lateDays);
                                            return response;
                                        });
                            });
                });
    }

    public Mono<BorrowedBookResponse> approveReturnBorrowedBook(Long bookId) {
        log.info("Approving return of book: {}", bookId);

        return Mono.justOrEmpty(getBookByIdOrThrow(bookId))
                .flatMap(book -> {
                    if (!book.isShareable()) {
                        log.warn("Book not shareable: {}", bookId);
                        return Mono.error(new OperationNotPermittedException("The requested book cannot be returned since it is not shareable"));
                    }

                    return Mono.justOrEmpty(bookTransactionHistoryRepository.findByBookAndReturnApprovedFalseAndReturnedTrue(book))
                            .switchIfEmpty(Mono.error(new EntityNotFoundException("No active borrow transaction found or this book already approved.")))
                            .flatMap(history -> {
                                history.setReturnApproved(true);
                                history.setReturnDate(LocalDateTime.now());

                                return Mono.fromCallable(() -> bookTransactionHistoryRepository.save(history))
                                        .map(savedHistory -> {
                                            long lateDays = 0;
                                            if (savedHistory.getDueDate() != null && LocalDateTime.now().isAfter(savedHistory.getDueDate())) {
                                                lateDays = ChronoUnit.DAYS.between(savedHistory.getDueDate(), LocalDateTime.now());
                                            }

                                            log.info("Book return approved: {}, lateDays={}", bookId, lateDays);
                                            BorrowedBookResponse response = bookMapper.toBorrowedBookResponse(savedHistory);
                                            response.setLateDays(lateDays);
                                            return response;
                                        });
                            });
                });
    }

    public Mono<Boolean> isAlreadyBorrowedByUser(Long bookId) {
        return Mono.fromCallable(() -> {
            return bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId);
        });
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
