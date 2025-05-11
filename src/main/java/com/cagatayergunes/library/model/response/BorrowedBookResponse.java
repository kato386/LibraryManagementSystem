package com.cagatayergunes.library.model.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BorrowedBookResponse {
    private Long id;
    private String title;
    private String authorName;
    private String isbn;
    private String genre;
    private LocalDate publicationDate;
    private double rate;
    private boolean returned;
    private boolean returnApproved;
    private long lateDays;
}
