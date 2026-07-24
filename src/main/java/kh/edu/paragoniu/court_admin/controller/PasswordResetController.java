package kh.edu.paragoniu.court_admin.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import kh.edu.paragoniu.court_admin.service.PasswordResetService;
import kh.edu.paragoniu.court_shared.dto.auth.ResetPasswordRequestDTO;
import kh.edu.paragoniu.court_shared.dto.auth.VerifyIdentityRequestDTO;

@Controller
public class PasswordResetController {

    private static final String SESSION_KEY = "passwordResetUserId";

    @Autowired private PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showVerifyForm(Model model) {
        if (!model.containsAttribute("identity")) {
            model.addAttribute("identity", new VerifyIdentityRequestDTO());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String verifyIdentity(
            @Valid @ModelAttribute("identity") VerifyIdentityRequestDTO request,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        Optional<UUID> userId = passwordResetService.verifyIdentity(request.getUsername(), request.getEmail());

        if (userId.isEmpty()) {
            // Deliberately generic — don't reveal whether username or email was the wrong part
            model.addAttribute("error", "No account found matching that username and email.");
            return "auth/forgot-password";
        }

        session.setAttribute(SESSION_KEY, userId.get());
        return "redirect:/forgot-password/reset";
    }

    @GetMapping("/forgot-password/reset")
    public String showResetForm(HttpSession session, Model model) {
        if (session.getAttribute(SESSION_KEY) == null) {
            return "redirect:/forgot-password";
        }
        if (!model.containsAttribute("resetForm")) {
            model.addAttribute("resetForm", new ResetPasswordRequestDTO());
        }
        return "auth/reset-password";
    }

    @PostMapping("/forgot-password/reset")
    public String resetPassword(
            @Valid @ModelAttribute("resetForm") ResetPasswordRequestDTO request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        UUID userId = (UUID) session.getAttribute(SESSION_KEY);
        if (userId == null) {
            return "redirect:/forgot-password";
        }

        if (!bindingResult.hasErrors() && !request.getNewPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        passwordResetService.resetPassword(userId, request.getNewPassword());
        session.removeAttribute(SESSION_KEY); // one-time use — can't reuse this session state to reset again
        redirectAttributes.addFlashAttribute("success", "Password reset. Please log in with your new password.");
        return "redirect:/login";
    }
}