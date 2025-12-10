// src/main/java/dwacademy/mylms001/controller/MediaController.java
package com.lms.mainpages.controller;

import com.lms.mainpages.repository.CourseMaterialRepository;
import com.lms.mainpages.repository.CourseMaterialRepository.Material;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequestMapping("/media")
public class MediaController {

    private final CourseMaterialRepository materialRepo;

    public MediaController(CourseMaterialRepository materialRepo) {
        this.materialRepo = materialRepo;
    }

    /** 동영상 스트리밍 (Partial Content / Range 지원) */
    @GetMapping(value = "/course/{materialId}", produces = "video/*")
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable long materialId,
            @RequestHeader HttpHeaders headers
    ) throws IOException {

        Material m = materialRepo.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "자료를 찾을 수 없습니다."));

        Path path = Path.of(m.filePath());
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }

        FileSystemResource resource = new FileSystemResource(path.toFile());

        String type = m.fileType();
        if (type == null || type.isBlank()) {
            String probed = Files.probeContentType(path);
            type = (probed != null && probed.startsWith("video/")) ? probed : "video/mp4";
        }

        ResourceRegion region = resourceRegion(resource, headers);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType(type))
                .body(region);
    }

    private ResourceRegion resourceRegion(Resource video, HttpHeaders headers) throws IOException {
        long contentLength = video.contentLength();
        long chunk = 1024 * 1024; // 1MB

        if (headers.getRange() != null && !headers.getRange().isEmpty()) {
            HttpRange range = headers.getRange().get(0);
            long start = range.getRangeStart(contentLength);
            long end   = range.getRangeEnd(contentLength);
            long rangeLen = Math.min(chunk, end - start + 1);
            return new ResourceRegion(video, start, rangeLen);
        } else {
            long rangeLen = Math.min(chunk, contentLength);
            return new ResourceRegion(video, 0, rangeLen);
        }
    }
}

