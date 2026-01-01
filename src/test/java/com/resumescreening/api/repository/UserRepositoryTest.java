package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashed_password");
        user.setFullName("Test User");
        user.setRole(Role.CANDIDATE);

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findByEmail("test@example.com")).isPresent();
    }
}