package com.sanskaracademy.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.NotificationClient;

/** "Message settings": each person decides whether to get messages, how, which types, and in which language. */
@Controller
@RequestMapping("/account/messages")
public class MessageSettingsController {

    private final NotificationClient client;
    private final UserRepository users;

    public MessageSettingsController(NotificationClient client, UserRepository users) {
        this.client = client;
        this.users = users;
    }

    /** which message types make sense for which role: code -> label */
    private Map<String, String> typesFor(String role) {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        switch (role) {
            case "PARENT":
                m.put("FEE", "Fees (due, overdue, payment received)");
                m.put("EXAM", "Exams (schedule, reminder, result)");
                m.put("ATTENDANCE", "Absent alerts");
                m.put("CLASS", "Extra classes");
                m.put("WISH", "Festival wishes");
                break;
            case "STUDENT":
                m.put("EXAM", "Exams (schedule, reminder, result)");
                m.put("CLASS", "Extra classes");
                m.put("WISH", "Birthday and festival wishes");
                break;
            default:
                m.put("STAFF", "Leave and salary updates");
                m.put("CLASS", "Extra classes");
                m.put("WISH", "Birthday and festival wishes");
        }
        return m;
    }

    @GetMapping
    public String page(Principal principal, Model model) {
        User u = users.findByUsername(principal.getName()).orElseThrow();
        String role = u.getRole().getName();
        model.addAttribute("role", role);
        model.addAttribute("types", typesFor(role));
        model.addAttribute("serviceOn", client.isEnabled());
        if (client.isEnabled()) {
            try {
                model.addAttribute("pref", client.loadPreferences(u));
            } catch (Exception e) {
                model.addAttribute("error", "The message service is not reachable right now. Please try again later.");
                model.addAttribute("serviceOn", false);
            }
        }
        return "account-messages";
    }

    @PostMapping
    public String save(Principal principal,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       @RequestParam(defaultValue = "EN") String language,
                       @RequestParam(defaultValue = "false") boolean emailOn,
                       @RequestParam(defaultValue = "false") boolean smsOn,
                       @RequestParam(defaultValue = "false") boolean whatsappOn,
                       @RequestParam(required = false) List<String> categories,
                       RedirectAttributes ra) {
        User u = users.findByUsername(principal.getName()).orElseThrow();
        Map<String, String> allowed = typesFor(u.getRole().getName());
        List<String> cats = new ArrayList<>();
        if (categories != null) {
            for (String c : categories) if (allowed.containsKey(c)) cats.add(c);   // only types that fit this role
        }
        Map<String, Object> body = new HashMap<>();
        body.put("enabled", enabled);
        body.put("language", language);
        body.put("emailOn", emailOn);
        body.put("smsOn", smsOn);
        body.put("whatsappOn", whatsappOn);
        body.put("categories", cats);
        try {
            client.savePreferences(u, body);
            ra.addFlashAttribute("success", "Your message settings are saved");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            ra.addFlashAttribute("error", "Could not save: " + readError(e));
        } catch (Exception e) {
            ra.addFlashAttribute("error", "The message service is not reachable right now. Please try again later.");
        }
        return "redirect:/account/messages";
    }

    private String readError(org.springframework.web.client.HttpClientErrorException e) {
        String b = e.getResponseBodyAsString();
        int i = b.indexOf("\"error\":\"");
        if (i >= 0) {
            int j = b.indexOf('"', i + 9);
            if (j > i) return b.substring(i + 9, j);
        }
        return "please check your choices";
    }
}
