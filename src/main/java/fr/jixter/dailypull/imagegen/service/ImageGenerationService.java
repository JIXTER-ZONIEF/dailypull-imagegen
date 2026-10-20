package fr.jixter.dailypull.imagegen.service;

import fr.jixter.dailypull.imagegen.client.LocalMediaClient;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImageGenerationService {

  private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);

  private final LocalMediaClient localClient;

  public ImageGenerationService(LocalMediaClient localClient) {
    this.localClient = localClient;
  }

  public ImageGenerationResult generate(ImageGenerationRequest request)
      throws IOException, InterruptedException {
    // Le secours 1min.ai est abandonné (ops#2908) : sans candidat local, on échoue franchement.
    return localClient.generate(request).orElseThrow(LocalMediaUnavailableException::new);
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
}
