package com.smartbank.service.impl;

import com.smartbank.entity.Users;
import com.smartbank.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /*Map user to granted authorities (roles/permissions)*/
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users appUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<GrantedAuthority> authorities = appUser.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                appUser.getUsername(),
                appUser.getPassword(),
                appUser.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }
}

/*1️⃣ Authentication  →  (User identity validated)
  2️⃣ Authorization   →  (Check what user is allowed to do)
*/