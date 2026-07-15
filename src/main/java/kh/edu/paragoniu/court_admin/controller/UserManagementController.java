package kh.edu.paragoniu.court_admin.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kh.edu.paragoniu.court_admin.service.UserCreationSevice;
import kh.edu.paragoniu.court_admin.service.UserDeletionService;
import kh.edu.paragoniu.court_admin.service.UserManagementService;
import kh.edu.paragoniu.court_admin.service.UserUpdateService;
import kh.edu.paragoniu.court_admin.service.UserManagementService.UserState;
import kh.edu.paragoniu.court_shared.dto.user.UserDTO;
import kh.edu.paragoniu.court_shared.dto.user.UserPageResultDTO;
import kh.edu.paragoniu.court_shared.repository.SystemRoleRepository;


@Controller
public class UserManagementController {

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private SystemRoleRepository systemRoleRepository;

    @Autowired
    private UserCreationSevice userCreationSevice;

    @Autowired
    private UserUpdateService userUpdateService;

    @Autowired
    private UserDeletionService userDeletionService;
    
    private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    @GetMapping("/admin/users")
    public String userManagement(
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "1") int page,
        Model model
    ) {

        UserPageResultDTO pageResult = userManagementService.search(q, page);
        UserState state = userManagementService.getState();

        model.addAttribute("activeNav", "users");
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("users", pageResult.getUsers());

        model.addAttribute("currentPage", pageResult.getCurrentPage());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("prevPage", Math.max(pageResult.getCurrentPage() -1, 1));
        model.addAttribute("nextPage", Math.max(pageResult.getCurrentPage() + 1, pageResult.getTotalPages()));
        model.addAttribute("hasPrevious", pageResult.isHasPrevious());
        model.addAttribute("hasNext", pageResult.isHasNext());

        model.addAttribute("totalUsers", state.total());
        model.addAttribute("activeUsers", state.active());
        model.addAttribute("inactiveUsers", state.inactive());

        return "admin/user-management";

    }

    @GetMapping("/admin/users/table")
    public String userTableFragment(
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "1") int page,
        Model model
    ) {

        UserPageResultDTO pageResult = userManagementService.search(q, page);

        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("users", pageResult.getUsers());
        model.addAttribute("currentPage", pageResult.getCurrentPage());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("prevPage", Math.max(pageResult.getCurrentPage() - 1, 1));
        model.addAttribute("nextPage", Math.min(pageResult.getCurrentPage() + 1, pageResult.getTotalPages()));
        model.addAttribute("hasPrevious", pageResult.isHasPrevious());
        model.addAttribute("hasNext", pageResult.isHasNext());

        return "admin/user-management :: userTable";
    }

    
    @GetMapping("admin/users/create")
    public String createUser(Model model){
        
        model.addAttribute("activeNav", "users");
        model.addAttribute("availableRoles", systemRoleRepository.findAll());
        return "admin/create-user.html";

    }

    @PostMapping("/admin/users/create")
    public String createUser(
        @RequestParam String username,
        @RequestParam String email,
        @RequestParam String firstName,
        @RequestParam String lastName,
        @RequestParam String password,
        @RequestParam(required = false) String active,   
        @RequestParam("roles") String roleName,          
        @RequestParam(required = false) MultipartFile profileImage,
        RedirectAttributes redirectAttributes
    ) {

        try {
            userCreationSevice.createUser(username, email, firstName, lastName, password, active != null, roleName, profileImage);
        } catch (Exception e) {
            log.error("Failed to create user", e);
            redirectAttributes.addFlashAttribute("error", "could not create user: " + e.getMessage());
            return "redirect:/admin/users/create";
        }

        return "redirect:/admin/users";
    }

    
    @GetMapping("/admin/users/update/{userId}")
    public String getUpdateUser(
        @PathVariable("userId") UUID userId,
        Model model
    ) {
        UserDTO user = userUpdateService.getUserById(userId);

        model.addAttribute("activeNav", "users");
        model.addAttribute("user", user);
        model.addAttribute("currentRole", user.getRoles().isEmpty() ? null : user.getRoles().get(0));
        model.addAttribute("availableRoles", systemRoleRepository.findAll());

        return "/admin/update-user";
    }

    @PostMapping("/admin/users/update/{userId}")
    public String processUpdateUser(
        @PathVariable UUID userId,
        @RequestParam String email,
        @RequestParam String firstName,
        @RequestParam String lastName,
        @RequestParam(required = false) String active,
        @RequestParam("roles") String roleName,
        @RequestParam(required = false) String newPassword,
        @RequestParam(required = false) MultipartFile profileImage,
        RedirectAttributes redirectAttributes
    ) {
        
        try {
            userUpdateService.updateUser(userId, email, firstName, lastName, active != null, roleName, newPassword, profileImage);
        } catch (Exception e) {
            log.error("Failed to update user {}", userId, e);
            redirectAttributes.addFlashAttribute("error", "Could not update user: " + e.getMessage());

            return "redirect:/adin/users" + "/update" + userId;
        }

        return "redirect:/admin/users";
    }


    @PostMapping("/admin/users/delete/{userId}")
    public String deleteUser(@PathVariable UUID userId, RedirectAttributes redirectAttributes) {
        try {
            userDeletionService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
        } catch (Exception e) {
            log.error("Failed to delete user {}", userId, e);
            redirectAttributes.addFlashAttribute("error", "Could not delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    
}
