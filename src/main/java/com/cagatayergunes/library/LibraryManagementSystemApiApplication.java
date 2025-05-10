package com.cagatayergunes.library;

import com.cagatayergunes.library.model.Role;
import com.cagatayergunes.library.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
public class LibraryManagementSystemApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryManagementSystemApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(RoleRepository roleRepository){
		return args -> {
			if(roleRepository.findByName("PATRON").isEmpty()){
				roleRepository.save(
						Role.builder().name("PATRON").build()
				);
			}
			if(roleRepository.findByName("LIBRARIAN").isEmpty()){
				roleRepository.save(
						Role.builder().name("LIBRARIAN").build()
				);
			}
		};
	}

}
