package com.homeops.backend.asset;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(String assetId) {
        super("Asset with id '%s' was not found".formatted(assetId));
    }
}