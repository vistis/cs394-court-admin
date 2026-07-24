package kh.edu.paragoniu.court_admin.controller;

import kh.edu.paragoniu.court_admin.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import kh.edu.paragoniu.court_admin.service.AdminProfileService;
import kh.edu.paragoniu.court_shared.dto.user.UserDTO;
import kh.edu.paragoniu.court_shared.entity.User;
import kh.edu.paragoniu.court_shared.repository.UserRepository;

@ControllerAdvice
public class GlobalAdminInfoController {
    
    @Autowired
    private StorageService storageService;

    @Autowired
    private UserRepository userRepository;


    @ModelAttribute("currentAdmin")
    public UserDTO currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        
        String username = authentication.getName();
        
        return userRepository.findByUsername(username)
            .map(user -> new UserDTO(
                user.getUserId(), user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(),
                storageService.getFullUrl(user.getProfilePicturePath()),
                user.isActive(),
                user.getUserRoles().stream().findFirst().map(ur -> ur.getSystemRole().getName()).orElse(null)
            ))
            .orElse(null);
        }
}
