package com.sanskaracademy.dto;

import java.io.Serializable;

/** One line of the student's AI tutor conversation. role = "student" or "tutor". */
public record ChatLine(String role, String text) implements Serializable {
}
