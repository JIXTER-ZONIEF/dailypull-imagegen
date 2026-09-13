package fr.jixter.dailypull.imagegen.controller;

import fr.jixter.dailypull.imagegen.domain.BatchGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationResult;
import fr.jixter.dailypull.imagegen.domain.ImageModel;
import fr.jixter.dailypull.imagegen.service.ImageGenerationService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/images")
public class ImageGenerationController {

  private final ImageGenerationService service;

  public ImageGenerationController(ImageGenerationService service) {
    this.service = service;
  }

  @PostMapping("/generate")
  public ResponseEntity<ImageGenerationResult> generate(
      @Valid @RequestBody ImageGenerationRequest request) throws IOException, InterruptedException {
    return ResponseEntity.ok(service.generate(request));
  }

  @PostMapping("/generate/batch")
  public ResponseEntity<List<ImageGenerationResult>> generateBatch(
      @Valid @RequestBody BatchGenerationRequest request) throws IOException, InterruptedException {
    return ResponseEntity.ok(service.generateBatch(request.requests()));
  }

  @GetMapping("/models")
  public ResponseEntity<List<Map<String, String>>> listModels() {
    List<Map<String, String>> models =
        Arrays.stream(ImageModel.values())
            .map(m -> Map.<String, String>of("id", m.getApiId(), "name", m.getDisplayName()))
            .toList();
    return ResponseEntity.ok(models);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> rejectedBrief(IllegalArgumentException exception) {
    return ResponseEntity.badRequest()
        .body(Map.of("status", "REJECTED", "reason", exception.getMessage()));
  }
}
