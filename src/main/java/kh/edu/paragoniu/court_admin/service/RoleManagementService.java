package kh.edu.paragoniu.court_admin.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kh.edu.paragoniu.court_shared.dto.permission.*;
import kh.edu.paragoniu.court_shared.entity.RolePermission;
import kh.edu.paragoniu.court_shared.entity.RolePermissionId;
import kh.edu.paragoniu.court_shared.entity.SystemPermission;
import kh.edu.paragoniu.court_shared.entity.SystemRole;
import kh.edu.paragoniu.court_shared.repository.RolePermissionRepository;
import kh.edu.paragoniu.court_shared.repository.SystemPermissionRepository;
import kh.edu.paragoniu.court_shared.repository.SystemRoleRepository;
import kh.edu.paragoniu.court_shared.repository.UserRoleRepository;

@Service
public class RoleManagementService {
    
    @Autowired 
    private SystemRoleRepository systemRoleRepository;

    @Autowired 
    private SystemPermissionRepository systemPermissionRepository;

    @Autowired 
    private RolePermissionRepository rolePermissionRepository;

    @Autowired 
    private UserRoleRepository userRoleRepository;

    public List<RoleListItemDTO> list() {
        return systemRoleRepository.findRoleListItems();
    }

     public RoleDetailDTO getRoleDetail(Integer roleId) {
        SystemRole role = systemRoleRepository.findByIdWithPermissions(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        List<RoleUserSummaryDTO> users = userRoleRepository.findUserSummariesByRoleId(roleId);

        List<PermissionDTO> permissions = role.getRolePermissions().stream()
            .map(rp -> new PermissionDTO(
                rp.getSystemPermission().getSystemPermissionId(),
                rp.getSystemPermission().getCode(),
                true
            ))
            .toList();

        return new RoleDetailDTO(role.getSystemRoleId(), role.getName(), users, permissions);
    }

    // Group permission into module
    public List<PermissionModuleDTO> getGroupedPermission() {
        return groupByModule(getAllPermissionsForForm());
    }

    public List<PermissionModuleDTO> getGroupedPermission(Integer roleId) {
        return groupByModule(getAllPermissionsForForm(roleId));
    }

    private List<PermissionModuleDTO> groupByModule(List<PermissionDTO> permission) {
        Map<String, List<PermissionDTO>> grouped = new LinkedHashMap<>();

        for (PermissionDTO p: permission) {
            String module = extractModuleName(p.getCode());
            grouped.computeIfAbsent(module, k -> new ArrayList<>()).add(p);
        }
        return grouped.entrySet().stream()
                .map(e -> new PermissionModuleDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PermissionModuleDTO::getModuleName))
                .toList();
    }

    private String extractModuleName(String code) {
        if (code == null || !code.contains("_")) {
            return "Other";
        }
        String prefix = code.substring(0, code.indexOf('_')).toLowerCase();
        String capitalized = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
        return capitalized;
    }

     // Create-role form: nothing pre-checked
    public List<PermissionDTO> getAllPermissionsForForm() {
        return systemPermissionRepository.findAllByOrderByCode().stream()
            .map(p -> new PermissionDTO(p.getSystemPermissionId(), p.getCode(), false))
            .toList();
    }

    // Edit-role form: pre-check whatever the role already has
    public List<PermissionDTO> getAllPermissionsForForm(Integer roleId) {
        List<Integer> assignedIds = rolePermissionRepository.findBySystemRoleSystemRoleId(roleId).stream()
            .map(rp -> rp.getSystemPermission().getSystemPermissionId())
            .toList();

        return systemPermissionRepository.findAllByOrderByCode().stream()
            .map(p -> new PermissionDTO(
                p.getSystemPermissionId(),
                p.getCode(),
                assignedIds.contains(p.getSystemPermissionId())
            ))
            .toList();
    }

    @Transactional
    public Integer createRole(CreateRoleRequestDTO request) {
        SystemRole role = new SystemRole();
        role.setName(request.getName());
        role = systemRoleRepository.save(role);

        savePermissions(role, request.getPermissionIds());
        return role.getSystemRoleId();
    }

    @Transactional
    public void updateRole(Integer roleId, CreateRoleRequestDTO request) {
        SystemRole role = systemRoleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        role.setName(request.getName());
        systemRoleRepository.save(role);

        rolePermissionRepository.deleteBySystemRoleSystemRoleId(roleId);
        savePermissions(role, request.getPermissionIds());
    }

    private void savePermissions(SystemRole role, List<Integer> permissionIds) {
        for (Integer permissionId : permissionIds) {
            SystemPermission permission = systemPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown permission: " + permissionId));

            RolePermission rolePermission = new RolePermission();
            rolePermission.setSystemRole(role);
            rolePermission.setSystemPermission(permission);

            RolePermissionId id = new RolePermissionId();
            id.setSystemRoleId(role.getSystemRoleId());
            id.setSystemPermissionId(permission.getSystemPermissionId());
            rolePermission.setId(id);

            rolePermissionRepository.save(rolePermission);
        }
    }

    /**
     * Returns true if delete succeeded, false if blocked because users are still assigned.
     * Note: role_permissions cascades automatically via ON DELETE CASCADE — no manual cleanup needed.
     */
    @Transactional
    public boolean deleteRole(Integer roleId) {
        long userCount = userRoleRepository.countByIdSystemRoleId(roleId);
        if (userCount > 0) {
            return false;
        }
        systemRoleRepository.deleteById(roleId);
        return true;
    }
}
