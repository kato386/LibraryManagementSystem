package com.cagatayergunes.library.repository;

import com.cagatayergunes.library.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
        SELECT book
        FROM Book book
        WHERE book.shareable = true
        """)
    Page<Book> findAllDisplayableBooks(Pageable pageable);

    Page<Book> findByTitleContainingIgnoreCaseAndAuthorNameContainingIgnoreCaseAndIsbnContainingIgnoreCaseAndGenreContainingIgnoreCase(
            String title, String authorName, String isbn, String genre, Pageable pageable);
}