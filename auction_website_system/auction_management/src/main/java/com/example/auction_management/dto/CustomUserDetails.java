//package com.example.auction_management.dto;
//
//import lombok.Getter;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.Collection;
//import java.util.List;
//@Getter
//public class CustomUserDetails implements UserDetails {
//    private final String username;
//    private final String password;
//    private final Long customerId; // Thêm trường customerId
//    private final Collection<? extends GrantedAuthority> authorities;
//
//    public CustomUserDetails(String username, String password, Long customerId, Collection<? extends GrantedAuthority> authorities) {
//        this.username = username;
//        this.password = password;
//        this.customerId = customerId;
//        this.authorities = authorities;
//    }
//
//    @Override public boolean isAccountNonExpired() { return true; }
//    @Override public boolean isAccountNonLocked() { return true; }
//    @Override public boolean isCredentialsNonExpired() { return true; }
//    @Override public boolean isEnabled() { return true; }
//}
