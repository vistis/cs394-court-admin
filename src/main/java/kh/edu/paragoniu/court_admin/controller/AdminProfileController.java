package kh.edu.paragoniu.court_admin.controller;

import java.util.UUID;

import org.hibernate.sql.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import kh.edu.paragoniu.court_admin.config.SecurityConfig;
import kh.edu.paragoniu.court_admin.service.AdminProfileService;
import kh.edu.paragoniu.court_shared.dto.user.UpdateProfileRequestDTO;
import kh.edu.paragoniu.court_shared.dto.user.UserDTO;
import kh.edu.paragoniu.court_shared.entity.User;
import kh.edu.paragoniu.court_shared.repository.UserRepository;

@Controller
public class AdminProfileController {
    
    @Autowired
    private AdminProfileService adminProfileService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/admin/profile")
    public String showProfile(
        Authentication authentication,
        Model model
    ) {
        UUID userId = resolveUserId(authentication);
        UserDTO profile = adminProfileService.getProfile(userId);

        model.addAttribute("activeNav", "profile");
        model.addAttribute("profile", profile);

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", 
                new UpdateProfileRequestDTO(profile.getUsername(), profile.getEmail(), profile.getFirstName(), profile.getLastName()));
        
        }

        return "admin/profile";
    }

    @PostMapping("/admin/profile")
    public String updateProfile(
        Authentication authentication,
        @Valid @ModelAttribute("profileForm") UpdateProfileRequestDTO request,
        BindingResult bindingResult,
        @RequestParam(required = false) MultipartFile profileImage,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        UUID userId = resolveUserId(authentication);

        if (!bindingResult.hasErrors()) {
            userRepository.findByUsername(request.getUsername())
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> bindingResult.rejectValue("username", "duplicate", "Username is already taken"));

            userRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> bindingResult.rejectValue("email", "duplicate", "Email is already in use"));

        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "profile");
            model.addAttribute("profile", adminProfileService.getProfile(userId));
            return "admin/profile";
        }

        adminProfileService.updateProfile(userId, request, profileImage);
        redirectAttributes.addFlashAttribute("success", "Profile Update");

        return "redirect:/admin/profile";
    }

    private UUID resolveUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Authentication user not found " + username));
        
        return user.getUserId();
    }
}
