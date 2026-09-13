package fr.jixter.dailypull.imagegen.service;

import fr.jixter.dailypull.imagegen.client.LocalMediaClient;
import fr.jixter.dailypull.imagegen.client.OneMinAiClient;
import fr.jixter.dailypull.imagegen.config.ImageGenConfig;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImageGenerationService {

  private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

  private final OneMinAiClient client;
  private final ImageGenConfig config;
  private final LocalMediaClient localClient;

  public ImageGenerationService(
      OneMinAiClient client, ImageGenConfig config, LocalMediaClient localClient) {
    this.client = client;
    this.config = config;
    this.localClient = localClient;
  }

  public ImageGenerationResult generate(ImageGenerationRequest request)
      throws IOException, InterruptedException {
    var local = localClient.generate(request);
    if (local.isPresent()) return local.get();
    ImageGenerationResult result = client.generateImage(request);

    if (result.imageUrl() != null && !result.imageUrl().isBlank()) {
      String localPath = downloadAndSave(result);
      result =
          new ImageGenerationResult(
              result.uuid(),
              result.model(),
              "REVIEW_REQUIRED",
              result.imageUrl(),
              localPath,
              result.createdAt());
    }

    // Le fournisseur legacy ne dispense pas de la revue des droits, faits et rendu.
    return new ImageGenerationResult(
        result.uuid(),
        result.model(),
        "REVIEW_REQUIRED",
        result.imageUrl(),
        result.localPath(),
        result.createdAt());
  }

  public List<ImageGenerationResult> generateBatch(List<ImageGenerationRequest> requests)
      throws IOException, InterruptedException {
    List<ImageGenerationResult> results = new ArrayList<>();
    for (int i = 0; i < requests.size(); i++) {
      log.info("Génération {}/{}", i + 1, requests.size());
      results.add(generate(requests.get(i)));
    }
    return results;
  }

  private String downloadAndSave(ImageGenerationResult result)
      throws IOException, InterruptedException {
    Path outputDir = Path.of(config.outputDir());
    Files.createDirectories(outputDir);

    String filename = "%s_%s.png".formatted(result.model().replace("/", "_"), result.uuid());
    Path outputPath = outputDir.resolve(filename);

    byte[] imageBytes = client.downloadImage(result.imageUrl());
    Files.write(outputPath, imageBytes);

    log.info("Image sauvegardée : {}", outputPath.toAbsolutePath());
    return outputPath.toAbsolutePath().toString();
  }
}
