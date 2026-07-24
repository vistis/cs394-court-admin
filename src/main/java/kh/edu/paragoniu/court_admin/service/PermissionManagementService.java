package kh.edu.paragoniu.court_admin.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kh.edu.paragoniu.court_shared.dto.permission.CreatePermissionRequestDTO;
import kh.edu.paragoniu.court_shared.dto.permission.PermissionListItemDTO;
import kh.edu.paragoniu.court_shared.dto.permission.PermissionListModuleDTO;
import kh.edu.paragoniu.court_shared.dto.permission.PermissionModuleDTO;
import kh.edu.paragoniu.court_shared.entity.SystemPermission;
import kh.edu.paragoniu.court_shared.repository.RolePermissionRepository;
import kh.edu.paragoniu.court_shared.repository.SystemPermissionRepository;

@Service
public class PermissionManagementService {
    @Autowired
    private SystemPermissionRepository systemPermissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    public List<PermissionListItemDTO> list() {
        return systemPermissionRepository.findPermissionListItems();
    }

    public SystemPermission getById(Integer permissionId) {
        return systemPermissionRepository.findById(permissionId)
                .orElseThrow( () -> new IllegalArgumentException("Permission not found" + permissionId));
    }

    @Transactional
    public Integer create(CreatePermissionRequestDTO request) {
        SystemPermission permission = new SystemPermission();

        permission.setCode(request.getCode().toUpperCase());

        permission = systemPermissionRepository.save(permission);

        return permission.getSystemPermissionId();
    }

    @Transactional
    public void update(Integer permissionId, CreatePermissionRequestDTO request) {
        SystemPermission permission = new SystemPermission();

        permission.setCode(request.getCode().toUpperCase());

        systemPermissionRepository.save(permission);
    }

    @Transactional
    public boolean delete(Integer permissionId) {
        long roleCount = rolePermissionRepository.countBySystemPermissionSystemPermissionId(permissionId);
        if (roleCount > 0) {
            return false;
        }
        systemPermissionRepository.deleteById(permissionId);
        return true;
    }

    public List<PermissionListModuleDTO> listGrouped() {
        List<PermissionListItemDTO> flat = list();
        Map<String, List<PermissionListItemDTO>> grouped = new LinkedHashMap<>();
        for (PermissionListItemDTO item: flat) {
            String module = extractModuleName(item.getCode());
            grouped.computeIfAbsent(module, k -> new ArrayList<>()).add(item);
        }

        return grouped.entrySet().stream()
                .map(e -> new PermissionListModuleDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PermissionListModuleDTO::getModuleName))
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
}
