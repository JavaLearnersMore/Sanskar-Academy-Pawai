package com.sanskaracademy.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sanskaracademy.entity.Role;
import com.sanskaracademy.repository.RoleRepository;

/** Makes sure the STAFF role exists (the other roles already exist in the database). */
@Component
public class RoleInitializer implements CommandLineRunner {

    private final RoleRepository roles;

    public RoleInitializer(RoleRepository roles) {
        this.roles = roles;
    }

    @Override
    public void run(String... args) {
        if (roles.findByName("STAFF").isEmpty()) {
            Role r = new Role();
            r.setName("STAFF");
            roles.save(r);
        }
    }
}
