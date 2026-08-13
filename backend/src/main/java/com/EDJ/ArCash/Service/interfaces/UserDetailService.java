package com.EDJ.ArCash.Service.interfaces;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserDetailService extends UserDetailsService {
    @Override
    UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException;
}
