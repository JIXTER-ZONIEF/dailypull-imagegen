package fr.jixter.dailypull.imagegen.domain;

public enum ImageModel {
  FLUX_SCHNELL_LOCAL("flux1-schnell-local", "FLUX.1 Schnell (local, PC Windows)");

  private final String apiId;
  private final String displayName;

  ImageModel(String apiId, String displayName) {
    this.apiId = apiId;
    this.displayName = displayName;
  }

  public String getApiId() {
    return apiId;
  }

  public String getDisplayName() {
    return displayName;
  }
}
