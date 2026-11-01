package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.dto.AdminStatisticsDto;
import fr.uge.project.ruge.services.AdminStatisticsService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsControllerTest extends MockitoTestSupport {
  @Mock
  private AdminStatisticsService adminStatisticsService;

  @Test
  void indexAddsStatisticsToModel() {
    AdminStatisticsController controller = new AdminStatisticsController(adminStatisticsService);
    ExtendedModelMap model = new ExtendedModelMap();
    var stats = new AdminStatisticsDto(1, 1, 0, 1, 2, 2, 0, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    when(adminStatisticsService.getStatistics()).thenReturn(stats);

    String view = controller.index(model);

    assertThat(view).isEqualTo("admin/statistics");
    assertThat(model.getAttribute("statistics")).isEqualTo(stats);
  }
}
