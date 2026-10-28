package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String collegeName;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String skillToTeach;

    @Column(nullable = false)
    private String skillToLearn;

    @Column(nullable = false)
    private String passwordHash;

    public Student() {
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSkillToTeach() {
        return skillToTeach;
    }

    public void setSkillToTeach(String skillToTeach) {
        this.skillToTeach = skillToTeach;
    }

    public String getSkillToLearn() {
        return skillToLearn;
    }

    public void setSkillToLearn(String skillToLearn) {
        this.skillToLearn = skillToLearn;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}