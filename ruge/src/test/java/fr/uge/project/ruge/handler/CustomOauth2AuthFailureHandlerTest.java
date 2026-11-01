package fr.uge.project.ruge.handler;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOauth2AuthFailureHandlerTest {
  @Test
  void redirectsToBannedWhenOauth2ErrorCodeIsBanned() throws Exception {
    var handler = new CustomOauth2AuthFailureHandler();
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var exception = new OAuth2AuthenticationException(new OAuth2Error("banned"), "Banned");

    handler.onAuthenticationFailure(request, response, exception);

    assertThat(response.getRedirectedUrl()).isEqualTo("/login?banned");
  }

  @Test
  void redirectsToErrorForOtherOauth2Failures() throws Exception {
    var handler = new CustomOauth2AuthFailureHandler();
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();

    handler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
  }
}
