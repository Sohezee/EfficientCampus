package com.example.demo.entity;

import jakarta.persistence.*;


@Entity
@Table(name="user")
public class User {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    int id;

    @Column(name="email")
    String email;
    @Column(name="password")
    String password;

    @Column(name="offering_name_one")
    String offeringNameOne;

    @Column(name="teacher_display_one")
    String teacherDisplayOne;

    @Column(name="offering_name_two")
    String offeringNameTwo;

    @Column(name="teacher_display_two")
    String teacherDisplayTwo;

    public User(String email, String password, String offeringNameOne, String teacherDisplayOne, String offeringNameTwo, String teacherDisplayTwo) {
        this.email = email;
        this.password = password;
        this.offeringNameOne = offeringNameOne;
        this.teacherDisplayOne = teacherDisplayOne;
        this.offeringNameTwo = offeringNameTwo;
        this.teacherDisplayTwo = teacherDisplayTwo;
    }

    public User() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOfferingNameOne() {
        return offeringNameOne;
    }

    public void setOfferingNameOne(String offeringNameOne) {
        this.offeringNameOne = offeringNameOne;
    }

    public String getTeacherDisplayOne() {
        return teacherDisplayOne;
    }

    public void setTeacherDisplayOne(String teacherDisplayOne) {
        this.teacherDisplayOne = teacherDisplayOne;
    }

    public String getOfferingNameTwo() {
        return offeringNameTwo;
    }

    public void setOfferingNameTwo(String offeringNameTwo) {
        this.offeringNameTwo = offeringNameTwo;
    }

    public String getTeacherDisplayTwo() {
        return teacherDisplayTwo;
    }

    public void setTeacherDisplayTwo(String teacherDisplayTwo) {
        this.teacherDisplayTwo = teacherDisplayTwo;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", offeringNameOne='" + offeringNameOne + '\'' +
                ", teacherDisplayOne='" + teacherDisplayOne + '\'' +
                ", offeringNameTwo='" + offeringNameTwo + '\'' +
                ", teacherDisplayTwo='" + teacherDisplayTwo + '\'' +
                '}';
    }
}
