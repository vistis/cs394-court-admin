package kh.edu.paragoniu.court_admin.service;

import java.util.UUID;
import kh.edu.paragoniu.court_shared.entity.User;
import kh.edu.paragoniu.court_shared.repository.UserRepository;
import kh.edu.paragoniu.court_shared.repository.UserRoleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDeletionService {

    @Autowired private UserRepository userRepository;
    @Autowired private StorageService storageService;
    @Autowired private UserRoleRepository userRoleRepository;

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String picturePath = user.getProfilePicturePath();

        userRoleRepository.deleteByIdUserId(userId);

        userRepository.delete(user); // user_roles rows cascade via ON DELETE CASCADE

        if (picturePath != null) {
            storageService.deleteFile(picturePath);
        }
    }
}