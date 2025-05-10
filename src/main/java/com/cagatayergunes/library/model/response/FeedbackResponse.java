package com.cagatayergunes.library.model.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackResponse {

    private Double note;
    private String comment;
}
