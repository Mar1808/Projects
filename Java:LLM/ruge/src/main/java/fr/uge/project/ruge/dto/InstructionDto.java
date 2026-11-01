package fr.uge.project.ruge.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructionDto {

  private Long id;

  @NotBlank(message = "Step description is required")
  private String step;

  @NotNull(message = "Duration is required")
  @Min(value = 0, message = "Duration must be zero or positive")
  private Integer duration;
}
