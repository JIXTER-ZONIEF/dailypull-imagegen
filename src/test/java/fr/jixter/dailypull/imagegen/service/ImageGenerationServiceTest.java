package fr.jixter.dailypull.imagegen.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import fr.jixter.dailypull.imagegen.client.OneMinAiClient;
import fr.jixter.dailypull.imagegen.config.ImageGenConfig;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationResult;
import fr.jixter.dailypull.imagegen.domain.ImageModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageGenerationServiceTest {

  @Mock private OneMinAiClient client;

  @TempDir Path outputDir;

  private ImageGenerationService service;

  @BeforeEach
  void setUp() {
    service = new ImageGenerationService(client, new ImageGenConfig(outputDir.toString()));
  }

  private ImageGenerationRequest request() {
    return new ImageGenerationRequest("a prompt", ImageModel.DALL_E_3, null, null, null, null);
  }

  private ImageGenerationResult result(String imageUrl) {
    return new ImageGenerationResult(
        "uuid-1", "dall-e-3", "SUCCESS", imageUrl, null, Instant.EPOCH);
  }

  @Test
  void generate_withImageUrl_downloadsAndPopulatesLocalPath()
      throws IOException, InterruptedException {
    ImageGenerationRequest request = request();
    byte[] bytes = {1, 2, 3, 4};
    when(client.generateImage(request)).thenReturn(result("https://cdn.example/img.png"));
    when(client.downloadImage("https://cdn.example/img.png")).thenReturn(bytes);

    ImageGenerationResult result = service.generate(request);

    Path expected = outputDir.resolve("dall-e-3_uuid-1.png");
    assertThat(result.localPath()).isEqualTo(expected.toAbsolutePath().toString());
    assertThat(Files.exists(expected)).isTrue();
    assertThat(Files.readAllBytes(expected)).isEqualTo(bytes);
    // les autres champs sont préservés
    assertThat(result.uuid()).isEqualTo("uuid-1");
    assertThat(result.imageUrl()).isEqualTo("https://cdn.example/img.png");
  }

  @Test
  void generate_withoutImageUrl_returnsResultUnchangedAndSkipsDownload()
      throws IOException, InterruptedException {
    ImageGenerationRequest request = request();
    when(client.generateImage(request)).thenReturn(result(null));

    ImageGenerationResult result = service.generate(request);

    assertThat(result.localPath()).isNull();
    verify(client, never()).downloadImage(any());
  }

  @Test
  void generate_withBlankImageUrl_skipsDownload() throws IOException, InterruptedException {
    ImageGenerationRequest request = request();
    when(client.generateImage(request)).thenReturn(result("   "));

    ImageGenerationResult result = service.generate(request);

    assertThat(result.localPath()).isNull();
    verify(client, never()).downloadImage(any());
  }

  @Test
  void generateBatch_generatesEachRequest() throws IOException, InterruptedException {
    ImageGenerationRequest r1 = request();
    ImageGenerationRequest r2 = request();
    when(client.generateImage(any())).thenReturn(result(null));

    List<ImageGenerationResult> results = service.generateBatch(List.of(r1, r2));

    assertThat(results).hasSize(2);
    verify(client, times(2)).generateImage(any());
    verify(client, never()).downloadImage(any());
  }
}
