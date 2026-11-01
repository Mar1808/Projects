package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.UserDto;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.exceptions.UserAlreadyExistsException;
import fr.uge.project.ruge.services.PasswordResetService;
import fr.uge.project.ruge.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

@Controller
public class UserController {
  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();
  private final PasswordResetService passwordResetService;

  public UserController(UserService userService,
                        AuthenticationManager authenticationManager,
                        PasswordResetService passwordResetService) {
    this.userService = Objects.requireNonNull(userService);
    this.authenticationManager = Objects.requireNonNull(authenticationManager);
    this.passwordResetService = Objects.requireNonNull(passwordResetService);
  }

  @GetMapping("/register")
  public String registerForm(Model model) {
    model.addAttribute("user", new UserDto(null, null, null, null, userService.getDefaultProfilePhotoId(), null));
    model.addAttribute("profilePhotos", userService.getAvailableProfilePhotos());
    model.addAttribute("roles", Role.values());
    return "auth/register";
  }

  @PostMapping("/register")
  public String registerUser(@Valid @ModelAttribute("user") UserDto userDto,
                             BindingResult result,
                             Model model,
                             HttpServletRequest request,
                             HttpServletResponse response) {
    if (result.hasErrors()) {
      model.addAttribute("profilePhotos", userService.getAvailableProfilePhotos());
      return "auth/register";
    }
    try {
      userService.registerUser(userDto);
    } catch (UserAlreadyExistsException | IllegalArgumentException e) {
      model.addAttribute("profilePhotos", userService.getAvailableProfilePhotos());
      model.addAttribute("error", e.getMessage());
      return "auth/register";
    }

    var authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(userDto.email(), userDto.password())
    );
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
    return "redirect:/home";
  }

  @GetMapping("/login")
  public String loginForm() {
    return "auth/login";
  }

  @GetMapping("/forgot-password")
  public String showForgotPasswordForm() {
    return "auth/forgot-password";
  }

  @PostMapping("/forgot-password")
  public String processForgotPassword(@RequestParam("email") String email, Model model) {
    passwordResetService.processForgotPassword(email);
    model.addAttribute("message", "If this email exists, a reset link has been sent.");
    return "auth/forgot-password";
  }

  @GetMapping("/reset-password")
  public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
    var result = passwordResetService.validatePasswordResetToken(token);
    if (result != null) {
      model.addAttribute("error", result);
    } else {
      model.addAttribute("token", token);
    }
    return "auth/reset-password";
  }

  @PostMapping("/reset-password")
  public String processResetPassword(@RequestParam("token") String token,
                                     @RequestParam("password") String password,
                                     Model model) {
    var result = passwordResetService.updatePassword(token, password);
    if (result != null && (result.toLowerCase().contains("success") || result.toLowerCase().contains("succes"))) {
      model.addAttribute("message", "Your password has been reset successfully. You can now sign in.");
      return "redirect:/login?resetSuccess";
    }
    model.addAttribute("error", result);
    return "auth/reset-password";
  }
}
