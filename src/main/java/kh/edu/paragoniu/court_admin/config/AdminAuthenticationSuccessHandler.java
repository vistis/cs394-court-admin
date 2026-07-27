package kh.edu.paragoniu.court_admin.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kh.edu.paragoniu.court_shared.entity.User;
import kh.edu.paragoniu.court_shared.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public AdminAuthenticationSuccessHandler(UserRepository userRepository) {
        super("/admin/users");
        setAlwaysUseDefaultTargetUrl(true);
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        //String username = authentication.getName();

        Boolean hasAccess = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ADMIN_PORTAL_ACCESS"));
        //User user = userRepository.findAuthenticatedUserByUsernameOrEmail(username).orElse(null);

        // boolean isAdministrator = user != null && user.getUserRoles().stream()
        //         .anyMatch(ur -> ur.getSystemRole().getName().equals("ADMINISTRATOR"));

        if (!hasAccess) {
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            getRedirectStrategy().sendRedirect(request, response, "/login?error");
            return;
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
