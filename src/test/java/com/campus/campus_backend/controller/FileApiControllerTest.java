package com.campus.campus_backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileApiControllerTest {
    @TempDir
    Path tempDir;

    @Test
    void servesOnlyImagesWithinUploadDirectory() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("uploads"));
        Path images = Files.createDirectory(root.resolve("avatars"));
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png",
                images.resolve("avatar.png").toFile());
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png",
                tempDir.resolve("private.png").toFile());
        Files.writeString(root.resolve("note.html"), "<script>alert(1)</script>");

        FileApiController controller = new FileApiController();
        ReflectionTestUtils.setField(controller, "uploadPath", root.toString());

        assertEquals(200, controller.getFile(request("/api/uploads/avatars/avatar.png")).getStatusCode().value());
        assertEquals(404, controller.getFile(request("/api/uploads/../private.png")).getStatusCode().value());
        assertEquals(404, controller.getFile(request("/api/uploads/note.html")).getStatusCode().value());
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }
}
