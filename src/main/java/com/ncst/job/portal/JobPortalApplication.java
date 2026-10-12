
package com.ncst.job.portal;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ncst.job.portal.entities.Role;
import com.ncst.job.portal.entities.RoleName;
import com.ncst.job.portal.entities.User;
import com.ncst.job.portal.repository.RoleRepo;
import com.ncst.job.portal.repository.UserRepo;

@SpringBootApplication
public class JobPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobPortalApplication.class, args);
        
    }
    @Bean
    CommandLineRunner roleSeeder(RoleRepo roleRepo, UserRepo userRepo, PasswordEncoder passwordEncoder,
                                 Environment environment) {
        return args -> {
            for (RoleName rn : RoleName.values()) {
                if (roleRepo.findByName(rn).isEmpty()) {
                    roleRepo.save(new Role(rn));
                    System.out.println("Inserted role -> " + rn);
                }
            }
            System.out.println("RoleDataSeeder completed. Role count = " + roleRepo.count());

            if (!environment.getProperty("app.admin.bootstrap-enabled", Boolean.class, false)) {
                return;
            }

            String email = environment.getProperty("app.admin.email", "").trim().toLowerCase(Locale.ROOT);
            String password = environment.getProperty("app.admin.password", "");
            String name = environment.getProperty("app.admin.name", "").trim();
            String contactNumber = environment.getProperty("app.admin.contact-number", "").trim();
            if (email.isBlank() || name.isBlank() || contactNumber.isBlank() || password.length() < 16) {
                throw new IllegalStateException("Admin bootstrap requires email, name, contact number, and a password of at least 16 characters");
            }

            var existingAdmin = userRepo.findByEmail(email);
            if (existingAdmin.isPresent()) {
                boolean alreadyAdmin = existingAdmin.get().getRoles().stream()
                        .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
                if (!alreadyAdmin) {
                    throw new IllegalStateException("Admin bootstrap email already belongs to a non-admin user");
                }
                return;
            }

            User admin = new User();
            admin.setId(UUID.randomUUID().toString());
            admin.setUsername(name);
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setContactNumber(contactNumber);
            admin.setRoles(new HashSet<>(java.util.Set.of(
                    roleRepo.findByName(RoleName.ROLE_ADMIN).orElseThrow())));
            userRepo.save(admin);
        };
    }
}


