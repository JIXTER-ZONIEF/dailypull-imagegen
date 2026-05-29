package fr.jixter.dailypull.imagegen.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import fr.jixter.dailypull.imagegen.config.OneMinAiConfig;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationResult;
import fr.jixter.dailypull.imagegen.domain.ImageModel;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OneMinAiClientTest {

  @Mock private HttpClient httpClient;

  @Mock private HttpResponse<String> httpResponse;

  private OneMinAiClient client;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    OneMinAiConfig config = new OneMinAiConfig("https://api.1min.ai", "test-api-key");
    client = new OneMinAiClient(httpClient, config, objectMapper);
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_success() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "aiRecord": {
            "uuid": "test-uuid-123",
            "status": "SUCCESS",
            "temporaryUrl": "https://s3.amazonaws.com/test-image.png",
            "aiRecordDetail": {
              "resultObject": ["images/test.png"]
            }
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

    ImageGenerationRequest request =
        new ImageGenerationRequest(
            "A motivational card with sunrise",
            ImageModel.DALL_E_3,
            "1024x1024",
            "hd",
            "vivid",
            "png");

    ImageGenerationResult result = client.generateImage(request);

    assertThat(result.uuid()).isEqualTo("test-uuid-123");
    assertThat(result.status()).isEqualTo("SUCCESS");
    assertThat(result.imageUrl()).isEqualTo("https://s3.amazonaws.com/test-image.png");
    assertThat(result.model()).isEqualTo("dall-e-3");
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_apiError_throwsException() throws IOException, InterruptedException {
    when(httpResponse.statusCode()).thenReturn(401);
    when(httpResponse.body()).thenReturn("Unauthorized");
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

    ImageGenerationRequest request =
        new ImageGenerationRequest(
            "Test prompt", ImageModel.DALL_E_3, "1024x1024", "hd", "vivid", "png");

    assertThatThrownBy(() -> client.generateImage(request))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Erreur API 1minai (HTTP 401)");
  }

  @Test
  @SuppressWarnings("unchecked")
  void downloadImage_success_returnsBytes() throws IOException, InterruptedException {
    byte[] expected = {(byte) 0x89, 0x50, 0x4E, 0x47};
    HttpResponse<byte[]> byteResponse = mock(HttpResponse.class);
    when(byteResponse.statusCode()).thenReturn(200);
    when(byteResponse.body()).thenReturn(expected);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(byteResponse);

    byte[] result = client.downloadImage("https://cdn.example/image.png");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  @SuppressWarnings("unchecked")
  void downloadImage_error_throwsException() throws IOException, InterruptedException {
    HttpResponse<byte[]> byteResponse = mock(HttpResponse.class);
    when(byteResponse.statusCode()).thenReturn(404);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(byteResponse);

    assertThatThrownBy(() -> client.downloadImage("https://cdn.example/missing.png"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Erreur téléchargement image (HTTP 404)");
  }

  @Test
  @SuppressWarnings("unchecked")
  void generateImage_fluxModel_usesAspectRatio() throws IOException, InterruptedException {
    String responseJson =
        """
        {
          "aiRecord": {
            "uuid": "flux-uuid",
            "status": "SUCCESS",
            "temporaryUrl": "https://s3.amazonaws.com/flux-image.png",
            "aiRecordDetail": {
              "resultObject": ["images/flux.png"]
            }
          }
        }
        """;

    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.body()).thenReturn(responseJson);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

    ImageGenerationRequest request =
        new ImageGenerationRequest(
            "Test flux", ImageModel.FLUX_PRO_1_1, "1024x1024", "hd", "vivid", "png");

    ImageGenerationResult result = client.generateImage(request);

    assertThat(result.uuid()).isEqualTo("flux-uuid");
    assertThat(result.model()).isEqualTo("black-forest-labs/flux-1.1-pro");
  }
}
