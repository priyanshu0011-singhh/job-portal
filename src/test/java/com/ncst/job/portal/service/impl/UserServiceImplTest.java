package com.ncst.job.portal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ncst.job.portal.entities.Role;
import com.ncst.job.portal.entities.RoleName;
import com.ncst.job.portal.entities.User;
import com.ncst.job.portal.loadouts.UserDto;
import com.ncst.job.portal.repository.RoleRepo;
import com.ncst.job.portal.repository.UserRepo;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private RoleRepo roleRepo;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void publicRegistrationAlwaysAssignsCandidateRole() {
        UserDto request = new UserDto();
        request.setName("Self-registered admin");
        request.setEmail("attacker@example.com");
        request.setPassword("password");
        request.setContactNumber("1234567890");
        request.setRoleNames(Set.of("ROLE_ADMIN", "ROLE_RECRUITER"));

        User mappedUser = new User();
        when(userRepo.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(modelMapper.map(request, User.class)).thenReturn(mappedUser);
        when(roleRepo.findByName(RoleName.ROLE_CANDIDATE))
                .thenReturn(Optional.of(new Role(RoleName.ROLE_CANDIDATE)));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto created = userService.createUser(request);

        assertEquals(Set.of("ROLE_CANDIDATE"), created.getRoleNames());
        verify(roleRepo).findByName(RoleName.ROLE_CANDIDATE);
        verify(userRepo).save(any(User.class));
    }
}