package com.ncst.job.portal.service;
import java.security.Principal;
import java.util.List;
import com.ncst.job.portal.loadouts.ApplicationDto;
import com.ncst.job.portal.entities.Application;

public interface ApplicationService {

    // create/apply for a job — if principal provided, use that user as applicant
    ApplicationDto applyToJob(ApplicationDto applicationDto, Principal principal);

    // admiN / owner: update application (or update status)
    ApplicationDto updateApplication(ApplicationDto applicationDto, String applicationId, Principal principal);

    // delete
    void deleteApplication(String applicationId, Principal principal);

    // get by id
    ApplicationDto getApplicationById(String applicationId, Principal principal);

    boolean canAccessResume(String filename, Principal principal);

    // list all (paginated )
    List<ApplicationDto> getAllApplications();

    // list by user (applicant)
    List<ApplicationDto> getApplicationsByApplicant(String applicantId, Principal principal);

    // list by job
    List<ApplicationDto> getApplicationsByJob(String jobId, Principal principal);

    // extra helper: return entity if needed
    Application getApplicationEntityById(String id);
    
}

