package kh.edu.paragoniu.court_admin.controller;

import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import kh.edu.paragoniu.court_admin.service.UserCreationSevice;
import kh.edu.paragoniu.court_admin.service.UserDeletionService;
import kh.edu.paragoniu.court_admin.service.UserManagementService;
import kh.edu.paragoniu.court_admin.service.UserManagementService.UserState;
import kh.edu.paragoniu.court_admin.service.UserUpdateService;
import kh.edu.paragoniu.court_shared.dto.user.CreateUserRequestDTO;
import kh.edu.paragoniu.court_shared.dto.user.UpdateUserRequestDTO;
import kh.edu.paragoniu.court_shared.dto.user.UserDTO;
import kh.edu.paragoniu.court_shared.dto.user.UserPageResultDTO;
import kh.edu.paragoniu.court_shared.repository.SystemRoleRepository;
import kh.edu.paragoniu.court_shared.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserManagementController {

    @Autowired
    private UserRepository userRepository;

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

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    private static final Logger log = LoggerFactory.getLogger(
        UserManagementController.class
    );

    private static final Set<String> SYSTEM_DEFULT_USERNAME = Set.of(
        "default.admin",
        "default.chief",
        "default.greffier"
    );

    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping("/admin/users")
    public String userManagement(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        Model model
    ) {
        Boolean statusFilter = parseStatusFilter(status);
        UserPageResultDTO pageResult = userManagementService.search(
            q,
            statusFilter,
            page
        );
        UserState state = userManagementService.getState();

        model.addAttribute("activeNav", "users");
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("users", pageResult.getUsers());

        model.addAttribute("currentPage", pageResult.getCurrentPage());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute(
            "prevPage",
            Math.max(pageResult.getCurrentPage() - 1, 1)
        );
        model.addAttribute(
            "nextPage",
            Math.min(
                pageResult.getCurrentPage() + 1,
                pageResult.getTotalPages()
            )
        );
        model.addAttribute("hasPrevious", pageResult.isHasPrevious());
        model.addAttribute("hasNext", pageResult.isHasNext());

        model.addAttribute("totalUsers", state.total());
        model.addAttribute("activeUsers", state.active());
        model.addAttribute("inactiveUsers", state.inactive());

        return "admin/user-management";
    }

    private Boolean parseStatusFilter(String status) {
        if ("active".equalsIgnoreCase(status)) return true;
        if ("inactive".equalsIgnoreCase(status)) return false;
        return null; // anything else (null, "", "all") = no filter
    }

    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping("/admin/users/table")
    public String userTableFragment(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        Model model
    ) {
        Boolean statusFilter = parseStatusFilter(status);
        UserPageResultDTO pageResult = userManagementService.search(
            q,
            statusFilter,
            page
        );

        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("users", pageResult.getUsers());
        model.addAttribute("currentPage", pageResult.getCurrentPage());
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute(
            "prevPage",
            Math.max(pageResult.getCurrentPage() - 1, 1)
        );
        model.addAttribute(
            "nextPage",
            Math.min(
                pageResult.getCurrentPage() + 1,
                pageResult.getTotalPages()
            )
        );
        model.addAttribute("hasPrevious", pageResult.isHasPrevious());
        model.addAttribute("hasNext", pageResult.isHasNext());

        return "admin/user-management :: userTable";
    }

    @PreAuthorize("hasAuthority('USER_CREATE')")
    @GetMapping("admin/users/create")
    public String createUser(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new CreateUserRequestDTO());
        }

        model.addAttribute("activeNav", "users");
        model.addAttribute("availableRoles", systemRoleRepository.findAll());
        return "admin/create-user";
    }

    @PreAuthorize("hasAuthority('USER_CREATE')")
    @PostMapping("/admin/users/create")
    public String createUser(
        @Valid @ModelAttribute("user") CreateUserRequestDTO request,
        BindingResult bindingResult,
        @RequestParam(required = false) MultipartFile profileImage,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (!bindingResult.hasErrors()) {
            if (profileImage == null || profileImage.isEmpty()) {
                bindingResult.reject(
                    "profileImage",
                    "Profile picture is required"
                );
            } else {
                long maxFileSizeBytes = DataSize.parse(maxFileSize).toBytes();
                if (profileImage.getSize() > maxFileSizeBytes) {
                    bindingResult.reject(
                        "profileImage.tooLarge",
                        "Profile picture must be under" + maxFileSizeBytes
                    );
                }
            }

            if (userRepository.existsByUsername(request.getUsername())) {
                bindingResult.rejectValue(
                    "username",
                    "duplicate",
                    "Username is already taken"
                );
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                bindingResult.rejectValue(
                    "email",
                    "duplicate",
                    "Email is already in use"
                );
            }

            if (
                !systemRoleRepository.existsByNameIgnoreCase(request.getRoles())
            ) {
                bindingResult.rejectValue(
                    "roles",
                    "invalid",
                    "Selected role no longer exists"
                );
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                "availableRoles",
                systemRoleRepository.findAll()
            );
            return "admin/create-user";
        }

        try {
            userCreationSevice.createUser(request, profileImage);
        } catch (Exception e) {
            log.error("Failed to create user", e);
            redirectAttributes.addFlashAttribute(
                "error",
                "could not create user: " + e.getMessage()
            );
            return "redirect:/admin/users/create";
        }

        return "redirect:/admin/users";
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @GetMapping("/admin/users/update/{userId}")
    public String getUpdateUser(@PathVariable UUID userId, Model model) {
        UserDTO existing = userUpdateService.getUserById(userId);
        boolean isDefualtUser = SYSTEM_DEFULT_USERNAME.contains(
            existing.getUsername().toLowerCase()
        );
        model.addAttribute("activeNav", "users");
        if (!model.containsAttribute("user")) {
            UpdateUserRequestDTO dto = new UpdateUserRequestDTO();
            dto.setUsername(existing.getUsername());
            dto.setEmail(existing.getEmail());
            dto.setFirstName(existing.getFirstName());
            dto.setLastName(existing.getLastName());
            dto.setIsActive(existing.isActive());
            dto.setRoles(existing.getRoles());
            model.addAttribute("user", dto);
            model.addAttribute(
                "profilePicturePath",
                existing.getProfilePicturePath()
            );
        }
        model.addAttribute("isDefaultUser", isDefualtUser);
        model.addAttribute("userId", userId);
        model.addAttribute(
            "currentRole",
            ((UpdateUserRequestDTO) model.getAttribute("user")).getRoles()
        );
        model.addAttribute("availableRoles", systemRoleRepository.findAll());

        return "admin/update-user";
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @PostMapping("/admin/users/update/{userId}")
    public String processUpdateUser(
        @PathVariable UUID userId,
        @Valid @ModelAttribute("user") UpdateUserRequestDTO request,
        BindingResult bindingResult,
        @RequestParam(required = false) MultipartFile profileImage,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (!bindingResult.hasErrors()) {
            userRepository.findById(userId).ifPresent(currentUser -> {
                if (
                    SYSTEM_DEFULT_USERNAME.contains(
                        currentUser.getUsername().toLowerCase()
                    )
                ) {
                    bindingResult.reject(
                        "protected.user",
                        "System defualt user can not update"
                    );
                }
            });

            if (profileImage != null && !profileImage.isEmpty()) {
                long maxFileSizeBytes = DataSize.parse(maxFileSize).toBytes();
                if (profileImage.getSize() > maxFileSizeBytes) {
                    bindingResult.reject(
                        "profileImage.tooLarge",
                        "Profile picture must be under " + maxFileSize
                    );
                }
            }

            userRepository
                .findByUsername(request.getUsername())
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing ->
                    bindingResult.rejectValue(
                        "username",
                        "duplicate",
                        "Username is already taken"
                    )
                );

            userRepository
                .findByEmail(request.getEmail())
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing ->
                    bindingResult.rejectValue(
                        "email",
                        "duplicate",
                        "Email is already in use"
                    )
                );

            if (
                !systemRoleRepository.existsByNameIgnoreCase(request.getRoles())
            ) {
                bindingResult.rejectValue(
                    "roles",
                    "invalid",
                    "Selected role no longer exists"
                );
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", userId);
            model.addAttribute(
                "profilePicturePath",
                userUpdateService.getUserById(userId).getProfilePicturePath()
            );
            model.addAttribute("currentRole", request.getRoles());
            model.addAttribute(
                "availableRoles",
                systemRoleRepository.findAll()
            );

            return "admin/update-user";
        }

        try {
            userUpdateService.updateUser(userId, request, profileImage);
        } catch (Exception e) {
            log.error("Failed to update user {}", userId, e);
            redirectAttributes.addFlashAttribute(
                "error",
                "Could not update user: " + e.getMessage()
            );

            return "redirect:/admin/users" + "/update/" + userId;
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/delete/{userId}")
    public String deleteUser(
        @PathVariable UUID userId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            userDeletionService.deleteUser(userId);
            redirectAttributes.addFlashAttribute(
                "success",
                "User deleted successfully."
            );
        } catch (Exception e) {
            log.error("Failed to delete user {}", userId, e);
            redirectAttributes.addFlashAttribute(
                "error",
                "Could not delete user: " + e.getMessage()
            );
        }
        return "redirect:/admin/users";
    }
}
