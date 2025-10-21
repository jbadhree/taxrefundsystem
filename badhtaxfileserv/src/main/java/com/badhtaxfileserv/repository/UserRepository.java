package com.badhtaxfileserv.repository;

import com.badhtaxfileserv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByUserId(String userId);
    
    List<User> findAllByOrderByCreatedAtDesc();
    
    boolean existsByUserId(String userId);
}
