package com.gsdd.file.util;

import com.gsdd.constants.NumericConstants;
import com.gsdd.exception.TechnicalException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUtil {

  /**
   * Check if dir exists, and create it if necessary.
   *
   * @param route
   * @return
   */
  public static boolean checkDirectory(String route) {
    try {
      File f = generateFileFromRoute(route);
      return !f.exists() ? f.mkdirs() : true;
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  /**
   * Check the available size on dir vs what we need to store on it.
   *
   * @param route
   * @param minDirSize minimum size for allow store.
   * @return
   */
  public static boolean checkAvailableSpaceOnDir(String route, Long minDirSize) {
    try {
      return ByteConversor.MIN_AVAILABLE_SIZE.test(
          generateFileFromRoute(route).getFreeSpace(), minDirSize);
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  /**
   * Delete 0B size files.
   *
   * @param route
   */
  public static void deleteEmptyFiles(String route) {
    try {
      File fr = generateFileFromRoute(route);
      File[] filesOnDir = getFilesFromDir(fr);
      Stream.of(filesOnDir)
          .filter(file -> file.isFile() && file.length() == NumericConstants.ZERO)
          .forEach(FileUtil::deleteFile);
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  public static List<File> getFilesSortedByLastModification(String route) {
    File f = generateFileFromRoute(route);
    Comparator<File> fileComparator =
        (File p1, File p2) -> Long.compare(p1.lastModified(), p2.lastModified());
    return Stream.of(getFilesFromDir(f)).sorted(fileComparator).collect(Collectors.toList());
  }

  /**
   * Allow to delete the oldests files from route.
   *
   * @param route
   * @param backup how many files preserve
   * @return
   */
  public static boolean deleteOldFiles(String route, int backup) {
    boolean deleted = false;
    try {
      List<File> filesOnDir = getFilesSortedByLastModification(route);
      int currentSize = filesOnDir.size();
      int size = filesOnDir.size();
      for (File f : filesOnDir) {
        if (size == backup) {
          break;
        }
        if (f.isFile() && deleteFile(f)) {
          size--;
        }
      }
      deleted = size < currentSize;
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
    return deleted;
  }

  public static File getLastModifiedFile(String ruta) {
    try {
      List<File> filesOnDir = getFilesSortedByLastModification(ruta);
      return !filesOnDir.isEmpty()
          ? filesOnDir.get(filesOnDir.size() - NumericConstants.ONE)
          : null;
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  public static boolean deleteFile(File f) {
    boolean b = false;
    if (f != null && f.exists()) {
      try {
        Files.delete(f.toPath());
        b = true;
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    return b;
  }

  public static void zipFileUsingPass(String route, List<File> filesToAdd, String pass) {
    // This is name and path of zip file to be created
    try (ZipFile externalZipFile = new ZipFile(route, pass.toCharArray()); ) {
      // Now add files to the zip file
      ZipParameters parameters = new ZipParameters();
      parameters.setEncryptFiles(true);
      parameters.setEncryptionMethod(EncryptionMethod.AES);
      externalZipFile.addFiles(filesToAdd, parameters);
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  /**
   * Zip files.
   *
   * @since 1.0
   * @param zipName name for zipped file.
   * @param fileName file to compress.
   * @param byteBuffer compression rate.
   * @return true if ok.
   */
  public static boolean zipFile(String zipName, String fileName, int byteBuffer) {
    byte[] buffer = new byte[byteBuffer];
    FileInputStream fis = null;
    try (FileOutputStream fos = new FileOutputStream(zipName);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      int len;
      fis = new FileInputStream(fileName);
      ZipEntry ze = new ZipEntry(zipName);
      zos.putNextEntry(ze);
      while ((len = fis.read(buffer)) > NumericConstants.ZERO) {
        zos.write(buffer, NumericConstants.ZERO, len);
      }
      zos.closeEntry();
      return true;
    } catch (Exception e) {
      throw new TechnicalException(e);
    } finally {
      IoUtils.closeQuietly(fis);
    }
  }

  /**
   * Unzip a zipped file
   *
   * @param inputZip zipped file.
   * @param outDir output for files.
   * @param byteBuffer buffer for unzip.
   * @return true no error.
   */
  public static boolean unzipFile(String inputZip, String outDir, int byteBuffer) {
    FileOutputStream fos = null;
    try (FileInputStream fis = new FileInputStream(inputZip);
        ZipInputStream zis = new ZipInputStream(fis)) {
      ZipEntry zipFile = null;
      while ((zipFile = zis.getNextEntry()) != null) {
        File fileExtracted = new File(outDir + File.separator + zipFile.getName());
        fos = new FileOutputStream(fileExtracted);
        byte[] buffer = new byte[byteBuffer];
        int read;
        while ((read = zis.read(buffer, NumericConstants.ZERO, buffer.length))
            != NumericConstants.ZERO) {
          fos.write(buffer, NumericConstants.ZERO, read);
        }
        fos.flush();
        IoUtils.closeQuietly(fos);
      }
      return true;
    } catch (Exception e) {
      throw new TechnicalException(e);
    } finally {
      IoUtils.closeQuietly(fos);
    }
  }

  private static File generateFileFromRoute(String path) {
    return new File(path);
  }

  private static File[] getFilesFromDir(File fr) {
    return Optional.ofNullable(fr.listFiles()).orElseGet(() -> new File[0]);
  }
}
