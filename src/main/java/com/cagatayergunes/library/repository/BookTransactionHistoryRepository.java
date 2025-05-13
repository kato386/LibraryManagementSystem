package com.cagatayergunes.library.repository;

import com.cagatayergunes.library.model.Book;
import com.cagatayergunes.library.model.BookTransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BookTransactionHistoryRepository extends JpaRepository<BookTransactionHistory, Long> {
    @Query("""
                SELECT history
                FROM BookTransactionHistory history
                WHERE history.user.id = :userId
            """
    )
    Page<BookTransactionHistory> findAllBorrowedBooks(Pageable pageable, Long userId);

    @Query("""
    SELECT history
    FROM BookTransactionHistory history
""")
    Page<BookTransactionHistory> findAllBorrowedBooks(Pageable pageable);


    @Query("""
    SELECT history
    FROM BookTransactionHistory history
    WHERE history.returned = true
""")
    Page<BookTransactionHistory> findAllReturnedBooks(Pageable pageable);

    @Query("""
                SELECT history
                FROM BookTransactionHistory history
                WHERE history.user.id = :userId
                AND history.returned = true
            """
    )
    Page<BookTransactionHistory> findAllReturnedBooks(Pageable pageable, Long userId);

    @Query("""
            SELECT EXISTS (
            SELECT 1
            FROM BookTransactionHistory bth
            WHERE bth.book.id = :bookId AND bth.returnApproved = false
            )
        """
    )
    boolean isAlreadyBorrowedByUser(Long bookId);

    Optional<BookTransactionHistory> findByBookAndReturnApprovedFalseAndReturnedFalse(Book book);

    Optional<BookTransactionHistory> findByBookAndReturnApprovedFalseAndReturnedTrue(Book book);
}
