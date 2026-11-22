package com.sanskaracademy.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.security.SecureRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.AttemptEvent;
import com.sanskaracademy.entity.AttemptGuard;
import com.sanskaracademy.entity.AttemptStatus;
import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.ExamAttempt;
import com.sanskaracademy.entity.ExamQuestion;
import com.sanskaracademy.entity.ExamSecurity;
import com.sanskaracademy.entity.ExamStatus;
import com.sanskaracademy.entity.QuestionType;
import com.sanskaracademy.repository.AttemptEventRepository;
import com.sanskaracademy.repository.AttemptGuardRepository;
import com.sanskaracademy.repository.ExamAttemptRepository;
import com.sanskaracademy.repository.ExamRepository;
import com.sanskaracademy.repository.ExamSecurityRepository;

/**
 * Anti-cheating helper for ONLINE exams:
 *  - settings per exam (strict mode, warnings, copy block, shuffle, question pool, time per question)
 *  - a personal paper for every student (random order / random subset / shuffled options)
 *  - counting of "left the exam page" warnings and the event log
 *  - per-question timing (kept on the server so a page refresh does not give extra time)
 * These measures make cheating harder and visible; no web page can stop a second phone.
 */
@Service("examGuardService")
public class ExamGuardService {

    private static final Set<String> COUNTED = Set.of("TAB_HIDDEN", "WINDOW_BLUR", "FULLSCREEN_EXIT");
    private static final Set<String> LOGGED_ONLY = Set.of("COPY_BLOCKED", "PASTE_BLOCKED", "MENU_BLOCKED", "FULLSCREEN_DENIED");
    private static final int MAX_EVENTS = 200;

    private final ExamSecurityRepository secRepo;
    private final AttemptGuardRepository guardRepo;
    private final AttemptEventRepository eventRepo;
    private final ExamRepository examRepo;
    private final ExamAttemptRepository attemptRepo;
    private final Random rnd = new SecureRandom();

    public ExamGuardService(ExamSecurityRepository secRepo, AttemptGuardRepository guardRepo,
                            AttemptEventRepository eventRepo, ExamRepository examRepo,
                            ExamAttemptRepository attemptRepo) {
        this.secRepo = secRepo;
        this.guardRepo = guardRepo;
        this.eventRepo = eventRepo;
        this.examRepo = examRepo;
        this.attemptRepo = attemptRepo;
    }

    // ------------------------------------------------------------------ settings
    public static class Settings {
        private final boolean strict, blockCopy, shuffle;
        private final int maxWarnings;
        private final Integer poolSize, perQuestionSeconds;
        Settings(boolean strict, int maxWarnings, boolean blockCopy, boolean shuffle, Integer poolSize, Integer perQuestionSeconds) {
            this.strict = strict;
            this.maxWarnings = maxWarnings;
            this.blockCopy = blockCopy;
            this.shuffle = shuffle;
            this.poolSize = poolSize;
            this.perQuestionSeconds = perQuestionSeconds;
        }
        public boolean isStrict() { return strict; }
        public int getMaxWarnings() { return maxWarnings; }
        public boolean isBlockCopy() { return blockCopy; }
        public boolean isShuffle() { return shuffle; }
        public Integer getPoolSize() { return poolSize; }
        public Integer getPerQuestionSeconds() { return perQuestionSeconds; }
        public boolean isPerQuestion() { return perQuestionSeconds != null; }
    }

    @Transactional(readOnly = true)
    public Settings settingsOf(Long examId) {
        ExamSecurity s = secRepo.findByExamId(examId).orElse(null);
        if (s == null) return new Settings(true, 3, true, true, null, null);     // defaults for every online exam
        return new Settings(s.isStrictMode(), s.getMaxWarnings(), s.isBlockCopy(), s.isShuffle(),
                s.getPoolSize(), s.getPerQuestionSeconds());
    }

