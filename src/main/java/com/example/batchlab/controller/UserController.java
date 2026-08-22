package com.example.batchlab.controller;


import com.example.batchlab.model.UserResponseDto;
import com.example.batchlab.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    public ResponseEntity<Page<UserResponseDto>> getUserList(@PageableDefault(size = 20) Pageable pageable) {
        Page<UserResponseDto> userList = userService.getUserList(pageable);
        return ResponseEntity.ok(userList);
    }
}
