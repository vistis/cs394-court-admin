package kh.edu.paragoniu.court_admin.service;


import kh.edu.paragoniu.court_shared.dto.user.CreateUserRequestDTO;
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

@Service
public class UserCreationSevice {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private SystemRoleRepository systemRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StorageService storageService;

    @Transactional
    public void createUser(
        CreateUserRequestDTO request,
        MultipartFile profileImage
    ) {
        SystemRole role = systemRoleRepository.findByNameIgnoreCase(request.getRoles())
            .orElseThrow( () -> new IllegalArgumentException("Unknow role: " + request.getRoles()));

        String profilePicturePath = (profileImage != null && !profileImage.isEmpty())
            ? storageService.uploadFile(profileImage, "profiles/users")
            : "N/A";

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(request.getIsActive());
        user.setProfilePicturePath(profilePicturePath);

        user = userRepository.save(user);

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setSystemRole(role);

        UserRoleId id = new UserRoleId();
        id.setUserId(user.getUserId());
        id.setSystemRoleId(role.getSystemRoleId());
        userRole.setId(id);
        // userRole.setSystemRole(role);

        userRoleRepository.save(userRole);
    }
}
