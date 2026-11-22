package com.sanskaracademy.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.DiaryCategory;
import com.sanskaracademy.entity.Homework;
import com.sanskaracademy.entity.QuestionType;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.service.DiaryService;
import com.sanskaracademy.service.DoubtService;
import com.sanskaracademy.service.ExamService;
import com.sanskaracademy.service.HomeworkService;
import com.sanskaracademy.service.MaterialService;
import com.sanskaracademy.service.ProgressService;
import com.sanskaracademy.service.QuestionBankService;
import com.sanskaracademy.service.SchoolAccess;
import com.sanskaracademy.service.SyllabusService;

/** Teacher pages: homework, study material, doubts, syllabus, class diary, question bank, my month. */
@Controller
public class TeacherLearningController {

    private final SchoolAccess access;
    private final HomeworkService homework;
    private final MaterialService materials;
    private final DoubtService doubts;
    private final SyllabusService syllabus;
    private final DiaryService diary;
    private final QuestionBankService bank;
    private final ProgressService progress;
    private final ExamService exams;

    public TeacherLearningController(SchoolAccess access, HomeworkService homework, MaterialService materials,
                                     DoubtService doubts, SyllabusService syllabus, DiaryService diary,
                                     QuestionBankService bank, ProgressService progress, ExamService exams) {
        this.access = access;
        this.homework = homework;
        this.materials = materials;
        this.doubts = doubts;
        this.syllabus = syllabus;
        this.diary = diary;
        this.bank = bank;
        this.progress = progress;
        this.exams = exams;
    }

    private static String back(RedirectAttributes ra, String url, String ok, RuntimeException e) {
        if (e != null) ra.addFlashAttribute("error", e.getMessage());
        else ra.addFlashAttribute("success", ok);
        return "redirect:" + url;
    }

    // ================================================================== homework
    @GetMapping("/teacher/homework")
    public String homeworkList(Model m, Principal p) {
        User t = access.me(p);
        List<Homework> list = homework.ofTeacher(t.getId());
        m.addAttribute("subjects", access.subjectsOfTeacher(t.getId()));
        m.addAttribute("list", list);
        m.addAttribute("counts", homework.countsFor(list));
        m.addAttribute("today", LocalDate.now());
        return "teacher/homework";
    }

    @PostMapping("/teacher/homework")
    public String homeworkCreate(@RequestParam Long subjectId, @RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                                 @RequestParam(required = false) Integer maxMarks,
                                 @RequestParam(required = false) MultipartFile file,
                                 Principal p, RedirectAttributes ra) {
        try {
            homework.create(access.me(p), subjectId, title, description, dueDate, maxMarks, file);
            return back(ra, "/teacher/homework", "Homework given. Students and parents were told.", null);
        } catch (IOException e) {
            return back(ra, "/teacher/homework", null, new IllegalArgumentException("Could not save the file"));
        } catch (RuntimeException e) {
            return back(ra, "/teacher/homework", null, e);
        }
    }

    @GetMapping("/teacher/homework/{id}")
    public String homeworkDetail(@PathVariable Long id, Model m, Principal p, RedirectAttributes ra) {
        try {
            Homework h = homework.ownedBy(id, access.me(p).getId());
            m.addAttribute("h", h);
            m.addAttribute("rows", homework.classRows(h));
            return "teacher/homework-detail";
        } catch (RuntimeException e) {
            return back(ra, "/teacher/homework", null, e);
        }
    }

    @PostMapping("/teacher/homework/submission/{sid}/check")
    public String homeworkCheck(@PathVariable Long sid, @RequestParam Long homeworkId,
                                @RequestParam(required = false) Integer marks,
                                @RequestParam(required = false) String remark, Principal p, RedirectAttributes ra) {
        try {
            homework.check(sid, access.me(p).getId(), marks, remark);
            return back(ra, "/teacher/homework/" + homeworkId, "Saved.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/homework/" + homeworkId, null, e);
        }
    }

