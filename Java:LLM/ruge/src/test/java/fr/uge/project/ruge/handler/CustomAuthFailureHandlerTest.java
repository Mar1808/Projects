package fr.uge.project.ruge.handler;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthFailureHandlerTest {
  @Test
  void redirectsToBannedWhenAccountIsLocked() throws Exception {
    var handler = new CustomAuthFailureHandler();
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();

    handler.onAuthenticationFailure(request, response, new LockedException("Locked"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/login?banned");
  }

  @Test
  void redirectsToErrorForOtherFailures() throws Exception {
    var handler = new CustomAuthFailureHandler();
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();

    handler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
  }
}
