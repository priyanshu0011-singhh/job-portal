package com.ncst.job.portal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.ncst.job.portal.entities.Application;
import com.ncst.job.portal.entities.ApplicationStatus;
import com.ncst.job.portal.entities.Job;
import com.ncst.job.portal.entities.User;
import com.ncst.job.portal.loadouts.ApplicationDto;
import com.ncst.job.portal.repository.ApplicationRepo;
import com.ncst.job.portal.repository.JobRepo;
import com.ncst.job.portal.service.UserService;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepo applicationRepo;

    @Mock
    private JobRepo jobRepo;

    @Mock
    private UserService userService;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void newApplicationStartsAsAppliedRegardlessOfClientStatus() {
        Principal principal = () -> "candidate@example.com";
        User candidate = new User();
        candidate.setId("candidate-id");
        Job job = new Job();
        job.setJobId("job-id");
        job.setActive(true);
        ApplicationDto request = new ApplicationDto();
        request.setJobId("job-id");
        request.setApplicantId("another-user-id");
        request.setApplicationStatus(ApplicationStatus.ACCEPTED);

        when(jobRepo.findById("job-id")).thenReturn(Optional.of(job));
        when(userService.getUserFromPrincipal(principal)).thenReturn(candidate);
        when(applicationRepo.findByApplicantId("candidate-id")).thenReturn(List.of());
        when(applicationRepo.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationDto created = applicationService.applyToJob(request, principal);

        assertEquals("candidate-id", created.getApplicantId());
        assertEquals(ApplicationStatus.APPLIED, created.getApplicationStatus());
    }

    @Test
    void applicantCannotSetApplicationStatus() {
        Principal principal = () -> "candidate@example.com";
        User candidate = new User();
        candidate.setId("candidate-id");
        Application existing = new Application();
        existing.setId("application-id");
        existing.setApplicant(candidate);
        existing.setApplicationStatus(ApplicationStatus.APPLIED);
        ApplicationDto request = new ApplicationDto();
        request.setApplicationStatus(ApplicationStatus.ACCEPTED);

        when(applicationRepo.findById("application-id")).thenReturn(Optional.of(existing));
        when(userService.getUserFromPrincipal(principal)).thenReturn(candidate);

        assertThrows(AccessDeniedException.class,
                () -> applicationService.updateApplication(request, "application-id", principal));
        verify(applicationRepo, never()).save(any(Application.class));
    }

    @Test
    void applicantCannotAccessAnotherUsersResumeByAttachingItsPath() {
        Principal principal = () -> "candidate@example.com";
        User candidate = new User();
        candidate.setId("candidate-id");
        User resumeOwner = new User();
        resumeOwner.setId("resume-owner-id");
        Application application = new Application();
        application.setApplicant(candidate);
        application.setResumeLink("/api/files/resumes/resume-owner-id_file.pdf");

        when(userService.getUserFromPrincipal(principal)).thenReturn(candidate);
        when(applicationRepo.findByResumeLink(application.getResumeLink())).thenReturn(List.of(application));

        assertEquals(false, applicationService.canAccessResume("resume-owner-id_file.pdf", principal));
    }
}