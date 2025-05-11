package com.cagatayergunes.library.controller;

import com.cagatayergunes.library.model.request.BookRequest;
import com.cagatayergunes.library.model.response.BookResponse;
import com.cagatayergunes.library.model.response.BorrowedBookResponse;
import com.cagatayergunes.library.model.response.PageResponse;
import com.cagatayergunes.library.service.BookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("books")
@RequiredArgsConstructor
@Tag(name = "Book")
public class BookController {

    private final BookService service;

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PostMapping
    public ResponseEntity<Long> saveBook(@Valid @RequestBody BookRequest request){
        return ResponseEntity.ok(service.save(request));
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PutMapping("/{book-id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable("book-id") Long bookId,
            @Valid @RequestBody BookRequest request
    ) {
        return ResponseEntity.ok(service.updateBook(bookId, request));
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long bookId) {
        service.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{book-id}")
    public ResponseEntity<BookResponse> findBookById(
            @PathVariable("book-id") Long bookId
    ){
        return ResponseEntity.ok(service.findById(bookId));
    }

    @GetMapping
    public ResponseEntity<PageResponse<BookResponse>> findAllBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size
    ){
        return ResponseEntity.ok(service.findAllBooks(page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<BookResponse>> searchBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String genre
    ) {
        PageResponse<BookResponse> response = service.searchBooks(page, size, title, authorName, isbn, genre);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/borrowed")
    public ResponseEntity<PageResponse<BorrowedBookResponse>> findAllBorrowedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(service.findAllBorrowedBooks(page, size, connectedUser));
    }

    @GetMapping("/returned")
    public ResponseEntity<PageResponse<BorrowedBookResponse>> findAllReturnedBooks(
            @RequestParam(name = "page", defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(service.findAllReturnedBooks(page, size, connectedUser));
    }

    @PatchMapping("/shareable/{book_id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BookResponse> updateShareableStatus(
            @PathVariable("book_id") Long bookId
    ){
        return ResponseEntity.ok(service.updateShareableStatus(bookId));
    }

    @PostMapping("/borrow/{book_id}")
    public ResponseEntity<BorrowedBookResponse> borrowBook(
            @PathVariable("book_id") Long bookId,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(service.borrowBook(bookId, connectedUser));
    }

    @PatchMapping("/borrow/return/{book_id}")
    public ResponseEntity<BorrowedBookResponse> returnBorrowBook(
            @PathVariable("book_id") Long bookId
    ){
        return ResponseEntity.ok(service.returnBorrowedBook(bookId));
    }

    @PatchMapping("/borrow/return/approve/{book_id}")
    @PreAuthorize("hasAuthority('LIBRARIAN')")
    public ResponseEntity<BorrowedBookResponse> approveReturnBorrowBook(
            @PathVariable("book_id") Long bookId
    ){
        return ResponseEntity.ok(service.approveReturnBorrowedBook(bookId));
    }
}
