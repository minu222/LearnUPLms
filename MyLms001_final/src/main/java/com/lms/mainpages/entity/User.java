package com.lms.mainpages.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private int user_id;
    private String nickname;
    private String email;
    private String password;
    private String name;
    private String phone;
    private String address;
    private Role role;
    private LocalDate birth_day;
    private Gender gender;
    private Status status;
    private Timestamp created_at;
    private Timestamp updated_at;


    public User(int user_id, String nickname, String name, String password, LocalDate birth_day, String email, String phone, String address) {
        this.user_id = user_id;
        this.nickname = nickname;
        this.name = name;
        this.password = password;
        this.birth_day = birth_day;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public  enum instructors{

    }
    public enum Role {
        STUDENT, INSTRUCTOR, ADMIN
    }

    public enum Gender {
        MALE, FEMALE
    }
    public enum Status {
        ACTIVE, SUSPENDED,DELETED
    }

}