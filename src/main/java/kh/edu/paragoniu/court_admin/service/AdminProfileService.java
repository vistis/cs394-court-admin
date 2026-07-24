package kh.edu.paragoniu.court_admin.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import kh.edu.paragoniu.court_shared.dto.user.UpdateProfileRequestDTO;
import kh.edu.paragoniu.court_shared.dto.user.UserDTO;
import kh.edu.paragoniu.court_shared.entity.User;
import kh.edu.paragoniu.court_shared.repository.UserRepository;

@Service
public class AdminProfileService {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    public UserDTO getProfile(UUID userId) {
        User user = userRepository.findByIdWithRoles(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return new UserDTO(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            storageService.getFullUrl(user.getProfilePicturePath()),
            user.isActive(),
            user.getUserRoles().stream().findFirst().map( ur -> ur.getSystemRole().getName()).orElse(null)
        );
    }

    @Transactional
    public void updateProfile(UUID userId, UpdateProfileRequestDTO request, MultipartFile newProfilePicture) {
        User user = userRepository.findById(userId)
            .orElseThrow( () -> new IllegalArgumentException("User not found: " + userId));

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (newProfilePicture != null && !newProfilePicture.isEmpty()) {
            String oldPath = user.getProfilePicturePath();
            String newPath = storageService.uploadFile(newProfilePicture, "profiles/users");
            user.setProfilePicturePath(newPath);

            if (oldPath != null) {
                storageService.deleteFile(oldPath);
            }
        }

        userRepository.save(user);
    }

}
