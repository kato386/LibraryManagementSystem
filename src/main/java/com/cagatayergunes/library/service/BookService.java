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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;

    public Long save(BookRequest request) {
        Book book = bookMapper.toBook(request);

        return bookRepository.save(book).getId();
    }

    public BookResponse findById(Long bookId) {
        return bookRepository.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the id"));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size) {
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());

        Page<Book> books = bookRepository.findAllDisplayableBooks(pageable);
        List<BookResponse> bookResponse = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponse,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = bookTransactionHistoryRepository.findAllBorrowedBooks(pageable, user.getId());
        List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {

        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createdDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks = bookTransactionHistoryRepository.findAllReturnedBooks(pageable, user.getId());
        List<BorrowedBookResponse> bookResponse = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponse,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public BookResponse updateBook(Long bookId, BookRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the id" + bookId));
        book.setTitle(request.title());
        book.setAuthorName(request.authorName());
        book.setIsbn(request.isbn());
        book.setSynopsis(request.synopsis());
        book.setShareable(request.shareable());

        bookRepository.save(book);

        return bookMapper.toBookResponse(bookRepository.save(book));
    }

    public BookResponse updateShareableStatus(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the id" + bookId));

        book.setShareable(!book.isShareable());
        bookRepository.save(book);
        return bookMapper.toBookResponse(book);
    }

    public BorrowedBookResponse borrowBook(Long bookId, Authentication connectedUser) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the id" + bookId));

        if(!book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be borrowed since it is not shareable");
        }
        User user = ((User) connectedUser.getPrincipal());
        final boolean isAlreadyBorrowed = bookTransactionHistoryRepository.isAlreadyBorrowedByUser(bookId);

        if(isAlreadyBorrowed){
            throw new OperationNotPermittedException("The requested book is already borrowed.");
        }
        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusWeeks(2))
                .returned(false)
                .returnApproved(false)
                .build();

        bookTransactionHistoryRepository.save(bookTransactionHistory);

        return bookMapper.toBorrowedBookResponse(bookTransactionHistory);
    }

    public BorrowedBookResponse returnBorrowedBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the id" + bookId));

        if(!book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be returned since it is not shareable");
        }

        BookTransactionHistory history = bookTransactionHistoryRepository.findByBookAndReturnApprovedFalseAndReturnedFalse(book)
                .orElseThrow(() -> new EntityNotFoundException("No active borrow transaction found for this book"));

        history.setReturned(true);
        bookTransactionHistoryRepository.save(history);
        long lateDays = 0;
        if(history.getDueDate() != null && LocalDateTime.now().isAfter(history.getDueDate())){
            lateDays = ChronoUnit.DAYS.between(history.getDueDate(), LocalDateTime.now());
        }

        BorrowedBookResponse response = bookMapper.toBorrowedBookResponse(history);
        response.setLateDays(lateDays);

        return response;
    }

    public BorrowedBookResponse approveReturnBorrowedBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("No book found with the id" + bookId));

        if(!book.isShareable()){
            throw new OperationNotPermittedException("The requested book cannot be returned since it is not shareable");
        }

        BookTransactionHistory history = bookTransactionHistoryRepository.findByBookAndReturnApprovedFalseAndReturnedTrue(book)
                .orElseThrow(() -> new EntityNotFoundException("No active borrow transaction found or borrow for this book already approved for this book"));

        history.setReturnApproved(true);
        bookTransactionHistoryRepository.save(history);

        long lateDays = 0;
        if(history.getDueDate() != null && LocalDateTime.now().isAfter(history.getDueDate())){
            lateDays = ChronoUnit.DAYS.between(history.getDueDate(), LocalDateTime.now());
        }

        BorrowedBookResponse response = bookMapper.toBorrowedBookResponse(history);
        response.setLateDays(lateDays);

        return response;
    }
}
