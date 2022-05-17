package com.gsdd.file.util;

import com.gsdd.constants.FileConstants;
import com.gsdd.constants.GralConstants;
import com.gsdd.constants.NumericConstants;
import com.gsdd.exception.TechnicalException;
import com.gsdd.file.util.model.UploadableSMBFile;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SMBUtil {

  public static NtlmPasswordAuthentication authenticateSMB(String user, String pass) {
    return new NtlmPasswordAuthentication(null, user, pass);
  }

  /**
   * Check if dir exists, and create it if necessary.
   *
   * @param smbo
   * @return
   */
  public static boolean checkDirectory(UploadableSMBFile smbo) {
    boolean b = false;
    try {
      String netUrl = FileConstants.SMB_URL + smbo.getUrl();
      smbo.setAuth(authenticateSMB(smbo.getUser(), smbo.getPass()));
      smbo.setRoute(new SmbFile(netUrl, smbo.getAuth()));
      if (!smbo.getRoute().exists()) {
        smbo.getRoute().mkdirs();
      }
      b = true;
    } catch (SmbException smbe) {
      b = forceReconnect(smbo);
      if (!b) {
        throw new TechnicalException(smbe);
      }
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
    return b;
  }

  /**
   * Check the available size on dir vs what we need to store on it.
   *
   * @param smbo
   * @param minDirSize minimum size for allow store.
   * @return
   */
  public static boolean checkAvailableSpaceOnDir(UploadableSMBFile smbo, Long minDirSize) {
    try {
      return ByteConversor.getMinAvailableSize(smbo.getRoute().getDiskFreeSpace(), minDirSize);
    } catch (Exception smbe) {
      throw new TechnicalException(smbe);
    }
  }

  /**
   * Delete 0B size files.
   *
   * @param smbo
   */
  public static void deleteEmptyFiles(UploadableSMBFile smbo) {
    try {
      SmbFile[] filesOnDir = smbo.getRoute().listFiles();
      for (SmbFile file : filesOnDir) {
        if (file.length() == NumericConstants.ZERO && file.isFile()) {
          deleteFile(file);
        }
      }
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  /**
   * Allow to delete the oldests files from route.
   *
   * @param smbo
   * @return
   */
  public static boolean deleteOldFiles(UploadableSMBFile smbo) {
    boolean deleted = false;
    try {
      List<SmbFile> smbFiles = getFilesSortedByLastModification(smbo);
      int currentSize = smbFiles.size();
      int size = smbFiles.size();
      for (SmbFile file : smbFiles) {
        if (file.isFile() && deleteFile(file)) {
          size--;
        }
      }
      deleted = size < currentSize;
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
    return deleted;
  }

  /**
   * Allows to transfer a file using SMB.
   *
   * @param smbf smb file
   * @param file local file
   * @param transferSpeed how many bytes to read/transfer
   * @param printStep for print the action
   * @return
   */
  public static boolean transferFile(
      UploadableSMBFile smbf, String file, Integer transferSpeed, Integer printStep) {
    SmbFileOutputStream smbos = null;
    FileInputStream fis = null;
    try {
      File local = new File(file);
      if (local.exists()) {
        smbf.setRoute(new SmbFile(smbf.getUrl(), smbf.getAuth()));
        SmbFile smbFile = new SmbFile(smbf.getUrl() + local.getName(), smbf.getAuth());
        smbos = new SmbFileOutputStream(smbFile);
        fis = new FileInputStream(local);
        byte[] buf = new byte[transferSpeed];
        int read = NumericConstants.ZERO;
        int count = NumericConstants.ZERO;
        int sum = NumericConstants.ZERO;
        while ((read = fis.read(buf)) > NumericConstants.ZERO) {
          smbos.write(buf, NumericConstants.ZERO, read);
          sum += read;
          if (count == NumericConstants.ZERO || (count % printStep) == NumericConstants.ZERO) {
            showProgress(smbFile, sum);
          }
          count++;
          smbos.flush();
        }
        return true;
      }
      return false;
    } catch (Exception e) {
      throw new TechnicalException(e);
    } finally {
      IOUtils.closeQuietly(fis);
      IOUtils.closeQuietly(smbos);
    }
  }

  public static List<SmbFile> getFilesSortedByLastModification(UploadableSMBFile smbf) {
    List<SmbFile> filesOnDir = new ArrayList<>();
    List<SmbFile> smbFiles = new ArrayList<>();
    try {
      filesOnDir = Arrays.asList(getFilesFromDir(smbf));
      for (SmbFile file : filesOnDir) {
        if (!file.isDirectory()) {
          smbFiles.add(file);
        }
      }
      Comparator<SmbFile> smbFileComparator =
          (SmbFile p1, SmbFile p2) -> Long.compare(p1.getLastModified(), p2.getLastModified());
      Collections.sort(smbFiles, smbFileComparator);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return smbFiles;
  }

  /**
   * Shows the progress for upload operation just if log is at INFO level
   *
   * @param smbFile
   * @param sum
   */
  private static void showProgress(SmbFile smbFile, int sum) {
    if (log.isInfoEnabled()) {
      StringBuilder progress = new StringBuilder();
      progress.append(smbFile);
      progress.append(GralConstants.COLON);
      progress.append(ByteConversor.readableFileSize((long) sum));
      progress.append("\n");
      log.info("{}", progress);
    }
  }

  /**
   * Try to reconnect with no credentials.
   *
   * @param smbo
   * @return
   */
  private static boolean forceReconnect(UploadableSMBFile smbo) {
    boolean b = false;
    if (!smbo.isReconnect()) {
      smbo.setUser(null);
      smbo.setPass(null);
      smbo.setReconnect(Boolean.TRUE);
      b = checkDirectory(smbo);
    }
    return b;
  }

  private static boolean deleteFile(SmbFile archivo) {
    boolean b = false;
    try {
      archivo.delete();
      b = true;
    } catch (SmbException smb) {
      log.error(smb.getMessage(), smb);
    }
    return b;
  }

  private static SmbFile[] getFilesFromDir(UploadableSMBFile smbf) throws SmbException {
    return Optional.ofNullable(smbf.getRoute().listFiles()).orElseGet(() -> new SmbFile[0]);
  }
}
