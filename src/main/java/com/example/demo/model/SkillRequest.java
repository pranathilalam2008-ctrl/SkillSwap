package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "skill_requests")
public class SkillRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private Student sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "receiver_id")
    private Student receiver;

    @ManyToOne(optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private String status = "PENDING";

    public SkillRequest() {}

    public Long getId() { return id; }

    public Student getSender() { return sender; }
    public void setSender(Student sender) {
        this.sender = sender;
    }

    public Student getReceiver() { return receiver; }
    public void setReceiver(Student receiver) {
        this.receiver = receiver;
    }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
    }
}