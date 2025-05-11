package com.cagatayergunes.library.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Book extends BaseEntity{
    private String title;
    private String authorName;
    private String isbn;
    private String synopsis;
    private String genre;
    private LocalDate publicationDate;
    private boolean shareable;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Feedback> feedbacks;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookTransactionHistory> histories;

    @Transient
    public double getRate(){
        if(feedbacks == null || feedbacks.isEmpty()){
            return 0.0;
        }

        return this.feedbacks.stream()
                .mapToDouble(Feedback::getNote)
                .average()
                .orElse(0.0);
    }

    @Transient
    public boolean isOverdue() {
        if (histories == null || histories.isEmpty()) {
            return false;
        }

        return histories.stream()
                .filter(history -> history.getReturnDate() == null)
                .anyMatch(history -> history.getDueDate().isBefore(LocalDateTime.now()));
    }
}
