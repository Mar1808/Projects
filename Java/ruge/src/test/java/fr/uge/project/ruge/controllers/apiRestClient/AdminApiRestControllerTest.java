package fr.uge.project.ruge.controllers.apiRestClient;

import fr.uge.project.ruge.dto.AdminStatisticsDto;
import fr.uge.project.ruge.dto.BanRequestDto;
import fr.uge.project.ruge.entity.UserEntity;
import fr.uge.project.ruge.enums.Role;
import fr.uge.project.ruge.enums.Status;
import fr.uge.project.ruge.services.*;
import fr.uge.project.ruge.services.rest.JwtService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminApiRestControllerTest extends MockitoTestSupport {
  @Mock
  private UserService userService;
  @Mock
  private JwtService jwtService;
  @Mock
  private WalletService walletService;
  @Mock
  private TransactionService transactionService;
  @Mock
  private ConfigurationService configurationService;
  @Mock
  private SuperLikeService superLikeService;
  @Mock
  private AdminStatisticsService adminStatisticsService;
  @Mock
  private RecipeService recipeService;
  @Mock
  private PromotionService promotionService; // AJOUT DU MOCK MANQUANT

  @Test
  void getStatisticsReturnsUnauthorizedWhenTokenIsInvalid() {
    var controller = controller();
    when(jwtService.validateToken("bad")).thenReturn(false);

    var response = controller.getStatistics("bad");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void getStatisticsReturnsForbiddenWhenUserIsNotAdmin() {
    var controller = controller();
    when(jwtService.validateToken("token")).thenReturn(true);
    when(jwtService.extractUsername("token")).thenReturn("member@ruge.test");
    when(userService.findByEmail("member@ruge.test")).thenReturn(Optional.of(user(1L, "member@ruge.test", Role.MEMBER)));

    var response = controller.getStatistics("token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void getStatisticsReturnsOkForAdmin() {
    var controller = controller();
    var stats = new AdminStatisticsDto(1, 1, 0, 1, 2, 2, 0, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    when(jwtService.validateToken("token")).thenReturn(true);
    when(jwtService.extractUsername("token")).thenReturn("admin@ruge.test");
    when(userService.findByEmail("admin@ruge.test")).thenReturn(Optional.of(user(2L, "admin@ruge.test", Role.ADMIN)));
    when(adminStatisticsService.getStatistics()).thenReturn(stats);

    var response = controller.getStatistics("token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(stats);
  }

  @Test
  void banUserRejectsSelfBan() {
    var controller = controller();
    when(jwtService.validateToken("token")).thenReturn(true);
    when(jwtService.extractUsername("token")).thenReturn("admin@ruge.test");
    when(userService.findByEmail("admin@ruge.test")).thenReturn(Optional.of(user(5L, "admin@ruge.test", Role.ADMIN)));

    var response = controller.banUser(5L, new BanRequestDto("test", 3), "token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("You cannot ban yourself");
  }

  @Test
  void deleteRecipeReturnsServerErrorWhenServiceFails() {
    var controller = controller();
    when(jwtService.validateToken("token")).thenReturn(true);
    when(jwtService.extractUsername("token")).thenReturn("admin@ruge.test");
    when(userService.findByEmail("admin@ruge.test")).thenReturn(Optional.of(user(3L, "admin@ruge.test", Role.ADMIN)));
    doThrow(new IllegalStateException("boom")).when(recipeService).deleteRecipe(44L);

    var response = controller.deleteRecipe(44L, "token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isFalse();
    verify(recipeService).deleteRecipe(44L);
  }

  private AdminApiRestController controller() {
    // CORRECTION : Ajout du 9ème paramètre (promotionService) pour correspondre au contrôleur
    return new AdminApiRestController(
            userService,
            jwtService,
            walletService,
            transactionService,
            configurationService,
            superLikeService,
            adminStatisticsService,
            recipeService,
            promotionService
    );
  }

  private static UserEntity user(Long id, String email, Role role) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    user.setRole(role);
    user.setPassword("pwd");
    user.setStatus(Status.ACTIVE);
    return user;
  }
}