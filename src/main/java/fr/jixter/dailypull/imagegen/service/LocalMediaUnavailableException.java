package fr.jixter.dailypull.imagegen.service;

/** Le fournisseur local n'a rien produit ; aucun fournisseur distant ne prend le relais. */
public class LocalMediaUnavailableException extends IllegalStateException {
  public LocalMediaUnavailableException() {
    super("Fournisseur local indisponible : relancer quand le PC Windows répond.");
  }
}
