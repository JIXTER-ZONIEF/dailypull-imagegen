package fr.jixter.dailypull.imagegen.client;

import fr.jixter.dailypull.imagegen.domain.ImageGenerationRequest;
import fr.jixter.dailypull.imagegen.domain.ImageGenerationResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Produit uniquement un candidat à inspecter, jamais une carte publiée. */
@Component
public class LocalMediaClient {
  private final Path script;
  private final boolean enabled;
  private final ObjectMapper mapper;

  public LocalMediaClient(
      @Value("${imagegen.local-script:}") String script,
      @Value("${imagegen.local-enabled:true}") boolean enabled,
      ObjectMapper mapper) {
    this.script =
        script.isBlank()
            ? Path.of(System.getProperty("user.home"), "scripts", "local-media.py")
            : Path.of(script);
    this.enabled = enabled;
    this.mapper = mapper;
  }

  public Optional<ImageGenerationResult> generate(ImageGenerationRequest request)
      throws IOException, InterruptedException {
    if (!enabled || !Files.isRegularFile(script)) return Optional.empty();
    // Pas de promesse implicite sur un format ignoré : legacy pour les tailles non locales.
    if (!List.of("768x768", "768x1024", "1024x1024", "1024x1280", "1536x1024")
        .contains(request.size())) {
      return Optional.empty();
    }
    String[] size = request.size().split("x");
    Map<String, Object> spec =
        Map.of(
            "app",
            "dailypull",
            "purpose",
            "card",
            "prompt",
            request.prompt(),
            "size",
            List.of(Integer.parseInt(size[0]), Integer.parseInt(size[1])));
    Path output = Files.createTempFile("dailypull-local-", ".json");
    Process process = null;
    try {
      process =
          new ProcessBuilder("python3", script.toString(), "draft", "--spec", "-")
              .redirectOutput(output.toFile())
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      try (var input = process.getOutputStream()) {
        input.write(mapper.writeValueAsBytes(spec));
      }
      if (!process.waitFor(155, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return Optional.empty();
      }
      if (process.exitValue() == 20) return Optional.empty();
      if (process.exitValue() != 0) {
        // Un refus de doctrine ne peut pas être contourné en changeant de fournisseur.
        throw new IllegalArgumentException(
            "Brief local refusé : vérifier les doctrines avant toute génération.");
      }
      var response = mapper.readTree(Files.readString(output, StandardCharsets.UTF_8));
      if (!"REVIEW_REQUIRED".equals(response.path("status").asText())) {
        throw new IOException("Réponse locale sans statut de revue");
      }
      Path file = Path.of(response.path("file").asText());
      if (!Files.isRegularFile(file)) throw new IOException("Candidat local absent");
      return Optional.of(
          new ImageGenerationResult(
              UUID.randomUUID().toString(),
              "flux1-schnell-local",
              "REVIEW_REQUIRED",
              null,
              file.toAbsolutePath().toString(),
              Instant.now()));
    } catch (IOException | tools.jackson.core.JacksonException e) {
      // Dépendance locale indisponible ; l'ancien client reste utilisable.
      return Optional.empty();
    } finally {
      if (process != null && process.isAlive()) process.destroyForcibly();
      Files.deleteIfExists(output);
    }
  }
}
