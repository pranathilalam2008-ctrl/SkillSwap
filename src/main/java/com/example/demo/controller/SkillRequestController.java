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

import java.net.URI;
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
            String status,
            String meetingLink
    ) {}

    private RequestResponse toResponse(SkillRequest request) {
        return new RequestResponse(
                request.getId(),
                request.getSender().getFullName(),
                request.getReceiver().getFullName(),
                request.getSkill().getSkillName(),
                request.getStatus(),
                request.getMeetingLink()
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

    // SEND REQUEST
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

        SkillRequest saved =
                requestRepository.save(newRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    // RECEIVED REQUESTS
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

    // SENT REQUESTS
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

    // ACCEPT OR REJECT REQUEST
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

    // ADD LEARNING SESSION LINK
    @PostMapping("/{requestId}/meeting")
    public ResponseEntity<?> addMeetingLink(
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

        // Only the teacher can add the link
        if (!skillRequest.getReceiver().getId().equals(studentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only the teacher can add a meeting link"));
        }

        // Request must be accepted first
        if (!"ACCEPTED".equals(skillRequest.getStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Accept the request first"));
        }

        String meetingLink = body.get("meetingLink");

        if (meetingLink == null ||
                meetingLink.isBlank() ||
                meetingLink.length() > 500) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Enter a valid meeting link"));
        }

        meetingLink = meetingLink.trim();

        try {
            URI uri = URI.create(meetingLink);

            if (!"https".equalsIgnoreCase(uri.getScheme()) ||
                    uri.getHost() == null ||
                    uri.getUserInfo() != null ||
                    !List.of(
                            "meet.google.com",
                            "teams.microsoft.com",
                            "zoom.us"
                    ).contains(uri.getHost().toLowerCase())) {

                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Use a supported HTTPS meeting link"));
            }

        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid meeting URL"));
        }

        skillRequest.setMeetingLink(meetingLink);

        SkillRequest saved =
                requestRepository.save(skillRequest);

        return ResponseEntity.ok(toResponse(saved));
    }
}