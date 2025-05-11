package com.cagatayergunes.library.repository;

import com.cagatayergunes.library.model.Role;
import com.cagatayergunes.library.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName role);
}
