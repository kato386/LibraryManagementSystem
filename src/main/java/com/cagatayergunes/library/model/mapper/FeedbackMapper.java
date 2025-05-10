package com.cagatayergunes.library.model.mapper;

import com.cagatayergunes.library.model.Book;
import com.cagatayergunes.library.model.Feedback;
import com.cagatayergunes.library.model.request.FeedbackRequest;
import com.cagatayergunes.library.model.response.FeedbackResponse;
import org.springframework.stereotype.Service;

@Service
public class FeedbackMapper {

    public Feedback toFeedback(FeedbackRequest request){
        Book book = Book.builder()
                .id(request.bookId())
                .build();

        return Feedback.builder()
                .note(request.note())
                .comment(request.comment())
                .book(book)
                .build();
    }

    public FeedbackResponse toFeedbackResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .note(feedback.getNote())
                .comment(feedback.getComment())
                .build();
    }
}
