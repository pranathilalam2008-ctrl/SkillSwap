package com.example.demo.controller;

import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final StudentRepository studentRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public LoginController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String email = request.email().trim()
                .toLowerCase(Locale.ROOT);

        Optional<Student> student =
                studentRepository.findByEmail(email);

        if (student.isEmpty() ||
            !passwordEncoder.matches(
                request.password(),
                student.get().getPasswordHash()
            )) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "message", "Invalid email or password"
                    ));
        }

        HttpSession oldSession = httpRequest.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();

        session.setAttribute("studentId", student.get().getId());
        session.setMaxInactiveInterval(30 * 60);

        return ResponseEntity.ok(Map.of(
            "message", "Login successful!"
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentStudent(HttpSession session) {

        Long studentId = (Long) session.getAttribute("studentId");

        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        return studentRepository.findById(studentId)
                .<ResponseEntity<?>>map(student -> ResponseEntity.ok(
                          Map.of(
                             "fullName", student.getFullName(),
                              "email", student.getEmail(),
                              "collegeName", student.getCollegeName(),
                              "department", student.getDepartment(),
                              "skillToTeach", student.getSkillToTeach(),
                              "skillToLearn", student.getSkillToLearn()
)
                ))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Student not found")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(Map.of(
            "message", "Logged out successfully"
        ));
    }
}