package com.auth.user.service;

import com.auth.user.entity.User;
import com.auth.user.repository.UserRepository;
import com.auth.user.service.model.UserDetailsImpl;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    /** Concrete class of UserDetailsService from Spring Security
     * This method loads a user by their phone number instead of username
     **/
    @Transactional
    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User Not Found with phone number: " + phoneNumber)
                );

        return UserDetailsImpl.build(user);
    }
}
