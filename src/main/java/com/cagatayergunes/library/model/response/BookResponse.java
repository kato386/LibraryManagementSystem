package com.cagatayergunes.library.model.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookResponse {

    private Long id;
    private String title;
    private String authorName;
    private String isbn;
    private String synopsis;
    private boolean shareable;
    private double rate;
}
