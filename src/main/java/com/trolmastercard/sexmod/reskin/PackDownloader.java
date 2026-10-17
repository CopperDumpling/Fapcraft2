package com.trolmastercard.sexmod.reskin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <b>Role.</b> MC-free downloader + extractor for the custom-models archive.
 * <p>
 * Downloads the GitHub archive zip ({@code main.zip}) and extracts every pack
 * into the instance's custom-models directory (e.g.
 * {@code sexmod/custom_models/singleplayer}), stripping the top-level folder
 * GitHub adds ({@code Jenny-Mod-Custom-Models-Archive-main/}) so packs land
 * directly alongside the user's own models.
 * <p>
 * Safety: entries are resolved against the destination with a canonical-path
 * check (no {@code ..} traversal), directories are created, and the whole
 * operation is wrapped so a failure returns a message instead of throwing.
 * Pure Java — no Minecraft imports (headless-testable).
 */
public class PackDownloader {

   /** Source archive. */
   public static final String ARCHIVE_URL = "https://github.com/ReverseEngineeringEnthusiasts/Jenny-Mod-Custom-Models-Archive/archive/refs/heads/main.zip";

   /** GitHub page for "View on Github". */
   public static final String PAGE_URL = "https://github.com/ReverseEngineeringEnthusiasts/Jenny-Mod-Custom-Models-Archive";

   /** Progress sink: {@code total <= 0} means the size is unknown. Called from the worker thread. */
   public interface Progress {
      void update(long done, long total);
   }

   private static final Progress NONE = (done, total) -> { };

   /**
    * Downloads {@code url} into a temp file and returns it (caller cleans up).
    */
   public static File downloadToTemp(String url) throws IOException {
      return downloadToTemp(url, NONE);
   }

   /** As {@link #downloadToTemp(String)}, reporting bytes received against Content-Length. */
   public static File downloadToTemp(String url, Progress progress) throws IOException {
      HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
      conn.setConnectTimeout(15000);
      conn.setReadTimeout(30000);
      conn.setInstanceFollowRedirects(true);
      conn.setRequestProperty("User-Agent", "FapcraftMod/1.1 (sexmod reskin downloader)");
      int code = conn.getResponseCode();
      if (code != 200) {
         throw new IOException("HTTP " + code + " from " + url);
      }
      long length = conn.getContentLengthLong(); // -1 for GitHub's chunked archives
      File tmp = File.createTempFile("sexmod_packs", ".zip");
      tmp.deleteOnExit();
      try (InputStream in = new BufferedInputStream(conn.getInputStream());
           OutputStream out = new FileOutputStream(tmp)) {
         byte[] buf = new byte[65536];
         int n;
         long total = 0;
         while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            total += n;
            progress.update(total, length);
            if (total > 1024L * 1024 * 1024) { // 1GB sanity cap (archive is ~560MB)
               throw new IOException("Archive too large (>1GB)");
            }
         }
      }
      return tmp;
   }

   /**
    * Extracts the zip archive into {@code destDir}, stripping the single top-level
    * folder added by GitHub. Returns a summary string; never throws for corrupt
    * entries individually (they're reported in the summary).
    */
   public static String extractTo(File zip, File destDir) {
      return extractTo(zip, destDir, NONE);
   }

   /** As {@link #extractTo(File, File)}, reporting compressed bytes consumed against the zip size. */
   public static String extractTo(File zip, File destDir, Progress progress) {
      if (zip == null || !zip.isFile()) return "Downloaded file is missing.";
      final long zipSize = zip.length();
      destDir.mkdirs();
      int count = 0;
      long bytes = 0;
      StringBuilder problems = new StringBuilder();
      try (CountingInputStream raw = new CountingInputStream(Files.newInputStream(zip.toPath()));
           ZipInputStream zin = new ZipInputStream(new BufferedInputStream(raw))) {
         ZipEntry entry;
         while ((entry = zin.getNextEntry()) != null) {
            progress.update(raw.count, zipSize);
            String name = entry.getName();
            if (name == null || name.isEmpty() || name.startsWith("/")) continue;
            // strip the single top-level folder (e.g. "Jenny-Mod-Custom-Models-Archive-main/")
            String rel = name;
            int slash = name.indexOf('/');
            if (slash >= 0) {
               rel = name.substring(slash + 1);
            }
            if (rel.isEmpty()) continue; // the top folder itself

            File target = new File(destDir, rel);
            // path traversal guard
            File norm;
            File normDest;
            try {
               norm = target.getCanonicalFile();
               normDest = destDir.getCanonicalFile();
            } catch (IOException e) {
               problems.append("skip ").append(name).append(" (bad path)\n");
               continue;
            }
            if (!norm.getPath().startsWith(normDest.getPath() + File.separator) && !norm.equals(normDest)) {
               problems.append("skip ").append(name).append(" (unsafe path)\n");
               continue;
            }

            if (entry.isDirectory()) {
               target.mkdirs();
               continue;
            }
            File parent = target.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
               problems.append("skip ").append(name).append(" (cannot create dir)\n");
               continue;
            }
            try (OutputStream out = new FileOutputStream(target)) {
               byte[] buf = new byte[65536];
               int n;
               while ((n = zin.read(buf)) > 0) {
                  out.write(buf, 0, n);
                  bytes += n;
               }
               count++;
            } catch (IOException e) {
               problems.append("skip ").append(name).append(" (").append(e.getMessage()).append(")\n");
            }
         }
      } catch (IOException e) {
         return "Extraction failed: " + e.getMessage();
      }
      StringBuilder sb = new StringBuilder();
      sb.append("Installed ").append(count).append(" file(s)");
      if (bytes > 0) sb.append(" (").append(Math.round(bytes / 1024.0)).append(" KB)");
      sb.append(" into ").append(destDir.getAbsolutePath()).append(".");
      if (problems.length() > 0) {
         sb.append(" Some files were skipped:\n").append(problems);
      }
      return sb.toString();
   }

   /** Counts bytes pulled from the underlying stream (for extraction progress). */
   private static final class CountingInputStream extends FilterInputStream {
      volatile long count;

      CountingInputStream(InputStream in) {
         super(in);
      }

      @Override
      public int read() throws IOException {
         int b = super.read();
         if (b >= 0) count++;
         return b;
      }

      @Override
      public int read(byte[] buf, int off, int len) throws IOException {
         int n = super.read(buf, off, len);
         if (n > 0) count += n;
         return n;
      }

      @Override
      public long skip(long n) throws IOException {
         long k = super.skip(n);
         count += k;
         return k;
      }
   }
}