package com.ncst.job.portal.service.impl;
import java.security.Principal; 
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ncst.job.portal.entities.Application;
import com.ncst.job.portal.entities.ApplicationStatus;
import com.ncst.job.portal.entities.Job;
import com.ncst.job.portal.entities.RoleName;
import com.ncst.job.portal.entities.User;
import com.ncst.job.portal.loadouts.ApplicationDto;
import com.ncst.job.portal.repository.ApplicationRepo;
import com.ncst.job.portal.repository.JobRepo;
import com.ncst.job.portal.service.ApplicationService;
import com.ncst.job.portal.service.UserService;
import com.ncst.job.portal.globalExceptionHandler.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepo applicationRepo;
    private final JobRepo jobRepo;
    private final UserService userService; // has getUserFromPrincipal helper

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public ApplicationDto applyToJob(ApplicationDto applicationDto, Principal principal) {
        // job must exist
        Job job = jobRepo.findById(applicationDto.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", applicationDto.getJobId()));

        User applicant = userService.getUserFromPrincipal(principal);
        if (applicant == null) {
            throw new AccessDeniedException("An authenticated applicant is required");
        }

        // optional: check job is active
        if (!job.isActive()) {
            throw new IllegalStateException("Cannot apply to an inactive job.");
        }

        // Prevent duplicate application (optional)
        List<Application> existing = applicationRepo.findByApplicantId(applicant.getId()).stream()
                .filter(a -> a.getJob() != null && job.getJobId().equals(a.getJob().getJobId()))
                .collect(Collectors.toList());
        if (!existing.isEmpty()) {
            throw new IllegalStateException("You have already applied to this job.");
        }

        Application app = new Application();
        app.setId(UUID.randomUUID().toString());
        app.setJob(job);
        app.setApplicant(applicant);
        app.setApplicationDate(LocalDateTime.now().format(ISO));
        app.setResumeLink(applicationDto.getResumeLink());
        app.setApplicationStatus(ApplicationStatus.APPLIED);

        Application saved = applicationRepo.save(app);
        return toDto(saved);
    }

    @Override
    public ApplicationDto updateApplication(ApplicationDto applicationDto, String applicationId, Principal principal) {
        Application existing = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        // Who can update? Admin or the poster of the job (employer) or applicant (depending on rule).
        // Here: allow admin OR job.postedBy OR applicant to update certain fields.
        User current = userService.getUserFromPrincipal(principal);
        boolean isOwnerJob = existing.getJob() != null && existing.getJob().getPostedBy() != null && current != null &&
                current.getId().equals(existing.getJob().getPostedBy().getId());
        boolean isApplicant = current != null && current.getId().equals(existing.getApplicant().getId());
        boolean isAdmin = isAdmin(current);
        if (!(isAdmin || isOwnerJob || isApplicant)) {
            throw new AccessDeniedException("Not authorized to update this application");
        }

        if (applicationDto.getApplicationDate() != null) {
            throw new AccessDeniedException("Application date cannot be changed");
        }
        if (applicationDto.getApplicationStatus() != null && !(isAdmin || isOwnerJob)) {
            throw new AccessDeniedException("Only the job owner or an admin can change application status");
        }
        if (applicationDto.getResumeLink() != null && (isAdmin || isApplicant)) {
            existing.setResumeLink(applicationDto.getResumeLink());
        }
        if (applicationDto.getApplicationStatus() != null && (isAdmin || isOwnerJob)) {
            existing.setApplicationStatus(applicationDto.getApplicationStatus());
        }

        Application updated = applicationRepo.save(existing);
        return toDto(updated);
    }

    @Override
    public void deleteApplication(String applicationId, Principal principal) {
        Application existing = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        User current = userService.getUserFromPrincipal(principal);
        boolean isApplicant = current != null && current.getId().equals(existing.getApplicant().getId());
        boolean isJobOwner = current != null && existing.getJob() != null && existing.getJob().getPostedBy() != null
            && current.getId().equals(existing.getJob().getPostedBy().getId());
        if (!(isAdmin(current) || isApplicant || isJobOwner)) {
            throw new AccessDeniedException("Not authorized to delete this application");
        }

        applicationRepo.delete(existing);
    }

    @Override
    public ApplicationDto getApplicationById(String applicationId, Principal principal) {
        Application application = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));
        User current = userService.getUserFromPrincipal(principal);
        boolean isApplicant = current != null && current.getId().equals(application.getApplicant().getId());
        boolean isJobOwner = current != null && application.getJob() != null && application.getJob().getPostedBy() != null
                && current.getId().equals(application.getJob().getPostedBy().getId());
        if (!(isAdmin(current) || isApplicant || isJobOwner)) {
            throw new AccessDeniedException("Not authorized to view this application");
        }
        return toDto(application);
    }

    @Override
    public boolean canAccessResume(String filename, Principal principal) {
        User current = userService.getUserFromPrincipal(principal);
        if (isAdmin(current)) return true;

        String resumeLink = "/api/files/resumes/" + filename;
        return applicationRepo.findByResumeLink(resumeLink).stream().anyMatch(application -> {
            User applicant = application.getApplicant();
            boolean belongsToApplicant = applicant != null && filename.startsWith(applicant.getId() + "_");
            boolean isApplicant = belongsToApplicant && current != null && current.getId().equals(applicant.getId());
            boolean isJobOwner = current != null && application.getJob() != null
                    && application.getJob().getPostedBy() != null
                && current.getId().equals(application.getJob().getPostedBy().getId())
                && belongsToApplicant;
            return isApplicant || isJobOwner;
        });
    }

    @Override
    public List<ApplicationDto> getAllApplications() {
        return applicationRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDto> getApplicationsByApplicant(String applicantId, Principal principal) {
        User current = userService.getUserFromPrincipal(principal);
        if (current == null) throw new AccessDeniedException("Not authorized to view applications");
        if (isAdmin(current)) {
            return applicationRepo.findByApplicantId(applicantId).stream().map(this::toDto).collect(Collectors.toList());
        }
        return applicationRepo.findByApplicantId(applicantId).stream()
                .filter(application -> current.getId().equals(applicantId)
                        || (application.getJob() != null && application.getJob().getPostedBy() != null
                                && current.getId().equals(application.getJob().getPostedBy().getId())))
                .map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ApplicationDto> getApplicationsByJob(String jobId, Principal principal) {
        User current = userService.getUserFromPrincipal(principal);
        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));
        boolean isJobOwner = current != null && job.getPostedBy() != null
                && current.getId().equals(job.getPostedBy().getId());
        if (!(isAdmin(current) || isJobOwner)) {
            throw new AccessDeniedException("Only the job owner or an admin can view these applications");
        }
        return applicationRepo.findByJobJobId(jobId).stream().map(this::toDto).collect(Collectors.toList());
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
    }

    @Override
    public Application getApplicationEntityById(String id) {
        return applicationRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Application", "id", id));
    }

    // helper mapping
    private ApplicationDto toDto(Application app) {
        ApplicationDto d = new ApplicationDto();
        d.setApplicationId(app.getId());
        if (app.getJob() != null) d.setJobId(app.getJob().getJobId());
        if (app.getApplicant() != null) d.setApplicantId(app.getApplicant().getId());
        d.setApplicationDate(app.getApplicationDate());
        d.setResumeLink(app.getResumeLink());
        d.setApplicationStatus(app.getApplicationStatus());
        return d;
    }
}
