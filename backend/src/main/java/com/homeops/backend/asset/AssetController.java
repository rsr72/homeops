package com.homeops.backend.asset;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse createAsset(@Valid @RequestBody CreateAssetRequest request) {
        return AssetResponse.from(assetService.createAsset(request));
    }

    @GetMapping
    public List<AssetResponse> getAssets() {
        return assetService.getAssets().stream()
                .map(AssetResponse::from)
                .toList();
    }

    @GetMapping("/{assetId}")
    public AssetResponse getAsset(@PathVariable String assetId) {
        return AssetResponse.from(assetService.getAsset(assetId));
    }

    @PutMapping("/{assetId}")
    public AssetResponse updateAsset(@PathVariable String assetId, @Valid @RequestBody UpdateAssetRequest request) {
        return AssetResponse.from(assetService.updateAsset(assetId, request));
    }

    @DeleteMapping("/{assetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAsset(@PathVariable String assetId) {
        assetService.deleteAsset(assetId);
    }
}