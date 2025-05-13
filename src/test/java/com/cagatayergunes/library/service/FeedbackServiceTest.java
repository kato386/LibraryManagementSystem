package com.cagatayergunes.library.service;

import com.cagatayergunes.library.exception.handler.OperationNotPermittedException;
import com.cagatayergunes.library.model.Book;
import com.cagatayergunes.library.model.Feedback;
import com.cagatayergunes.library.model.mapper.FeedbackMapper;
import com.cagatayergunes.library.model.request.FeedbackRequest;
import com.cagatayergunes.library.model.response.FeedbackResponse;
import com.cagatayergunes.library.model.response.PageResponse;
import com.cagatayergunes.library.repository.BookRepository;
import com.cagatayergunes.library.repository.FeedbackRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedbackServiceTest {

    @InjectMocks
    private FeedbackService feedbackService;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private FeedbackMapper feedbackMapper;

    private Book book;

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        book = new Book();
        book.setId(1L);
        book.setShareable(true);
    }

    @Test
    void testSaveFeedback_ShouldSaveFeedback_WhenBookIsShareable() {
        // Arrange
        FeedbackRequest request = new FeedbackRequest(4.5, "Great book!", 1L);
        Feedback feedback = new Feedback();
        feedback.setId(1L);
        feedback.setNote(4.5);
        feedback.setComment("Great book!");
        feedback.setBook(book);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(feedbackMapper.toFeedback(request)).thenReturn(feedback);
        when(feedbackRepository.save(feedback)).thenReturn(feedback);

        // Act
        Long savedFeedbackId = feedbackService.save(request);

        // Assert
        assertNotNull(savedFeedbackId);
        assertEquals(1L, savedFeedbackId);
        verify(bookRepository, times(1)).findById(1L);
        verify(feedbackRepository, times(1)).save(feedback);
    }

    @Test
    void testSaveFeedback_ShouldThrowException_WhenBookIsNotShareable() {
        // Arrange
        book.setShareable(false);
        FeedbackRequest request = new FeedbackRequest(4.5, "Great book!", 1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        // Act & Assert
        OperationNotPermittedException exception = assertThrows(OperationNotPermittedException.class, () -> {
            feedbackService.save(request);
        });
        assertEquals("You cannot give a feedback for an non shareable book.", exception.getMessage());
    }

    @Test
    void testSaveFeedback_ShouldThrowException_WhenBookNotFound() {
        // Arrange
        FeedbackRequest request = new FeedbackRequest(4.5, "Great book!", 999L);

        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            feedbackService.save(request);
        });
    }

    @Test
    void testFindAllFeedbackByBook_ShouldReturnPagedFeedback() {
        // Arrange
        FeedbackRequest request = new FeedbackRequest(4.5, "Great book!", 1L);
        Feedback feedback = feedbackMapper.toFeedback(request);

        PageRequest pageRequest = PageRequest.of(0, 5);
        Page<Feedback> feedbackPage = new PageImpl<>(Collections.singletonList(feedback), pageRequest, 1);
        FeedbackResponse feedbackResponse = new FeedbackResponse(4.5, "Great book!");

        when(feedbackRepository.findAllByBookId(1L, pageRequest)).thenReturn(feedbackPage);
        when(feedbackMapper.toFeedbackResponse(feedback)).thenReturn(feedbackResponse);

        // Act
        PageResponse<FeedbackResponse> response = feedbackService.findAllFeedbackByBook(1L, 0, 5);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Great book!", response.getContent().get(0).getComment());
        verify(feedbackRepository, times(1)).findAllByBookId(1L, pageRequest);
    }

    @Test
    void shouldFailValidation_WhenNoteIsNegative() {
        FeedbackRequest request = new FeedbackRequest(-1.0, "Good book", 1L);

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("note")));
    }

    @Test
    void shouldFailValidation_WhenCommentIsBlank() {
        FeedbackRequest request = new FeedbackRequest(4.5, "  ", 1L);

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("comment")));
    }

    @Test
    void shouldFailValidation_WhenBookIdIsNull() {
        FeedbackRequest request = new FeedbackRequest(4.5, "Nice!", null);

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bookId")));
    }

    @Test
    void shouldPassValidation_WhenAllFieldsAreValid() {
        FeedbackRequest request = new FeedbackRequest(4.5, "Nice!", 1L);

        Set<ConstraintViolation<FeedbackRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}