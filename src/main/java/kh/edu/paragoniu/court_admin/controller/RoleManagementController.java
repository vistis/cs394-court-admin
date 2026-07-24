package kh.edu.paragoniu.court_admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import kh.edu.paragoniu.court_admin.service.RoleManagementService;
import kh.edu.paragoniu.court_shared.dto.permission.*;
import kh.edu.paragoniu.court_shared.repository.SystemRoleRepository;

@Controller
public class RoleManagementController  {

    @Autowired 
    private RoleManagementService roleManagementService;

    @Autowired 
    private SystemRoleRepository systemRoleRepository;

    @GetMapping("/admin/roles")
    public String roleManagement(Model model) {
        model.addAttribute("activeNav", "roles");
        model.addAttribute("roles", roleManagementService.list());
        return "admin/role-management";
    }

    @GetMapping("/admin/roles/{roleId}")
    public String roleDetail(@PathVariable Integer roleId, Model model) {
        RoleDetailDTO detail = roleManagementService.getRoleDetail(roleId);
        model.addAttribute("activeNav", "roles");
        model.addAttribute("role", detail);
        return "admin/role-detail";
    }

    @GetMapping("/admin/roles/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("role")) {
            model.addAttribute("role", new CreateRoleRequestDTO());
        }
        model.addAttribute("activeNav", "roles");
        model.addAttribute("groupedPermissions", roleManagementService.getGroupedPermission());
        return "admin/create-role";
    }

    @PostMapping("/admin/roles/create")
    public String createRole(
            @Valid @ModelAttribute("role") CreateRoleRequestDTO request,
            BindingResult bindingResult,
            Model model) {

        if (!bindingResult.hasErrors() && systemRoleRepository.existsByNameIgnoreCase(request.getName())) {
            bindingResult.rejectValue("name", "duplicate", "A role with this name already exists");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "roles");
            model.addAttribute("allPermissions", markSelectedGrouped(
                roleManagementService.getGroupedPermission(), request.getPermissionIds()));
            return "admin/create-role";
        }

        Integer newRoleId = roleManagementService.createRole(request);
        return "redirect:/admin/roles/" + newRoleId;
    }

    @GetMapping("/admin/roles/{roleId}/edit")
    public String showEditForm(@PathVariable Integer roleId, Model model) {
        if (!model.containsAttribute("role")) {
            RoleDetailDTO existing = roleManagementService.getRoleDetail(roleId);
            CreateRoleRequestDTO dto = new CreateRoleRequestDTO();
            dto.setName(existing.getName());
            dto.setPermissionIds(existing.getPermissions().stream().map(PermissionDTO::getPermissionId).toList());
            model.addAttribute("role", dto);
        }
        model.addAttribute("activeNav", "roles");
        model.addAttribute("roleId", roleId);
        model.addAttribute("groupedPermissions", roleManagementService.getGroupedPermission(roleId));
        return "admin/edit-role";
    }

    @PostMapping("/admin/roles/{roleId}/edit")
    public String updateRole(
            @PathVariable Integer roleId,
            @Valid @ModelAttribute("role") CreateRoleRequestDTO request,
            BindingResult bindingResult,
            Model model) {

        if (!bindingResult.hasErrors()) {
            systemRoleRepository.findByNameIgnoreCase(request.getName())
                .filter(existing -> !existing.getSystemRoleId().equals(roleId))
                .ifPresent(existing -> bindingResult.rejectValue("name", "duplicate", "A role with this name already exists"));
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "roles");
            model.addAttribute("roleId", roleId);
            model.addAttribute("groupedPermissions", markSelectedGrouped(
                roleManagementService.getGroupedPermission(), request.getPermissionIds()));
            return "admin/edit-role";
        }

        roleManagementService.updateRole(roleId, request);
        return "redirect:/admin/roles/" + roleId;
    }

    @PostMapping("/admin/roles/{roleId}/delete")
    public String deleteRole(@PathVariable Integer roleId, RedirectAttributes redirectAttributes) {
        boolean deleted = roleManagementService.deleteRole(roleId);
        if (!deleted) {
            redirectAttributes.addFlashAttribute("error",
                "Cannot delete this role while users are still assigned to it. Reassign those users first.");
        } else {
            redirectAttributes.addFlashAttribute("success", "Role deleted.");
        }
        return "redirect:/admin/roles";
    }

    private List<PermissionModuleDTO> markSelectedGrouped(List<PermissionModuleDTO> groups, List<Integer> selectedIds) {
        return groups.stream()
            .map(group -> new PermissionModuleDTO(
            group.getModuleName(),
            group.getPermission().stream()
                .map(p -> new PermissionDTO(p.getPermissionId(), p.getCode(),
                    selectedIds != null && selectedIds.contains(p.getPermissionId())))
                .toList()
        ))
        .toList();
    }
}
