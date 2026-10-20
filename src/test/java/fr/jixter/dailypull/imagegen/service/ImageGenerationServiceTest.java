package fr.jixter.dailypull.imagegen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import fr.jixter.dailypull.imagegen.client.LocalMediaClient;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageGenerationServiceTest {

  @Mock private LocalMediaClient localClient;

  private ImageGenerationService service;

  @BeforeEach
  void setUp() {
    service = new ImageGenerationService(localClient);
  }

  private ImageGenerationRequest request() {
    return new ImageGenerationRequest("a prompt", null, null, null, null, null);
  }

  private ImageGenerationResult local() {
    return new ImageGenerationResult(
        "local",
        "flux1-schnell-local",
        "REVIEW_REQUIRED",
        null,
        "/tmp/candidate.png",
        Instant.EPOCH);
  }

  @Test
  void localCandidateIsReturnedForReview() throws Exception {
    when(localClient.generate(any())).thenReturn(Optional.of(local()));

    assertThat(service.generate(request())).isEqualTo(local());
  }

  @Test
  void localUnavailableNeverFallsBackToAnotherProvider() throws Exception {
    when(localClient.generate(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.generate(request()))
        .isInstanceOf(LocalMediaUnavailableException.class);
  }

  @Test
  void rejectedDoctrineIsPropagated() throws Exception {
    when(localClient.generate(any())).thenThrow(new IllegalArgumentException("refus"));

    assertThatThrownBy(() -> service.generate(request()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void generateBatch_generatesEachRequest() throws Exception {
    when(localClient.generate(any())).thenReturn(Optional.of(local()));

    List<ImageGenerationResult> results = service.generateBatch(List.of(request(), request()));

    assertThat(results).hasSize(2);
    verify(localClient, times(2)).generate(any());
  }
}
