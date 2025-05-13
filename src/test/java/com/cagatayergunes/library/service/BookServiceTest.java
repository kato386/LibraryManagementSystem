package com.cagatayergunes.library.service;

import com.cagatayergunes.library.exception.handler.OperationNotPermittedException;
import com.cagatayergunes.library.model.*;
import com.cagatayergunes.library.model.mapper.BookMapper;
import com.cagatayergunes.library.model.request.BookRequest;
import com.cagatayergunes.library.model.response.BookResponse;
import com.cagatayergunes.library.model.response.BorrowedBookResponse;
import com.cagatayergunes.library.model.response.PageResponse;
import com.cagatayergunes.library.repository.BookRepository;
import com.cagatayergunes.library.repository.BookTransactionHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookServiceTest {

    @InjectMocks
    private BookService bookService;

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookTransactionHistoryRepository historyRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private BookRequest sampleRequest() {
        return new BookRequest(1L, "Test Book", "Author", "123456", "Synopsis", "Fiction", LocalDate.now(), true);
    }

    @Test
    void testSaveBook() {
        BookRequest request = sampleRequest();
        Book book = Book.builder()
                .id(1L)
                .title(request.title())
                .authorName(request.authorName())
                .isbn(request.isbn())
                .synopsis(request.synopsis())
                .genre(request.genre())
                .publicationDate(request.publicationDate())
                .shareable(request.shareable())
                .build();

        when(bookMapper.toBook(request)).thenReturn(book);
        when(bookRepository.save(book)).thenReturn(book);

        Long savedId = bookService.save(request);
        assertEquals(1L, savedId);
        verify(bookRepository).save(book);
    }

    @Test
    void testFindBookById_Success() {
        Book book = Book.builder().id(1L).title("Test").build();
        BookResponse response = BookResponse.builder().id(1L).title("Test").build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toBookResponse(book)).thenReturn(response);

        BookResponse result = bookService.findById(1L);
        assertEquals("Test", result.getTitle());
    }

    @Test
    void testFindBookById_NotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bookService.findById(1L));
    }

    @Test
    void testUpdateBook() {
        BookRequest request = sampleRequest();
        Book existingBook = Book.builder().id(1L).title("Old").build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenReturn(existingBook);
        when(bookMapper.toBookResponse(existingBook)).thenReturn(new BookResponse());

        BookResponse response = bookService.updateBook(1L, request);
        assertNotNull(response);
        verify(bookRepository).save(existingBook);
    }

    @Test
    void testUpdateShareableStatus() {
        Book book = Book.builder().id(1L).shareable(true).build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);
        when(bookMapper.toBookResponse(book)).thenReturn(BookResponse.builder().shareable(false).build());

        BookResponse response = bookService.updateShareableStatus(1L);
        assertFalse(response.isShareable());
        verify(bookRepository).save(book);
    }

    @Test
    void testBorrowBook_Success() {
        Book book = Book.builder().id(1L).shareable(true).build();
        User user = User.builder().id(1L).email("testuser@gmail.com").build();
        BorrowedBookResponse expectedResponse = new BorrowedBookResponse();
        expectedResponse.setId(1L);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(historyRepository.isAlreadyBorrowedByUser(1L)).thenReturn(false);
        when(bookMapper.toBorrowedBookResponse(any())).thenReturn(expectedResponse);

        BorrowedBookResponse response = bookService.borrowBook(1L, authentication);

        assertNotNull(response);
        assertEquals(response.getId(), 1L);
        verify(historyRepository).save(any(BookTransactionHistory.class));
    }

    @Test
    void testBorrowBook_BookAlreadyBorrowed() {
        Book book = Book.builder().id(1L).shareable(true).build();
        User user = User.builder().id(1L).email("testuser@gmail.com").build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(historyRepository.isAlreadyBorrowedByUser(1L)).thenReturn(true);


        assertThrows(OperationNotPermittedException.class, () -> bookService.borrowBook(1L, authentication));
    }

    @Test
    void testBorrowBook_BookNotFound() {
        User user = User.builder().id(1L).email("testuser@gmail.com").build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bookService.borrowBook(1L, authentication));
    }

    @Test
    void testReturnBorrowedBook_Success() {
        Book book = Book.builder().id(1L).shareable(true).build();
        BookTransactionHistory history = BookTransactionHistory.builder().book(book).build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(historyRepository.findByBookAndReturnApprovedFalseAndReturnedFalse(book)).thenReturn(Optional.of(history));
        when(bookMapper.toBorrowedBookResponse(history)).thenReturn(new BorrowedBookResponse());

        BorrowedBookResponse response = bookService.returnBorrowedBook(1L);
        assertNotNull(response);
        assertTrue(history.isReturned());
    }

    @Test
    void testReturnBorrowedBook_BookIsNotShareable() {
        Book book = Book.builder().id(1L).shareable(false).build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        assertThrows(OperationNotPermittedException.class, () -> bookService.returnBorrowedBook(1L));
    }

    @Test
    void testReturnBorrowedBook_HistoryNotFound() {
        Book book = Book.builder().id(1L).shareable(true).build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(historyRepository.findByBookAndReturnApprovedFalseAndReturnedFalse(book)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bookService.returnBorrowedBook(1L));
    }

    @Test
    void testApproveReturnBorrowedBook_Success() {
        Book book = Book.builder().id(1L).shareable(true).build();
        BookTransactionHistory history = BookTransactionHistory.builder().book(book).build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(historyRepository.findByBookAndReturnApprovedFalseAndReturnedTrue(book)).thenReturn(Optional.of(history));
        when(bookMapper.toBorrowedBookResponse(history)).thenReturn(new BorrowedBookResponse());

        BorrowedBookResponse response = bookService.approveReturnBorrowedBook(1L);
        assertNotNull(response);
        assertTrue(history.isReturnApproved());
    }

    @Test
    void testApproveReturnBorrowedBook_BookIsNotShareable() {
        Book book = Book.builder().id(1L).shareable(false).build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThrows(OperationNotPermittedException.class, () -> bookService.approveReturnBorrowedBook(1L));
    }

    @Test
    void testSearchBooks() {
        Book book = Book.builder()
                .id(1L)
                .title("Test Book")
                .authorName("Author")
                .isbn("123456")
                .genre("Fiction")
                .build();
        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .authorName("Author")
                .isbn("123456")
                .genre("Fiction")
                .build();

        Page<Book> bookPage = new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1);

        when(bookRepository.findByTitleContainingIgnoreCaseAndAuthorNameContainingIgnoreCaseAndIsbnContainingIgnoreCaseAndGenreContainingIgnoreCase(
                eq("Test"), eq("Author"), eq("123456"), eq("Fiction"), any(Pageable.class)
        )).thenReturn(bookPage);

        when(bookMapper.toBookResponse(book)).thenReturn(bookResponse);

        PageResponse<BookResponse> result = bookService.searchBooks(0, 10, "Test", "Author", "123456", "Fiction");

        assertEquals(1, result.getContent().size());
        assertEquals("Test Book", result.getContent().get(0).getTitle());
        assertEquals(1, result.getTotalElements());
        verify(bookRepository).findByTitleContainingIgnoreCaseAndAuthorNameContainingIgnoreCaseAndIsbnContainingIgnoreCaseAndGenreContainingIgnoreCase(
                eq("Test"), eq("Author"), eq("123456"), eq("Fiction"), any(Pageable.class)
        );
    }

    @Test
    void testDeleteBook() {
        Book book = Book.builder().id(1L).title("To Delete").build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.deleteBook(1L);

        verify(bookRepository).delete(book);
    }

}
