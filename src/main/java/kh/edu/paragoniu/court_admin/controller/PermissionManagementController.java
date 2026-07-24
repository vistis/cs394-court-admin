package kh.edu.paragoniu.court_admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import kh.edu.paragoniu.court_admin.service.PermissionManagementService;
import kh.edu.paragoniu.court_shared.dto.permission.CreatePermissionRequestDTO;
import kh.edu.paragoniu.court_shared.repository.SystemPermissionRepository;


@Controller
public class PermissionManagementController {
    
    @Autowired
    private PermissionManagementService permissionManagementService;

    @Autowired
    private SystemPermissionRepository systemPermissionRepository;

    @GetMapping("/admin/permissions")
    public String permissionManagement(Model model) {
        model.addAttribute("activeNav", "permissions");
        model.addAttribute("groupedPermissions", permissionManagementService.listGrouped());
        return "admin/permission-management";
    }


    @GetMapping("/admin/permissions/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("permission")) {
            model.addAttribute("permission", new CreatePermissionRequestDTO());
        }
        model.addAttribute("activeNav", "permissions");
        return "admin/create-permission";
    }

    @PostMapping("/admin/permissions/create")
    public String createPermission(
        @Valid @ModelAttribute("permission") CreatePermissionRequestDTO request,
        BindingResult bindingResult,
        Model model
    ) {
        if (!bindingResult.hasErrors() && systemPermissionRepository.existsByCodeIgnoreCase(request.getCode())) {
            bindingResult.rejectValue("code", "duplicate", "This permission code already exists");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "permissions");
            return "admin/create-permission";
        }

        permissionManagementService.create(request);
        return "redirect:/admin/permissions";
    }

    @GetMapping("/admin/permissions/{permissionId}/edit")
    public String showEditForm(
        @PathVariable Integer permissionId,
        Model model
    ) {
        if (!model.containsAttribute("permission")) {
            var existing = permissionManagementService.getById(permissionId);
            model.addAttribute("permission", new CreatePermissionRequestDTO(existing.getCode()));
        }

        model.addAttribute("activeNav", "permissions");
        model.addAttribute("permissionId", permissionId);
        return "admin/edit-permission";
    }

    @PostMapping("/admin/permissions/{permissionId}/edit")
    public String updatePermission(
        @PathVariable Integer permissionId,
        @Valid @ModelAttribute("permission") CreatePermissionRequestDTO request,
        BindingResult bindingResult,
        Model model
    ) {
        if (!bindingResult.hasErrors()) {
            systemPermissionRepository.findByCodeIgnoreCase(request.getCode())
                .filter(existing -> !existing.getSystemPermissionId().equals(permissionId))
                .ifPresent(existing -> bindingResult.rejectValue("code", "duplicate", "This permission already exist"));

        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "permissions");
            model.addAttribute("permissionId", permissionId);
            return "admin/edit-permission";
        }

        permissionManagementService.update(permissionId, request);

        return "redirect:/admin/permissions";
    }

    @PostMapping("/admin/permissions/{permissionId}/delete")
    public String deletePermission(
        @PathVariable Integer permissionId,
        RedirectAttributes redirectAttributes
    ) {
        boolean deleted = permissionManagementService.delete(permissionId);
        if (!deleted) {
            redirectAttributes.addFlashAttribute("error", "Can not delete this permission while it is still assigned to roles");
        } else {
            redirectAttributes.addFlashAttribute("success", "Permission deleted.");
        }
        return "redirect:/admin/permissions";
    }

}
