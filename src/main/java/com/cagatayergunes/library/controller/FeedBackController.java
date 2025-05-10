package com.cagatayergunes.library.controller;

import com.cagatayergunes.library.model.request.FeedbackRequest;
import com.cagatayergunes.library.model.response.FeedbackResponse;
import com.cagatayergunes.library.model.response.PageResponse;
import com.cagatayergunes.library.service.FeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("feedbacks")
@RequiredArgsConstructor
@Tag(name = "feedback")
public class FeedBackController {

    private final FeedbackService service;

    @PostMapping
    public ResponseEntity<Long> saveFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication connectedUser
    ){
        return ResponseEntity.ok(service.save(request));
    }

    @GetMapping("/book/{book-id}")
    public ResponseEntity<PageResponse<FeedbackResponse>> findAllFeedbackByBook(
            @PathVariable("book-id") Long bookId,
            @RequestParam(name = "page",defaultValue = "0", required = false) int page,
            @RequestParam(name = "size", defaultValue = "10", required = false) int size
    ){
        return ResponseEntity.ok(service.findAllFeedbackByBook(bookId, page, size));
    }
}
