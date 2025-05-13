package com.cagatayergunes.library.model.mapper;

import com.cagatayergunes.library.model.Book;
import com.cagatayergunes.library.model.BookTransactionHistory;
import com.cagatayergunes.library.model.request.BookRequest;
import com.cagatayergunes.library.model.response.BookResponse;
import com.cagatayergunes.library.model.response.BorrowedBookResponse;
import org.springframework.stereotype.Service;

@Service
public class BookMapper {

    public Book toBook(BookRequest request){
        return Book.builder()
                .id(request.id())
                .title(request.title())
                .authorName(request.authorName())
                .isbn(request.isbn())
                .publicationDate(request.publicationDate())
                .genre(request.genre())
                .synopsis(request.synopsis())
                .shareable(request.shareable())
                .build();
    }

    public BookResponse toBookResponse(Book book){
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .authorName(book.getAuthorName())
                .isbn(book.getIsbn())
                .publicationDate(book.getPublicationDate())
                .genre(book.getGenre())
                .synopsis(book.getSynopsis())
                .rate(book.getRate())
                .shareable(book.isShareable())
                .build();
    }

    public BorrowedBookResponse toBorrowedBookResponse(BookTransactionHistory history){
        return BorrowedBookResponse.builder()
                .id(history.getBook().getId())
                .title(history.getBook().getTitle())
                .authorName(history.getBook().getAuthorName())
                .isbn(history.getBook().getIsbn())
                .rate(history.getBook().getRate())
                .returned(history.isReturned())
                .returnApproved(history.isReturnApproved())
                .email(history.getUser().getEmail())
                .build();
    }
}
