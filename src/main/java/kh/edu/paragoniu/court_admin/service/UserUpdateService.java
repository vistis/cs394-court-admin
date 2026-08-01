package kh.edu.paragoniu.court_admin.service;

import java.util.UUID;

import kh.edu.paragoniu.court_shared.dto.user.UpdateUserRequestDTO;
import kh.edu.paragoniu.court_shared.dto.user.UserDTO;
import kh.edu.paragoniu.court_shared.entity.SystemRole;
import kh.edu.paragoniu.court_shared.entity.User;
import kh.edu.paragoniu.court_shared.entity.UserRole;
import kh.edu.paragoniu.court_shared.entity.UserRoleId;
import kh.edu.paragoniu.court_shared.repository.SystemRoleRepository;
import kh.edu.paragoniu.court_shared.repository.UserRepository;
import kh.edu.paragoniu.court_shared.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;


@Service
public class UserUpdateService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private SystemRoleRepository systemRoleRepository;

    UserUpdateService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users:detail", key = "#userId")
    public UserDTO getUserById(UUID userId) {
        User user = userRepository
            .findByIdWithRoles(userId)
            .orElseThrow(() ->
                new IllegalArgumentException("User not found: " + userId)
            );

        return new UserDTO(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            storageService.getFullUrl(user.getProfilePicturePath()),
            user.isActive(),
            user.getUserRoles().stream().findFirst().map(ur -> ur.getSystemRole().getName()).orElse(null)
        );
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "users:detail", key = "#userId"),
        @CacheEvict(value = {"users:search", "users:state"}, allEntries = true)
    })
    public void updateUser(
        UUID userId,
        UpdateUserRequestDTO request,
        MultipartFile newProfilePicture
    ) {
        User user = userRepository
            .findById(userId)
            .orElseThrow(() ->
                new IllegalArgumentException("User not found: " + userId)
            );

        SystemRole role = systemRoleRepository.findByNameIgnoreCase(request.getRoles())
            .orElseThrow( () -> new IllegalArgumentException("Unknow role: " + request.getRoles()));

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setActive(Boolean.TRUE.equals(request.getIsActive()));


        // if (newPassword != null && !newPassword.isBlank()) {
        //     user.setPassword(passwordEncoder.encode(newPassword));
        // }

        if (newProfilePicture != null && !newProfilePicture.isEmpty()) {
            String oldPath = user.getProfilePicturePath();
            String newPath = storageService.uploadFile(
                newProfilePicture,
                "users"
            );
            user.setProfilePicturePath(newPath);

            if (oldPath != null) {
                storageService.deleteFile(oldPath);
            }
        }

        userRepository.save(user);

        userRoleRepository.deleteByIdUserId(user.getUserId());

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setSystemRole(role);

        UserRoleId id = new UserRoleId();
        id.setUserId(user.getUserId());
        id.setSystemRoleId(role.getSystemRoleId());
        userRole.setId(id);

        userRoleRepository.save(userRole);
    }
}
