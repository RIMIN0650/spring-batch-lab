package com.example.batchlab.service;

import com.example.batchlab.model.UserResponseDto;
import com.example.batchlab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public Page<UserResponseDto> getUserList(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponseDto::from);
    }
}
