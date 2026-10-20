package fr.jixter.dailypull.imagegen.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ImageGenerationRequest(
    @NotBlank String prompt,
    ImageModel model,
    String size,
    String quality,
    String style,
    String outputFormat) {

  public ImageGenerationRequest {
    if (model == null) model = ImageModel.FLUX_SCHNELL_LOCAL;
    if (size == null) size = "1024x1024";
    if (quality == null) quality = "hd";
    if (style == null) style = "vivid";
    if (outputFormat == null) outputFormat = "png";
  }
}
