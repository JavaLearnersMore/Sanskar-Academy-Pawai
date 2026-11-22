package com.sanskaracademy.dto;

/** One written "thinking" question made by the AI, with a model answer. */
public record AiReasoningQuestion(String question, String modelAnswer) {
}
