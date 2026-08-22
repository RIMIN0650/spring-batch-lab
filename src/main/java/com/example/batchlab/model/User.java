package com.example.batchlab.model;



import com.example.batchlab.enums.Role;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "user")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_idx")
    private Long idx;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "tel", nullable = false)
    private String tel;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    // 가맹점 수정 시 회원 이름과 이메일 수정
    public void updateOwner(String name, String email) {
        this.userName = name; //
        this.email = email;
    }

}