ONLINE EXAM SAFETY - what is added
==================================
Copy the "src" folder over your project (Replace files), refresh (F5) and restart.
New tables are created automatically (exam_security, attempt_guards, attempt_events).

NEW FILES : entity/ExamSecurity, AttemptGuard, AttemptEvent; their 3 repositories;
            service/ExamGuardService, AiReasoningService; dto/AiReasoningQuestion;
            controller/StudentExamGuardController, TeacherExamSecurityController
CHANGED   (exam-builder is NOT replaced - see MANUAL-STEP-exam-builder.txt): service/impl/ExamServiceImpl, controller/StudentExamController,
            templates student/exam-take, student/exam-result,
            exam-monitor, exam-results, attempt-detail
            (if you changed these yourself, merge by hand)

HOW IT WORKS
1 Tab / window switch: every time the student leaves the exam page it is counted (once per 2 s) and logged.
  When the limit (2-5, default 3) is reached the exam is submitted automatically. Student is asked for full screen.
2 Teacher: Monitor and Results show "left page" counts and "auto-submitted"; the answer page shows the full log.
3 Personal paper: random order of questions and options. "Questions per student" = pool (e.g. 20 questions, 10 each).
4 Copy, cut, paste, drag, right-click and Ctrl+C/V/X/A/P/S/U are blocked and logged.
5 Seconds per question (15-600): one question at a time, no going back; the clock is kept on the server.
6 Teacher exam builder -> "AI thinking questions": why/how, own example, real-life application. Marked by AI with the
  model answer (teacher can change marks). Needs AI_API_KEY like the other AI features.
Settings are on the exam builder page while the exam is DRAFT. Exams without saved settings use defaults:
strict on, 3 warnings, copy block on, shuffle on.

HONEST LIMITS
- A website cannot see a second phone, a book or a person sitting next to the student. This makes cheating harder
  and visible; it does not make it impossible. A warning is a hint, not proof.
- Switching detection depends on the browser (phones and some browsers behave differently); test it once with a student account.
- Per-question timing is checked on the server, but the countdown display is in the browser.
- Not run by the author: please test with a teacher and a student account before a real exam.
