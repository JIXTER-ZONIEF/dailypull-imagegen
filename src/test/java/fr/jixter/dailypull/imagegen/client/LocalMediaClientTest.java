package fr.jixter.dailypull.imagegen.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

class LocalMediaClientTest {
  @TempDir Path directory;
  private final ObjectMapper mapper = new ObjectMapper();

  private LocalMediaClient helper(String code) throws Exception {
    Path script = directory.resolve("helper.py");
    Files.writeString(script, code);
    return new LocalMediaClient(script.toString(), true, mapper);
  }

  private ImageGenerationRequest request() {
    return new ImageGenerationRequest("A brass compass", null, null, null, null, null);
  }

  @Test
  void offlineWindowsSignalsLegacyWithoutError() throws Exception {
    var client = helper("import sys\nsys.stdin.read()\nsys.exit(20)\n");
    assertThat(client.generate(request())).isEmpty();
  }

  @Test
  void doctrineRefusalDoesNotSignalFallback() throws Exception {
    var client = helper("import sys\nsys.stdin.read()\nsys.exit(30)\n");
    assertThatThrownBy(() -> client.generate(request()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void localSuccessRequiresExistingCandidateAndEditorialReview() throws Exception {
    Path image = directory.resolve("candidate.png");
    Files.writeString(image, "fixture, not a production image");
    var client =
        helper(
            "import json,sys\ndata=json.load(sys.stdin)\nassert data['purpose']=='card'\n"
                + "assert data['app']=='dailypull'\nprint(json.dumps({'status':'REVIEW_REQUIRED','file':"
                + mapper.writeValueAsString(image.toString())
                + "}))\n");
    var result = client.generate(request()).orElseThrow();
    assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
    assertThat(result.model()).isEqualTo("flux1-schnell-local");
    assertThat(result.localPath()).isEqualTo(image.toString());
  }

  @Test
  void malformedLocalResponseReturnsToLegacy() throws Exception {
    var client = helper("import sys\nsys.stdin.read()\nprint('bad JSON')\n");
    assertThat(client.generate(request())).isEmpty();
  }
}
