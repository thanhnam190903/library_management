package com.example.library_management.service.impl;

import com.example.library_management.entity.Reader;
import com.example.library_management.entity.Role;
import com.example.library_management.entity.User;
import com.example.library_management.repository.ReaderRepository;
import com.example.library_management.repository.UserRepository;
import com.example.library_management.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@FieldDefaults(level = lombok.AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    ReaderRepository readerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> userOpt = userRepository.findByEmail(username);
        if (userOpt.isPresent()) {
            System.out.println("LOGIN: " + username);
            User user = userOpt.get();
            user.getRoles().forEach(r ->
                    System.out.println(r.getRoleName()));

            String[] authorities = user.getRoles().stream()
                    .map(role -> "ROLE_" + role.getRoleName())
                    .toArray(String[]::new);

            System.out.println(Arrays.toString(authorities));
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .authorities(
                            user.getRoles().stream()
                                    .map(role -> "ROLE_" + role.getRoleName())
                                    .toArray(String[]::new)
                    )
                    .build();
        }

        Reader reader = readerRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        return org.springframework.security.core.userdetails.User
                .withUsername(reader.getEmail())
                .password(reader.getPassword())
                .authorities("ROLE_READER")
                .build();
    }

}
