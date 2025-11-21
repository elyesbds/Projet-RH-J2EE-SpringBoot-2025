package com.example.CY_RH_Springboot.config;

import com.example.CY_RH_Springboot.models.Employee;
import com.example.CY_RH_Springboot.repositories.EmployeeRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);

        if (employeeOpt.isEmpty()) {
            throw new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email);
        }

        Employee employee = employeeOpt.get();

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (employee.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + employee.getRole()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYE"));
        }

        return User.builder()
                .username(employee.getEmail())
                .password(employee.getPassword())
                .authorities(authorities)
                .build();
    }
}