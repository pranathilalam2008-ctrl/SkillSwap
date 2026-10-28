package com.example.demo.repository;

import com.example.demo.model.SkillRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRequestRepository
        extends JpaRepository<SkillRequest, Long> {

    // Requests received by a student
    List<SkillRequest> findByReceiverIdOrderByIdDesc(Long receiverId);

    // Requests sent by a student
    List<SkillRequest> findBySenderIdOrderByIdDesc(Long senderId);

    // Prevent duplicate pending requests
    boolean existsBySenderIdAndSkillIdAndStatus(
            Long senderId,
            Long skillId,
            String status
    );
}