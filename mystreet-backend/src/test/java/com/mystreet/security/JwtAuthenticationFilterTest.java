package com.mystreet.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mystreet.domain.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-for-unit-tests-only-0123456789abcdef";

    private final JwtService jwtService = new JwtService(SECRET, 60);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User user(boolean admin) {
        User u = new User();
        u.setEmail("ana@test.com");
        u.setFullName("Ana");
        u.setAdmin(admin);
        return u;
    }

    @Test
    void validAdminToken_authenticatesWithUserAndAdminRoles() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(user(true)));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("ana@test.com");
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void validUserToken_doesNotGetAdminRole() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken(user(false)));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(roles).containsExactly("ROLE_USER");
    }

    @Test
    void invalidTokenOrMissingHeader_leavesRequestUnauthenticatedButContinuesChain() throws Exception {
        MockHttpServletRequest garbage = new MockHttpServletRequest();
        garbage.addHeader("Authorization", "Bearer not.a.token");
        MockFilterChain chainWithGarbage = new MockFilterChain();
        filter.doFilter(garbage, new MockHttpServletResponse(), chainWithGarbage);

        MockFilterChain chainWithoutHeader = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chainWithoutHeader);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chainWithGarbage.getRequest()).isNotNull();
        assertThat(chainWithoutHeader.getRequest()).isNotNull();
    }
}
