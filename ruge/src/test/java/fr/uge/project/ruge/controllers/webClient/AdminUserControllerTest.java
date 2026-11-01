package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.BanHistoryDto;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.services.UserService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest extends MockitoTestSupport {
  @Mock
  private UserService userService;

  @Test
  void banUserRejectsInvalidDuration() {
    AdminUserController controller = new AdminUserController(userService);
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view = controller.banUser(
        5L,
        "abuse",
        null,
        "days",
        (Principal) () -> "admin@ruge.test",
        redirect
    );

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirect.getFlashAttributes().get("errorMessage"))
        .isEqualTo("Please provide a valid duration.");
    verify(userService, never()).banUser(5L, 1L, "abuse", 3);
  }

  @Test
  void banUserRejectsSelfBan() {
    AdminUserController controller = new AdminUserController(userService);
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    when(userService.findByEmail("admin@ruge.test")).thenReturn(Optional.of(user(7L, "admin@ruge.test")));

    String view = controller.banUser(
        7L,
        "abuse",
        2,
        "days",
        (Principal) () -> "admin@ruge.test",
        redirect
    );

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirect.getFlashAttributes().get("errorMessage")).isEqualTo("You cannot ban yourself.");
  }

  @Test
  void unbanUserSetsSuccessMessage() {
    AdminUserController controller = new AdminUserController(userService);
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view = controller.unbanUser(9L, redirect);

    assertThat(view).isEqualTo("redirect:/admin/users");
    assertThat(redirect.getFlashAttributes().get("successMessage"))
        .isEqualTo("User unbanned successfully.");
    verify(userService).unban(9L);
  }

  @Test
  void getBanHistoryPopulatesModel() {
    AdminUserController controller = new AdminUserController(userService);
    ExtendedModelMap model = new ExtendedModelMap();
    UserEntity target = user(11L, "target@ruge.test");
    var history = List.of(new BanHistoryDto(1L, 11L, 7L, LocalDateTime.now(), "test", 3));
    when(userService.getByIdOrThrow(11L)).thenReturn(target);
    when(userService.getBanHistory(11L)).thenReturn(history);

    String view = controller.getBanHistory(11L, model);

    assertThat(view).isEqualTo("admin/banHistory");
    assertThat(model.getAttribute("user")).isEqualTo(target);
    assertThat(model.getAttribute("banHistory")).isEqualTo(history);
  }

  private static UserEntity user(Long id, String email) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    user.setPassword("pwd");
    user.setName("User");
    user.setRole(Role.ADMIN);
    user.setStatus(Status.ACTIVE);
    return user;
  }
}
