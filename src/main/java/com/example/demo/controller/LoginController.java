package com.example.demo.controller;

import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;

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
            @Valid @RequestBody LoginRequest request) {

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

        return ResponseEntity.ok(Map.of(
            "message", "Login successful!"
        ));
    }
}