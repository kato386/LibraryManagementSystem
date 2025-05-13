package com.cagatayergunes.library.controller;

import com.cagatayergunes.library.model.response.BookResponse;
import com.cagatayergunes.library.model.response.BorrowedBookResponse;
import com.cagatayergunes.library.service.ReactiveBookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("reactive/books")
@RequiredArgsConstructor
@Tag(name = "Book")
public class ReactiveBookController {

    private final ReactiveBookService service;


    @PatchMapping("/shareable/{book_id}")
    public Mono<ResponseEntity<BookResponse>> updateShareableStatus(
            @PathVariable("book_id") Long bookId
    ) {
        return service.updateShareableStatus(bookId)
                .map(updatedBook -> ResponseEntity.ok(updatedBook))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/borrow/{book_id}")
    public Mono<ResponseEntity<BorrowedBookResponse>> borrowBook(
            @PathVariable("book_id") Long bookId,
            Authentication connectedUser
    ){
        return service.borrowBook(bookId, connectedUser)
                .map(borrowedBook -> ResponseEntity.ok(borrowedBook))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));
    }

    @PatchMapping("/borrow/return/{book_id}")
    public Mono<ResponseEntity<BorrowedBookResponse>> returnBorrowBook(
            @PathVariable("book_id") Long bookId
    ){
        return service.returnBorrowedBook(bookId)
                .map(returnedBook -> ResponseEntity.ok(returnedBook))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));
    }

    @PreAuthorize("hasAuthority('LIBRARIAN')")
    @PatchMapping("/borrow/return/approve/{book_id}")
    public Mono<ResponseEntity<BorrowedBookResponse>> approveReturnBorrowBook(
            @PathVariable("book_id") Long bookId
    ){
        return service.approveReturnBorrowedBook(bookId)
                .map(approvedBook -> ResponseEntity.ok(approvedBook))
                .switchIfEmpty(Mono.just(ResponseEntity.badRequest().build()));
    }
}
