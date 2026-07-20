package com.suchika.gateway.system;

import com.suchika.shared.logging.AppLogger;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Path("/v1/system/documents")
public class DocumentResource {

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDocument(@PathParam("name") String name) {
        if (name == null || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid document name").build();
        }

        if (!name.endsWith(".md")) {
            name = name + ".md";
        }

        try {
            // First try reading from local file system by walking up the directory tree (for development and tests)
            java.nio.file.Path current = Paths.get("").toAbsolutePath();
            java.nio.file.Path localPath = null;
            while (current != null) {
                java.nio.file.Path p = current.resolve("documents").resolve(name);
                java.nio.file.Path rootReadme = current.resolve("README.md");
                
                if (Files.exists(p)) {
                    localPath = p;
                    break;
                } else if (name.equals("README.md") && Files.exists(rootReadme)) {
                    localPath = rootReadme;
                    break;
                }
                current = current.getParent();
            }
            if (localPath != null) {
                String content = Files.readString(localPath, StandardCharsets.UTF_8);
                return Response.ok(content).build();
            }

            // Fallback to classpath (for production jar)
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("documents/" + name)) {
                if (is != null) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    return Response.ok(content).build();
                }
            }
        } catch (IOException e) {
            AppLogger.error("Failed to read document: " + name, e);
            return Response.serverError().entity("Error reading document").build();
        }

        return Response.status(Response.Status.NOT_FOUND).entity("Document not found: " + name).build();
    }
}
