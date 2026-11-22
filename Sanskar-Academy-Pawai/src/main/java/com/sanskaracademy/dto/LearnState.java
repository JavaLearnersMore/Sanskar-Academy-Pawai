package com.sanskaracademy.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** What the student is currently learning - kept in the HTTP session. */
@Getter
@Setter
public class LearnState implements Serializable {

    private static final long serialVersionUID = 1L;

    private String subjectName;
    private String topic;
    private String level;
    private String lesson;
    private ArrayList<ChatLine> chat = new ArrayList<>();

    private String practiceTopic;
    private ArrayList<AiQuestion> practice = new ArrayList<>();

    public boolean hasLesson() {
        return lesson != null && !lesson.isBlank();
    }

    public void setPracticeList(List<AiQuestion> list) {
        this.practice = new ArrayList<>(list);
    }
}
