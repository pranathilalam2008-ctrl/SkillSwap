package com.example.demo.controller;

import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final StudentRepository studentRepository;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public RegistrationController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public record RegistrationRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank String collegeName,
            @NotBlank String department,
            @NotBlank String skillToTeach,
            @NotBlank String skillToLearn,
            @NotBlank @Size(min = 8) String password
    ) {}

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegistrationRequest request) {

        String email = request.email().trim().toLowerCase();

        if (studentRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already registered"));
        }

        Student student = new Student();

        student.setFullName(request.fullName().trim());
        student.setEmail(email);
        student.setCollegeName(request.collegeName().trim());
        student.setDepartment(request.department().trim());
        student.setSkillToTeach(request.skillToTeach().trim());
        student.setSkillToLearn(request.skillToLearn().trim());

        // Never store the original password.
        student.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        studentRepository.save(student);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful"));
    }
}