    @PostMapping("/teacher/homework/{id}/delete")
    public String homeworkDelete(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            homework.delete(id, access.me(p).getId());
            return back(ra, "/teacher/homework", "Homework deleted.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/homework", null, e);
        }
    }

    // ================================================================== study material
    @GetMapping("/teacher/materials")
    public String materialList(Model m, Principal p) {
        User t = access.me(p);
        m.addAttribute("subjects", access.subjectsOfTeacher(t.getId()));
        m.addAttribute("list", materials.ofTeacher(t.getId()));
        return "teacher/materials";
    }

    @PostMapping("/teacher/materials")
    public String materialAdd(@RequestParam Long subjectId, @RequestParam(required = false) String chapter,
                              @RequestParam String title, @RequestParam(required = false) String description,
                              @RequestParam(required = false) String link,
                              @RequestParam(required = false) MultipartFile file, Principal p, RedirectAttributes ra) {
        try {
            materials.add(access.me(p), subjectId, chapter, title, description, link, file);
            return back(ra, "/teacher/materials", "Material added. Students were told.", null);
        } catch (IOException e) {
            return back(ra, "/teacher/materials", null, new IllegalArgumentException("Could not save the file"));
        } catch (RuntimeException e) {
            return back(ra, "/teacher/materials", null, e);
        }
    }

    @PostMapping("/teacher/materials/{id}/delete")
    public String materialDelete(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            materials.delete(id, access.me(p).getId());
            return back(ra, "/teacher/materials", "Deleted.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/materials", null, e);
        }
    }

    // ================================================================== doubts
    @GetMapping("/teacher/doubts")
    public String doubtList(Model m, Principal p) {
        User t = access.me(p);
        m.addAttribute("list", doubts.ofTeacher(t.getId()));
        m.addAttribute("open", doubts.openCount(t.getId()));
        return "teacher/doubts";
    }

    @PostMapping("/teacher/doubts/{id}/answer")
    public String doubtAnswer(@PathVariable Long id, @RequestParam String answer,
                              @RequestParam(defaultValue = "false") boolean ai, Principal p, RedirectAttributes ra) {
        try {
            doubts.answer(id, access.me(p), answer, ai);
            return back(ra, "/teacher/doubts", "Answer sent.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/doubts", null, e);
        }
    }

    /** puts an AI draft into the answer box; the teacher still reads, edits and sends it */
    @PostMapping("/teacher/doubts/{id}/draft")
    public String doubtDraft(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            String d = doubts.aiDraft(id, access.me(p));
            if (d == null) ra.addFlashAttribute("error", "AI draft is not available right now. Please type the answer.");
            else {
                ra.addFlashAttribute("draftId", id);
                ra.addFlashAttribute("draftText", d);
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/teacher/doubts";
    }

    // ================================================================== syllabus
    @GetMapping("/teacher/syllabus")
    public String syllabusPage(Model m, Principal p) {
        m.addAttribute("list", syllabus.forTeacher(access.me(p).getId()));
        m.addAttribute("today", LocalDate.now());
        return "teacher/syllabus";
    }

    @PostMapping("/teacher/syllabus/add")
    public String syllabusAdd(@RequestParam Long subjectId, @RequestParam String chapters, Principal p, RedirectAttributes ra) {
        try {
            int n = syllabus.addChapters(subjectId, access.me(p).getId(), chapters);
            return back(ra, "/teacher/syllabus", n + " chapter(s) added.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/syllabus", null, e);
        }
    }

    @PostMapping("/teacher/syllabus/{id}/plan")
    public String syllabusPlan(@PathVariable Long id,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planned,
                               Principal p, RedirectAttributes ra) {
        try {
            syllabus.plan(id, access.me(p).getId(), planned);
            return back(ra, "/teacher/syllabus", "Planned date saved.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/syllabus", null, e);
        }
    }

    @PostMapping("/teacher/syllabus/{id}/done")
    public String syllabusDone(@PathVariable Long id, @RequestParam(required = false) String note, Principal p, RedirectAttributes ra) {
        try {
            syllabus.complete(id, access.me(p), note);
            return back(ra, "/teacher/syllabus", "Chapter marked as completed.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/syllabus", null, e);
        }
    }

    @PostMapping("/teacher/syllabus/{id}/reopen")
    public String syllabusReopen(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            syllabus.reopen(id, access.me(p).getId());
            return back(ra, "/teacher/syllabus", "Chapter re-opened.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/syllabus", null, e);
        }
    }

    @PostMapping("/teacher/syllabus/{id}/delete")
    public String syllabusDelete(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            syllabus.delete(id, access.me(p).getId());
            return back(ra, "/teacher/syllabus", "Chapter removed.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/syllabus", null, e);
        }
    }

    // ================================================================== class diary
    @GetMapping("/teacher/diary")
    public String diaryPage(@RequestParam(required = false) Long subjectId, Model m, Principal p) {
        User t = access.me(p);
        List<com.sanskaracademy.entity.Subject> subs = access.subjectsOfTeacher(t.getId());
        m.addAttribute("subjects", subs);
        com.sanskaracademy.entity.Subject pick = null;
        for (com.sanskaracademy.entity.Subject s : subs) if (subjectId != null && s.getId().equals(subjectId)) pick = s;
        if (pick == null && !subs.isEmpty()) pick = subs.get(0);
        m.addAttribute("pick", pick);
        m.addAttribute("students", pick == null ? List.of() : access.studentsOfClass(pick.getClassEntity().getId()));
        m.addAttribute("recent", diary.recentOfTeacher(t.getId()));
        m.addAttribute("categories", DiaryCategory.values());
        return "teacher/diary";
    }

    @PostMapping("/teacher/diary")
    public String diaryAdd(@RequestParam(required = false) List<Long> studentIds, @RequestParam DiaryCategory category,
                           @RequestParam String text, @RequestParam(defaultValue = "false") boolean visible,
                           @RequestParam(required = false) Long subjectId, Principal p, RedirectAttributes ra) {
        String url = "/teacher/diary" + (subjectId == null ? "" : "?subjectId=" + subjectId);
        try {
            int n = diary.add(access.me(p), studentIds, category, text, visible);
            return back(ra, url, "Remark added for " + n + " student(s).", null);
        } catch (RuntimeException e) {
            return back(ra, url, null, e);
        }
    }

    @PostMapping("/teacher/diary/{id}/delete")
    public String diaryDelete(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            diary.delete(id, access.me(p).getId());
            return back(ra, "/teacher/diary", "Removed.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/diary", null, e);
        }
    }

    // ================================================================== question bank
    @GetMapping("/teacher/question-bank")
    public String bankPage(Model m, Principal p) {
        User t = access.me(p);
        m.addAttribute("list", bank.ofTeacher(t.getId()));
        m.addAttribute("subjects", access.subjectsOfTeacher(t.getId()));
        m.addAttribute("exams", exams.getTeacherExams(t.getId()));
        return "teacher/question-bank";
    }

    @PostMapping("/teacher/question-bank")
    public String bankAdd(@RequestParam(required = false) Long subjectId, @RequestParam(required = false) String chapter,
                          @RequestParam QuestionType type, @RequestParam String questionText,
                          @RequestParam(required = false) String optionA, @RequestParam(required = false) String optionB,
                          @RequestParam(required = false) String optionC, @RequestParam(required = false) String optionD,
                          @RequestParam(required = false) String correctOption,
                          @RequestParam(required = false) String modelAnswer,
                          @RequestParam(defaultValue = "1") int marks,
                          @RequestParam(required = false) String difficulty, Principal p, RedirectAttributes ra) {
        try {
            bank.add(access.me(p), subjectId, chapter, type, questionText, optionA, optionB, optionC, optionD,
                    correctOption, modelAnswer, marks, difficulty);
            return back(ra, "/teacher/question-bank", "Question saved to your bank.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/question-bank", null, e);
        }
    }

    @PostMapping("/teacher/question-bank/{id}/delete")
    public String bankDelete(@PathVariable Long id, Principal p, RedirectAttributes ra) {
        try {
            bank.delete(id, access.me(p).getId());
            return back(ra, "/teacher/question-bank", "Deleted.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/question-bank", null, e);
        }
    }

    @PostMapping("/teacher/question-bank/to-exam")
    public String bankToExam(@RequestParam Long examId, @RequestParam(required = false) List<Long> ids, Principal p, RedirectAttributes ra) {
        try {
            int n = bank.addToExam(examId, access.me(p), ids);
            return back(ra, "/teacher/question-bank", n + " question(s) added to the exam.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/question-bank", null, e);
        }
    }

    @PostMapping("/teacher/question-bank/import")
    public String bankImport(@RequestParam Long examId, Principal p, RedirectAttributes ra) {
        try {
            int n = bank.importFromExam(examId, access.me(p));
            return back(ra, "/teacher/question-bank", n + " question(s) saved to your bank.", null);
        } catch (RuntimeException e) {
            return back(ra, "/teacher/question-bank", null, e);
        }
    }

    // ================================================================== my month
    @GetMapping("/teacher/my-month")
    public String myMonth(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                          Model m, Principal p) {
        LocalDate now = LocalDate.now();
        int y = year == null ? now.getYear() : year;
        int mo = month == null || month < 1 || month > 12 ? now.getMonthValue() : month;
        m.addAttribute("mm", progress.myMonth(access.me(p), y, mo));
        m.addAttribute("monthName", java.time.Month.of(mo).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH));
        m.addAttribute("years", List.of(now.getYear() - 1, now.getYear(), now.getYear() + 1));
        m.addAttribute("year", y);
        m.addAttribute("month", mo);
        return "teacher/my-month";
    }
}
