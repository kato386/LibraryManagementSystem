package com.cagatayergunes.library.repository;

import com.cagatayergunes.library.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByBookId(Long bookId, Pageable pageable);
}