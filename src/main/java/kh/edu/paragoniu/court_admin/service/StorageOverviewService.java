package kh.edu.paragoniu.court_admin.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import kh.edu.paragoniu.court_shared.config.S3Config;
import kh.edu.paragoniu.court_shared.dto.storage.PrefixBreakdownDTO;
import kh.edu.paragoniu.court_shared.dto.storage.RecentUploadDTO;
import kh.edu.paragoniu.court_shared.dto.storage.StorageOverviewDTO;

@Service
public class StorageOverviewService {

    private static final int RECENT_UPLOADS_LIMIT = 15;
    private static final List<String> KNOWN_PREFIXES = List.of("profiles/users/", "participants/", "judges/", "lawyers/", "users/", "documents/");

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Config s3Config;

    public StorageOverviewDTO getOverview() {
        List<S3Object> allObjects = listAllObjects();

        long totalCount = allObjects.size();
        long totalBytes = allObjects.stream().mapToLong(S3Object::size).sum();

        List<S3Object> uncategorized = allObjects.stream()
            .filter(o -> KNOWN_PREFIXES.stream().noneMatch(prefix -> o.key().startsWith(prefix)))
            .toList();

        List<PrefixBreakdownDTO> breakdown = KNOWN_PREFIXES.stream()
            .map(prefix -> {
                List<S3Object> matching = allObjects.stream()
                    .filter(o -> o.key().startsWith(prefix))
                    .toList();
                long bytes = matching.stream().mapToLong(S3Object::size).sum();
                return new PrefixBreakdownDTO(prefix, matching.size(), formatSize(bytes));
            })
            .toList();

        if (!uncategorized.isEmpty()) {
            long bytes = uncategorized.stream().mapToLong(S3Object::size).sum();
            breakdown.add(new PrefixBreakdownDTO("(uncategorized)", uncategorized.size(), formatSize(bytes)));
}

        List<RecentUploadDTO> recent = allObjects.stream()
            .sorted(Comparator.comparing(S3Object::lastModified).reversed())
            .limit(RECENT_UPLOADS_LIMIT)
            .map(o -> new RecentUploadDTO(o.key(), formatSize(o.size()), o.lastModified()))
            .toList();

        return new StorageOverviewDTO(totalCount, formatSize(totalBytes), breakdown, recent);
    }

    private List<S3Object> listAllObjects() {
        List<S3Object> all = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(s3Config.getBucketName())
                .maxKeys(1000);

            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            all.addAll(response.contents());
            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;

        } while (continuationToken != null);

        return all;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}