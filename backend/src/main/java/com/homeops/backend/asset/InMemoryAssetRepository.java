package com.homeops.backend.asset;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAssetRepository {

    private final ConcurrentHashMap<String, Asset> assets = new ConcurrentHashMap<>();

    public Asset save(Asset asset) {
        assets.put(asset.id(), asset);
        return asset;
    }

    public List<Asset> findAll() {
        List<Asset> storedAssets = new ArrayList<>(assets.values());
        storedAssets.sort(Comparator.comparing(Asset::createdAt));
        return storedAssets;
    }

    public Optional<Asset> findById(String assetId) {
        return Optional.ofNullable(assets.get(assetId));
    }

    public void deleteById(String assetId) {
        assets.remove(assetId);
    }

    public void clear() {
        assets.clear();
    }
}