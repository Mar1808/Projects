package fr.uge.project.ruge.controllers.webClient;

import fr.uge.project.ruge.services.ConfigurationService;
import fr.uge.project.ruge.testsupport.MockitoTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTermsControllerTest extends MockitoTestSupport {
  @Mock
  private ConfigurationService configurationService;

  @Test
  void indexLoadsTermsContent() {
    AdminTermsController controller = new AdminTermsController(configurationService);
    ExtendedModelMap model = new ExtendedModelMap();
    when(configurationService.getTermsContent()).thenReturn("terms body");

    String view = controller.index(model);

    assertThat(view).isEqualTo("admin/manageTerms");
    assertThat(model.getAttribute("termsContent")).isEqualTo("terms body");
  }

  @Test
  void updateTermsShowsSuccessFlashOnSuccess() {
    AdminTermsController controller = new AdminTermsController(configurationService);
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view = controller.updateTerms("new terms", redirect);

    assertThat(view).isEqualTo("redirect:/admin/terms");
    assertThat(redirect.getFlashAttributes().get("successMessage"))
        .isEqualTo("Terms updated successfully.");
    verify(configurationService).updateTermsContent("new terms");
  }

  @Test
  void updateTermsShowsErrorFlashOnValidationError() {
    AdminTermsController controller = new AdminTermsController(configurationService);
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    doThrow(new IllegalArgumentException("Too short")).when(configurationService).updateTermsContent("x");

    String view = controller.updateTerms("x", redirect);

    assertThat(view).isEqualTo("redirect:/admin/terms");
    assertThat(redirect.getFlashAttributes().get("errorMessage")).isEqualTo("Too short");
  }
}
