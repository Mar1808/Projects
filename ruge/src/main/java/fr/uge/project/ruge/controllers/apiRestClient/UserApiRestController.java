package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.dto.AuthResponseDto;
import fr.uge.project.ruge.dto.LoginDto;
import fr.uge.project.ruge.dto.ProfilePhotoDto;
import fr.uge.project.ruge.dto.UserDto;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.exceptions.UserAlreadyExistsException;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.services.rest.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/users") // Toutes les routes ici commencent par /api/users
public class UserApiRestController {
  private final UserService userService;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public UserApiRestController(UserService userService,
                               AuthenticationManager authenticationManager,
                               JwtService jwtService) {
    this.userService = Objects.requireNonNull(userService);
    this.authenticationManager = Objects.requireNonNull(authenticationManager);
    this.jwtService = Objects.requireNonNull(jwtService);
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody UserDto userDto) {
    try {
      userService.registerUser(userDto);
      return ResponseEntity.ok().body("Account created successfully");
    } catch (UserAlreadyExistsException | IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @GetMapping("/profile-photos")
  public ResponseEntity<List<ProfilePhotoDto>> getProfilePhotos() {
    return ResponseEntity.ok(userService.getAvailableProfilePhotos());
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDto> login(@RequestBody LoginDto loginDto) {
    var user = userService.findByEmail(loginDto.email());
    if (user.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (user.get().getStatus() == Status.BANNED) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    try {
      var authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(loginDto.email(), loginDto.password())
      );
      var token = jwtService.generateToken(authentication);
      var userEntity = user.get();
      return ResponseEntity.ok(new AuthResponseDto(token, userEntity.getRole(), userEntity.getName(), userEntity.getEmail(), userEntity.getImageUrl(), userEntity.getId()));

    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}