package com.sanskaracademy.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.sanskaracademy.dto.AiReasoningQuestion;

/** AI-written "thinking" questions (why / how / own example) with a model answer for marking. */
@Service
public class AiReasoningService {

    private static final int MAX_CALLS_PER_HOUR = 20;

    private final ChatClient chatClient;
    private final AiTutorService tutor;
    private final Map<Long, Deque<Long>> usage = new ConcurrentHashMap<>();

    public AiReasoningService(ChatClient.Builder builder, AiTutorService tutor) {
        this.chatClient = builder.defaultSystem(
                "You write exam questions for school students of an Indian coaching academy. "
              + "Be accurate. Plain text only. Never write anything harmful or adult.").build();
        this.tutor = tutor;
    }

    public boolean isEnabled() {
        return tutor.isEnabled();
    }

    public List<AiReasoningQuestion> generate(Long userId, String className, String subject, String topic,
                                              int count, String difficulty, String style) {
        if (!tutor.isEnabled()) {
            throw new IllegalStateException("AI is not switched on yet. Ask the admin to set the AI_API_KEY.");
        }
        long now = System.currentTimeMillis();
        Deque<Long> q = usage.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > 3_600_000L) q.pollFirst();
            if (q.size() >= MAX_CALLS_PER_HOUR) throw new IllegalStateException("You have used the AI many times this hour. Please try again later.");
            q.addLast(now);
        }
        int n = Math.max(1, Math.min(count, 5));
        String kind;
        switch (style == null ? "WHY" : style) {
            case "EXAMPLE":
                kind = "Each question must ask the student to GIVE THEIR OWN EXAMPLE or explain it with a situation from their own life, so that a copied textbook answer does not fit.";
                break;
            case "REAL_LIFE":
                kind = "Each question must ask the student to APPLY the idea to a new, real-life situation described in the question, not to repeat a definition.";
                break;
            default:
                kind = "Each question must ask WHY or HOW (reasoning, comparison, cause and effect), not for a definition that can be copied from a book.";
        }
        String user = """
                Create %d written-answer questions.
                Class: %s
                Subject: %s
                Chapter / topic: %s
                Difficulty: %s

                %s
                Rules:
                - "question" is one clear question the student can answer in 2-5 sentences.
                - "modelAnswer" is a short correct answer (2-4 sentences, or key points) that a teacher or AI
                  can use to mark answers written in different words.
                - Suitable for the class and factually correct.
                """.formatted(n, clean(className), clean(subject), clip(topic, 200), clean(difficulty), kind);
        List<AiReasoningQuestion> list;
        try {
            list = chatClient.prompt().user(user).call().entity(new ParameterizedTypeReference<List<AiReasoningQuestion>>() { });
        } catch (Exception e) {
            throw new IllegalStateException("The AI tutor is not available right now. Please try again in a moment.");
        }
        List<AiReasoningQuestion> out = new ArrayList<>();
        if (list != null) {
            for (AiReasoningQuestion r : list) {
                if (r != null && r.question() != null && !r.question().isBlank()
                        && r.modelAnswer() != null && !r.modelAnswer().isBlank()) {
                    out.add(new AiReasoningQuestion(clip(r.question(), 950), clip(r.modelAnswer(), 950)));
                }
            }
        }
        if (out.isEmpty()) throw new IllegalStateException("The AI could not create questions for this topic. Try different words.");
        return out;
    }

    private static String clip(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? "not given" : s.trim();
    }
}
