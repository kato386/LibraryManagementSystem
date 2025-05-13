package com.cagatayergunes.library;

import com.cagatayergunes.library.model.Role;
import com.cagatayergunes.library.model.RoleName;
import com.cagatayergunes.library.model.User;
import com.cagatayergunes.library.repository.RoleRepository;
import com.cagatayergunes.library.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
public class LibraryManagementSystemApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementSystemApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(RoleRepository roleRepository, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
		return args -> {
			for (RoleName roleName : RoleName.values()) {
				if (roleRepository.findByName(roleName).isEmpty()) {
					roleRepository.save(Role.builder().name(roleName).build());
				}
			}

			if (userRepository.findByFirstName("admin").isEmpty()) {
				Role adminRole = roleRepository.findByName(RoleName.ADMIN)
						.orElseThrow(() -> new IllegalStateException("Missing Admind Role"));

				User admin = new User();
				admin.setFirstName("admin");
				admin.setLastName("admin");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setEmail("admin@example.com");
				admin.setEnabled(true);
				admin.setRoles(List.of(adminRole));

				userRepository.save(admin);
			}
		};
	}

}
