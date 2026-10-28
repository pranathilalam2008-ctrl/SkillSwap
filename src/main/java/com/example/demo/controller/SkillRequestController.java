package com.example.demo.controller;

import com.example.demo.model.Skill;
import com.example.demo.model.SkillRequest;
import com.example.demo.model.Student;
import com.example.demo.repository.SkillRepository;
import com.example.demo.repository.SkillRequestRepository;
import com.example.demo.repository.StudentRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/requests")
public class SkillRequestController {

    private final SkillRequestRepository requestRepository;
    private final SkillRepository skillRepository;
    private final StudentRepository studentRepository;

    public SkillRequestController(
            SkillRequestRepository requestRepository,
            SkillRepository skillRepository,
            StudentRepository studentRepository) {

        this.requestRepository = requestRepository;
        this.skillRepository = skillRepository;
        this.studentRepository = studentRepository;
    }

    public record RequestResponse(
            Long id,
            String senderName,
            String receiverName,
            String skillName,
            String status
    ) {}

    private RequestResponse toResponse(SkillRequest request) {
        return new RequestResponse(
                request.getId(),
                request.getSender().getFullName(),
                request.getReceiver().getFullName(),
                request.getSkill().getSkillName(),
                request.getStatus()
        );
    }

    private Long loggedInStudentId(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        Object id = session.getAttribute("studentId");

        return id instanceof Long ? (Long) id : null;
    }

    // Send request
    @PostMapping("/send/{skillId}")
    public ResponseEntity<?> sendRequest(
            @PathVariable Long skillId,
            HttpServletRequest httpRequest) {

        Long senderId = loggedInStudentId(httpRequest);

        if (senderId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        Optional<Student> sender =
                studentRepository.findById(senderId);

        Optional<Skill> skill =
                skillRepository.findById(skillId);

        if (sender.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Student not found"));
        }

        if (skill.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Skill not found"));
        }

        Student receiver = skill.get().getStudent();

        if (receiver.getId().equals(senderId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "You cannot send a request to yourself"
                    ));
        }

        boolean duplicate =
                requestRepository.existsBySenderIdAndSkillIdAndStatus(
                        senderId, skillId, "PENDING"
                );

        if (duplicate) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "You already sent a pending request"
                    ));
        }

        SkillRequest newRequest = new SkillRequest();

        newRequest.setSender(sender.get());
        newRequest.setReceiver(receiver);
        newRequest.setSkill(skill.get());
        newRequest.setStatus("PENDING");

        SkillRequest saved = requestRepository.save(newRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    // Received requests
    @GetMapping("/received")
    public ResponseEntity<?> receivedRequests(
            HttpServletRequest httpRequest) {

        Long studentId = loggedInStudentId(httpRequest);

        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        List<RequestResponse> result = requestRepository
                .findByReceiverIdOrderByIdDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Sent requests
    @GetMapping("/sent")
    public ResponseEntity<?> sentRequests(
            HttpServletRequest httpRequest) {

        Long studentId = loggedInStudentId(httpRequest);

        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        List<RequestResponse> result = requestRepository
                .findBySenderIdOrderByIdDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    // Accept or reject a received request
    @PostMapping("/{requestId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {

        Long studentId = loggedInStudentId(httpRequest);

        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        Optional<SkillRequest> found =
                requestRepository.findById(requestId);

        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Request not found"));
        }

        SkillRequest skillRequest = found.get();

        if (!skillRequest.getReceiver().getId().equals(studentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied"));
        }

        if (!"PENDING".equals(skillRequest.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Request already processed"));
        }

        String status = body.get("status");

        if (!"ACCEPTED".equals(status) &&
                !"REJECTED".equals(status)) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid status"));
        }

        skillRequest.setStatus(status);

        SkillRequest saved =
                requestRepository.save(skillRequest);

        return ResponseEntity.ok(toResponse(saved));
    }
}