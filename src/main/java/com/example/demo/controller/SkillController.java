package com.example.demo.controller;

import com.example.demo.model.Skill;
import com.example.demo.model.Student;
import com.example.demo.repository.SkillRepository;
import com.example.demo.repository.StudentRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillRepository skillRepository;
    private final StudentRepository studentRepository;

    public SkillController(
            SkillRepository skillRepository,
            StudentRepository studentRepository) {

        this.skillRepository = skillRepository;
        this.studentRepository = studentRepository;
    }

    public record SkillRequest(
            @NotBlank @Size(max = 100) String skillName,
            @NotBlank String skillLevel
    ) {}

    public record SkillResponse(
            Long id,
            Long studentId,
            String studentName,
            String skillName,
            String skillLevel
    ) {}

    private SkillResponse toResponse(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getStudent().getId(),
                skill.getStudent().getFullName(),
                skill.getSkillName(),
                skill.getSkillLevel()
        );
    }

    // Explore all available skills
    @GetMapping
    public ResponseEntity<?> getAllSkills(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null ||
                session.getAttribute("studentId") == null) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        List<SkillResponse> skills = skillRepository
                .findAllByOrderByIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(skills);
    }

    // Add a new skill
    @PostMapping
    public ResponseEntity<?> addSkill(
            @Valid @RequestBody SkillRequest request,
            HttpServletRequest httpRequest) {

        HttpSession session = httpRequest.getSession(false);

        if (session == null ||
                session.getAttribute("studentId") == null) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        Long studentId =
                (Long) session.getAttribute("studentId");

        Optional<Student> student =
                studentRepository.findById(studentId);

        if (student.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Student not found"));
        }

        String skillName = request.skillName().trim();
        String skillLevel = request.skillLevel().trim();

        if (skillName.isEmpty() ||
                !List.of("Beginner", "Intermediate", "Advanced")
                        .contains(skillLevel)) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid skill details"));
        }

        Skill skill = new Skill();

        skill.setStudent(student.get());
        skill.setSkillName(skillName);
        skill.setSkillLevel(skillLevel);

        Skill savedSkill = skillRepository.save(skill);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(savedSkill));
    }
}