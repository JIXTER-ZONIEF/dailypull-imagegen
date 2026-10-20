package fr.jixter.dailypull.imagegen.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import fr.jixter.dailypull.imagegen.domain.BatchGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationResult;
import fr.jixter.dailypull.imagegen.domain.ImageModel;
import fr.jixter.dailypull.imagegen.service.ImageGenerationService;
import fr.jixter.dailypull.imagegen.service.LocalMediaUnavailableException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageGenerationControllerTest {

  @Mock private ImageGenerationService service;

  private ImageGenerationController controller;

  @BeforeEach
  void setUp() {
    controller = new ImageGenerationController(service);
  }

  private ImageGenerationResult sampleResult() {
    return new ImageGenerationResult(
        "uuid-1",
        "flux1-schnell-local",
        "SUCCESS",
        "https://cdn.example/img.png",
        "/tmp/img.png",
        Instant.EPOCH);
  }

  @Test
  void generate_returnsServiceResult() throws IOException, InterruptedException {
    ImageGenerationRequest request =
        new ImageGenerationRequest("prompt", ImageModel.FLUX_SCHNELL_LOCAL, null, null, null, null);
    ImageGenerationResult expected = sampleResult();
    when(service.generate(request)).thenReturn(expected);

    var response = controller.generate(request);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(expected);
    verify(service).generate(request);
  }

  @Test
  void generateBatch_delegatesRequestsToService() throws IOException, InterruptedException {
    ImageGenerationRequest request =
        new ImageGenerationRequest("prompt", ImageModel.FLUX_SCHNELL_LOCAL, null, null, null, null);
    BatchGenerationRequest batch = new BatchGenerationRequest(List.of(request), false);
    List<ImageGenerationResult> expected = List.of(sampleResult());
    when(service.generateBatch(batch.requests())).thenReturn(expected);

    var response = controller.generateBatch(batch);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(expected);
    verify(service).generateBatch(batch.requests());
  }

  @Test
  void listModels_returnsAllModelsWithIdAndName() {
    var response = controller.listModels();

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    List<Map<String, String>> body = response.getBody();
    assertThat(body).hasSize(ImageModel.values().length);
    assertThat(body).allSatisfy(m -> assertThat(m).containsKeys("id", "name"));
    // chaque apiId du enum est présent dans la réponse
    assertThat(body)
        .anySatisfy(
            m -> assertThat(m.get("id")).isEqualTo(ImageModel.FLUX_SCHNELL_LOCAL.getApiId()));
  }

  @Test
  void localUnavailable_answers503() {
    var response = controller.localUnavailable(new LocalMediaUnavailableException());

    assertThat(response.getStatusCode().value()).isEqualTo(503);
    assertThat(response.getBody()).containsEntry("status", "UNAVAILABLE");
  }
}
