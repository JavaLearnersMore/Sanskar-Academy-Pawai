package com.sanskaracademy.dto;

import java.io.Serializable;
import java.util.List;

/** One multiple-choice question produced by the AI. */
public record AiQuestion(String question, List<String> options, int correctIndex, String explanation)
        implements Serializable {
}
