package com.cagatayergunes.library.service;

import com.cagatayergunes.library.exception.handler.OperationNotPermittedException;
import com.cagatayergunes.library.model.Book;
import com.cagatayergunes.library.model.Feedback;
import com.cagatayergunes.library.model.response.FeedbackResponse;
import com.cagatayergunes.library.model.response.PageResponse;
import com.cagatayergunes.library.repository.FeedbackRepository;
import com.cagatayergunes.library.model.mapper.FeedbackMapper;
import com.cagatayergunes.library.model.request.FeedbackRequest;
import com.cagatayergunes.library.repository.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackService {

    private final BookRepository bookRepository;
    private final FeedbackMapper mapper;
    private final FeedbackRepository feedbackRepository;

    public Long save(FeedbackRequest request) {
        log.info("Attempting to save feedback for bookId: {}", request.bookId());

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> {
                    log.warn("Book with ID {} not found", request.bookId());
                    return new EntityNotFoundException("No book found with the ID: " + request.bookId());
                });

        if (!book.isShareable()) {
            log.warn("Book with ID {} is not shareable. Feedback cannot be added.", book.getId());
            throw new OperationNotPermittedException("You cannot give a feedback for an non shareable book.");
        }

        Feedback feedback = mapper.toFeedback(request);
        Feedback savedFeedback = feedbackRepository.save(feedback);

        log.info("Feedback saved with ID: {}", savedFeedback.getId());
        return savedFeedback.getId();
    }

    public PageResponse<FeedbackResponse> findAllFeedbackByBook(Long bookId, int page, int size) {
        log.info("Retrieving feedbacks for bookId: {} | page: {} | size: {}", bookId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbacks = feedbackRepository.findAllByBookId(bookId, pageable);

        List<FeedbackResponse> feedbackResponses = feedbacks.stream()
                .map(f -> mapper.toFeedbackResponse(f))
                .toList();

        log.debug("Fetched {} feedback(s) for bookId: {}", feedbackResponses.size(), bookId);

        PageResponse<FeedbackResponse> response = new PageResponse<>(
                feedbackResponses,
                feedbacks.getNumber(),
                feedbacks.getSize(),
                feedbacks.getTotalElements(),
                feedbacks.getTotalPages(),
                feedbacks.isFirst(),
                feedbacks.isLast()
        );

        log.info("Returning paged feedback response for bookId: {}", bookId);
        return response;
    }
}