    @Transactional
    public void saveSettings(Long examId, Long teacherId, boolean strict, int maxWarnings, boolean blockCopy,
                             boolean shuffle, Integer poolSize, Integer perQuestionSeconds) {
        Exam e = examRepo.findById(examId).orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!e.getSubject().getTeacher().getId().equals(teacherId)) throw new IllegalArgumentException("This exam does not belong to you");
        if (e.getStatus() != ExamStatus.DRAFT) throw new IllegalArgumentException("Settings cannot be changed after publishing");
        if (maxWarnings < 2 || maxWarnings > 5) throw new IllegalArgumentException("Warnings must be between 2 and 5");
        if (poolSize != null && poolSize < 1) throw new IllegalArgumentException("Pool size must be at least 1");
        if (perQuestionSeconds != null && (perQuestionSeconds < 15 || perQuestionSeconds > 600))
            throw new IllegalArgumentException("Time per question must be between 15 and 600 seconds");
        ExamSecurity s = secRepo.findByExamId(examId).orElseGet(() -> {
            ExamSecurity n = new ExamSecurity();
            n.setExam(e);
            return n;
        });
        s.setStrictMode(strict);
        s.setMaxWarnings(maxWarnings);
        s.setBlockCopy(blockCopy);
        s.setShuffle(shuffle);
        s.setPoolSize(poolSize);
        s.setPerQuestionSeconds(perQuestionSeconds);
        secRepo.save(s);
    }

    // ------------------------------------------------------------------ personal paper
    @Transactional
    public AttemptGuard getOrAssign(ExamAttempt attempt) {
        AttemptGuard g = guardRepo.findByAttemptId(attempt.getId()).orElse(null);
        if (g != null) return g;
        Exam exam = attempt.getExam();
        Settings s = settingsOf(exam.getId());
        List<ExamQuestion> all = new ArrayList<>(exam.getQuestions());
        List<ExamQuestion> chosen = new ArrayList<>(all);
        if (s.getPoolSize() != null && s.getPoolSize() < all.size()) {
            List<ExamQuestion> tmp = new ArrayList<>(all);
            Collections.shuffle(tmp, rnd);
            chosen = new ArrayList<>(tmp.subList(0, s.getPoolSize()));
            chosen.sort((a, b) -> Integer.compare(all.indexOf(a), all.indexOf(b)));
        }
        if (s.isShuffle()) Collections.shuffle(chosen, rnd);

        StringBuilder order = new StringBuilder();
        StringBuilder opts = new StringBuilder();
        for (ExamQuestion q : chosen) {
            if (order.length() > 0) order.append(',');
            order.append(q.getId());
            if (s.isShuffle() && q.getType() == QuestionType.MCQ) {
                List<String> letters = new ArrayList<>();
                for (String L : new String[] {"A", "B", "C", "D"}) {
                    String t = q.optionText(L);
                    if (t != null && !t.isBlank()) letters.add(L);
                }
                Collections.shuffle(letters, rnd);
                if (opts.length() > 0) opts.append(';');
                opts.append(q.getId()).append(':').append(String.join("", letters));
            }
        }
        g = new AttemptGuard();
        g.setAttempt(attempt);
        g.setViolations(0);
        g.setAutoSubmitted(false);
        g.setQuestionOrder(order.toString());
        g.setOptionOrders(opts.toString());
        g.setQIndex(0);
        g.setQStartedAt(LocalDateTime.now());
        return guardRepo.save(g);
    }

    /** the questions this student must answer (all questions when no personal paper exists) */
    @Transactional(readOnly = true)
    public List<ExamQuestion> questionsFor(ExamAttempt attempt) {
        AttemptGuard g = guardRepo.findByAttemptId(attempt.getId()).orElse(null);
        List<ExamQuestion> all = attempt.getExam().getQuestions();
        if (g == null || g.getQuestionOrder() == null || g.getQuestionOrder().isBlank()) return new ArrayList<>(all);
        Map<Long, ExamQuestion> byId = new HashMap<>();
        for (ExamQuestion q : all) byId.put(q.getId(), q);
        List<ExamQuestion> out = new ArrayList<>();
        for (String id : g.getQuestionOrder().split(",")) {
            ExamQuestion q = byId.get(Long.valueOf(id));
            if (q != null) out.add(q);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public int paperTotal(ExamAttempt attempt) {
        int t = 0;
        for (ExamQuestion q : questionsFor(attempt)) t += q.getMarks();
        return t;
    }

    public static class Opt {
        private final String label, value, text;
        Opt(String label, String value, String text) {
            this.label = label;
            this.value = value;
            this.text = text;
        }
        public String getLabel() { return label; }
        public String getValue() { return value; }
        public String getText() { return text; }
    }

    public static class PaperQ {
        private final ExamQuestion question;
        private final int number;
        private final List<Opt> options;
        PaperQ(ExamQuestion question, int number, List<Opt> options) {
            this.question = question;
            this.number = number;
            this.options = options;
        }
        public ExamQuestion getQuestion() { return question; }
        public int getNumber() { return number; }
        public List<Opt> getOptions() { return options; }
    }

    /** the paper as this student sees it (shown labels A-D by position, submitted value = original letter) */
    @Transactional
    public List<PaperQ> paper(ExamAttempt attempt) {
        AttemptGuard g = getOrAssign(attempt);
        Map<Long, String> orders = new HashMap<>();
        if (g.getOptionOrders() != null && !g.getOptionOrders().isBlank()) {
            for (String part : g.getOptionOrders().split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2) orders.put(Long.valueOf(kv[0]), kv[1]);
            }
        }
        List<PaperQ> out = new ArrayList<>();
        int n = 1;
        for (ExamQuestion q : questionsFor(attempt)) {
            List<Opt> opts = new ArrayList<>();
            if (q.getType() == QuestionType.MCQ) {
                String order = orders.get(q.getId());
                if (order == null) {
                    StringBuilder sb = new StringBuilder();
                    for (String L : new String[] {"A", "B", "C", "D"}) {
                        String t = q.optionText(L);
                        if (t != null && !t.isBlank()) sb.append(L);
                    }
                    order = sb.toString();
                }
                String[] labels = {"A", "B", "C", "D"};
                for (int i = 0; i < order.length(); i++) {
                    String L = String.valueOf(order.charAt(i));
                    opts.add(new Opt(labels[i], L, q.optionText(L)));
                }
            }
            out.add(new PaperQ(q, n++, opts));
        }
        return out;
    }

    // ------------------------------------------------------------------ warnings + log
    public static class Outcome {
        private final int count, max;
        private final boolean autoSubmit;
        Outcome(int count, int max, boolean autoSubmit) {
            this.count = count;
            this.max = max;
            this.autoSubmit = autoSubmit;
        }
        public int getCount() { return count; }
        public int getMax() { return max; }
        public boolean isAutoSubmit() { return autoSubmit; }
    }

    @Transactional
    public Outcome record(Long attemptId, Long studentId, String type, String detail) {
        ExamAttempt a = attemptRepo.findById(attemptId).orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        if (!a.getStudent().getId().equals(studentId)) throw new IllegalArgumentException("Not your exam");
        Settings s = settingsOf(a.getExam().getId());
        AttemptGuard g = getOrAssign(a);
        if (a.getStatus() != AttemptStatus.IN_PROGRESS || type == null) {
            return new Outcome(g.getViolations(), s.getMaxWarnings(), false);
        }
        boolean counted = false;
        if (COUNTED.contains(type)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last = g.getLastViolationAt();
            if (s.isStrict() && (last == null || Duration.between(last, now).toMillis() > 2000)) {
                g.setViolations(g.getViolations() + 1);
                g.setLastViolationAt(now);
                guardRepo.save(g);
                counted = true;
            } else {
                return new Outcome(g.getViolations(), s.getMaxWarnings(), s.isStrict() && g.getViolations() >= s.getMaxWarnings());
            }
        } else if (!LOGGED_ONLY.contains(type)) {
            return new Outcome(g.getViolations(), s.getMaxWarnings(), false);
        }
        if (counted || eventRepo.countByAttemptId(attemptId) < MAX_EVENTS) {
            AttemptEvent ev = new AttemptEvent();
            ev.setAttempt(a);
            ev.setType(type);
            ev.setAt(LocalDateTime.now());
            ev.setCounted(counted);
            ev.setDetail(detail == null ? null : (detail.length() > 100 ? detail.substring(0, 100) : detail));
            eventRepo.save(ev);
        }
        return new Outcome(g.getViolations(), s.getMaxWarnings(), s.isStrict() && g.getViolations() >= s.getMaxWarnings());
    }

    /** called when the exam is submitted: remembers that it was forced by too many warnings */
    @Transactional
    public void markFinal(Long attemptId) {
        ExamAttempt a = attemptRepo.findById(attemptId).orElse(null);
        if (a == null) return;
        Settings s = settingsOf(a.getExam().getId());
        AttemptGuard g = guardRepo.findByAttemptId(attemptId).orElse(null);
        if (g != null && s.isStrict() && g.getViolations() >= s.getMaxWarnings() && !g.isAutoSubmitted()) {
            g.setAutoSubmitted(true);
            guardRepo.save(g);
        }
    }

    // ------------------------------------------------------------------ per-question timing (server clock)
    public static class QState {
        private final int index, total, remaining;
        private final boolean finished;
        QState(int index, int total, int remaining, boolean finished) {
            this.index = index;
            this.total = total;
            this.remaining = remaining;
            this.finished = finished;
        }
        public int getIndex() { return index; }
        public int getTotal() { return total; }
        public int getRemaining() { return remaining; }
        public boolean isFinished() { return finished; }
    }

    private QState stateOf(AttemptGuard g, int perQ, int total) {
        long elapsed = Math.max(0, Duration.between(g.getQStartedAt(), LocalDateTime.now()).getSeconds());
        long steps = elapsed / perQ;
        long raw = g.getQIndex() + steps;
        boolean finished = raw >= total;
        int idx = (int) Math.min(total - 1, raw);
        int rem = finished ? 0 : (int) (perQ - (elapsed % perQ));
        return new QState(idx, total, rem, finished);
    }

    @Transactional
    public QState state(ExamAttempt attempt) {
        Settings s = settingsOf(attempt.getExam().getId());
        if (!s.isPerQuestion()) return null;
        AttemptGuard g = getOrAssign(attempt);
        return stateOf(g, s.getPerQuestionSeconds(), questionsFor(attempt).size());
    }

    /** student pressed "Next" (or the question time ran out): move on, never back */
    @Transactional
    public QState advance(Long attemptId, Long studentId) {
        ExamAttempt a = attemptRepo.findById(attemptId).orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        if (!a.getStudent().getId().equals(studentId)) throw new IllegalArgumentException("Not your exam");
        Settings s = settingsOf(a.getExam().getId());
        if (!s.isPerQuestion()) throw new IllegalArgumentException("Not a timed-question exam");
        AttemptGuard g = getOrAssign(a);
        int total = questionsFor(a).size();
        QState cur = stateOf(g, s.getPerQuestionSeconds(), total);
        if (!cur.isFinished() && cur.getIndex() < total - 1) {
            g.setQIndex(cur.getIndex() + 1);
            g.setQStartedAt(LocalDateTime.now());
            guardRepo.save(g);
            return stateOf(g, s.getPerQuestionSeconds(), total);
        }
        return new QState(total - 1, total, 0, true);
    }

    // ------------------------------------------------------------------ for the teacher pages
    @Transactional(readOnly = true)
    public int violationsOf(Long attemptId) {
        return guardRepo.findByAttemptId(attemptId).map(AttemptGuard::getViolations).orElse(0);
    }

    @Transactional(readOnly = true)
    public boolean autoSubmitted(Long attemptId) {
        return guardRepo.findByAttemptId(attemptId).map(AttemptGuard::isAutoSubmitted).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<AttemptEvent> eventsOf(Long attemptId) {
        return eventRepo.findByAttemptIdOrderByIdAsc(attemptId);
    }

    @Transactional(readOnly = true)
    public int questionCountOf(Long attemptId) {
        AttemptGuard g = guardRepo.findByAttemptId(attemptId).orElse(null);
        if (g == null || g.getQuestionOrder() == null || g.getQuestionOrder().isBlank()) return -1;
        return g.getQuestionOrder().split(",").length;
    }
}
