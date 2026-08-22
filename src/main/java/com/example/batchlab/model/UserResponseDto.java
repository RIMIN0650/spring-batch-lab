package com.example.batchlab.model;

import com.example.batchlab.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long idx;
    private String email;
    private String userName;
    private String tel;
    private Role role;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .idx(user.getIdx())
                .email(user.getEmail())
                .userName(user.getUserName())
                .tel(user.getTel())
                .role(user.getRole())
                .build();
    }
}
