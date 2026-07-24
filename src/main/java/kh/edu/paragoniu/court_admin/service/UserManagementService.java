package kh.edu.paragoniu.court_admin.service;


import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import kh.edu.paragoniu.court_shared.dto.user.UserDTO;
import kh.edu.paragoniu.court_shared.dto.user.UserPageResultDTO;
import kh.edu.paragoniu.court_shared.entity.User;
import kh.edu.paragoniu.court_shared.repository.UserRepository;

@Service
public class UserManagementService {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    public UserPageResultDTO search(String query, Boolean statusFilter, int page ) {
        int safePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(safePage -1, PAGE_SIZE, Sort.by("lastName", "firstName"));

        Page<UUID> idPage = userRepository.searchUserIds(query, statusFilter, pageable);
        List<UUID> orderedIds = idPage.getContent();

        List<User> hydrated = orderedIds.isEmpty()
            ? List.of()
            : userRepository.findByIdsWithRoles(orderedIds);

        
        Map<UUID, User> byId = hydrated.stream()
            .collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<UserDTO> rows = orderedIds.stream()
            .map(byId::get)
            .filter(java.util.Objects::nonNull) // defensive: skip if a user was deleted between queries
            .map(user -> new UserDTO(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                storageService.getFullUrl(user.getProfilePicturePath()),
                user.isActive(),
                user.getUserRoles().stream().findFirst().map(ur -> ur.getSystemRole().getName()).orElse(null)
            ))
            .toList();

        int totalPages = Math.max(idPage.getTotalPages(), 1);

        return new UserPageResultDTO(
            rows,
            safePage,
            totalPages,
            idPage.hasPrevious(),
            idPage.hasNext()
        );
    }

    public UserState getState() {
        long totalUser = userRepository.count();
        long activeUser = userRepository.countByIsActive(true);
        long inactiveUser = totalUser - activeUser;
        return new UserState(totalUser, activeUser, inactiveUser);
    }

    public record UserState(long total, long active, long inactive) {}

}
