package com.gsdd.file.util;

import com.gsdd.constants.FileConstants;
import com.gsdd.constants.GralConstants;
import com.gsdd.constants.NumericConstants;
import com.gsdd.exception.TechnicalException;
import com.gsdd.file.util.model.UploadableSmbFile;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class SmbUtil {

  public static NtlmPasswordAuthentication authenticateSMB(String user, String pass) {
    return new NtlmPasswordAuthentication(null, user, pass);
  }

  /**
   * Check if dir exists, and create it if necessary.
   *
   * @param smbFile
   * @return
   */
  public static boolean checkDirectory(UploadableSmbFile smbFile) {
    boolean b;
    try {
      String netUrl = FileConstants.SMB_URL + smbFile.getUrl();
      smbFile.setAuth(authenticateSMB(smbFile.getUser(), smbFile.getPass()));
      smbFile.setRoute(new SmbFile(netUrl, smbFile.getAuth()));
      if (!smbFile.getRoute().exists()) {
        smbFile.getRoute().mkdirs();
      }
      b = true;
    } catch (SmbException smbe) {
      b = forceReconnect(smbFile);
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
   * @param smbFile
   * @param minDirSize minimum size for allow store.
   * @return
   */
  public static boolean checkAvailableSpaceOnDir(UploadableSmbFile smbFile, Long minDirSize) {
    try {
      return ByteConverter.MIN_AVAILABLE_SIZE.test(smbFile.getRoute().getDiskFreeSpace(), minDirSize);
    } catch (Exception smbe) {
      throw new TechnicalException(smbe);
    }
  }

  /**
   * Delete 0B size files.
   *
   * @param smbFile
   */
  public static void deleteEmptyFiles(UploadableSmbFile smbFile) {
    try {
      SmbFile[] filesOnDir = smbFile.getRoute().listFiles();
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
   * Allow to delete the oldest files from route.
   *
   * @param smbFile
   * @return
   */
  public static boolean deleteOldFiles(UploadableSmbFile smbFile) {
    boolean deleted;
    try {
      List<SmbFile> smbFiles = getFilesSortedByLastModification(smbFile);
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
   * @param smbFile smb file
   * @param file local file
   * @param transferSpeed how many bytes to read/transfer
   * @param printStep for print the action
   * @return
   */
  public static boolean transferFile(
      UploadableSmbFile smbFile, String file, Integer transferSpeed, Integer printStep) {
    SmbFileOutputStream smbos = null;
    FileInputStream fis = null;
    try {
      File local = new File(file);
      if (local.exists()) {
        smbFile.setRoute(new SmbFile(smbFile.getUrl(), smbFile.getAuth()));
        SmbFile smbFileCopy = new SmbFile(smbFile.getUrl() + local.getName(), smbFile.getAuth());
        smbos = new SmbFileOutputStream(smbFileCopy);
        fis = new FileInputStream(local);
        byte[] buf = new byte[transferSpeed];
        int read = NumericConstants.ZERO;
        int count = NumericConstants.ZERO;
        int sum = NumericConstants.ZERO;
        while ((read = fis.read(buf)) > NumericConstants.ZERO) {
          smbos.write(buf, NumericConstants.ZERO, read);
          sum += read;
          if (count == NumericConstants.ZERO || (count % printStep) == NumericConstants.ZERO) {
            showProgress(smbFileCopy, sum);
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
      IoUtils.closeQuietly(fis);
      IoUtils.closeQuietly(smbos);
    }
  }

  public static List<SmbFile> getFilesSortedByLastModification(UploadableSmbFile smbFile) {
    List<SmbFile> smbFiles = new ArrayList<>();
    try {
      SmbFile[] filesOnDir = getFilesFromDir(smbFile);
      for (SmbFile file : filesOnDir) {
        if (!file.isDirectory()) {
          smbFiles.add(file);
        }
      }
      Comparator<SmbFile> smbFileComparator = Comparator.comparingLong(SmbFile::getLastModified);
      smbFiles.sort(smbFileComparator);
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
      progress.append(ByteConverter.readableFileSize(sum));
      progress.append("\n");
      log.info("{}", progress);
    }
  }

  /**
   * Try to reconnect with no credentials.
   *
   * @param smbFile
   * @return
   */
  private static boolean forceReconnect(UploadableSmbFile smbFile) {
    boolean b = false;
    if (!smbFile.isReconnect()) {
      smbFile.setUser(null);
      smbFile.setPass(null);
      smbFile.setReconnect(Boolean.TRUE);
      b = checkDirectory(smbFile);
    }
    return b;
  }

  private static boolean deleteFile(SmbFile smbFile) {
    boolean b = false;
    try {
      smbFile.delete();
      b = true;
    } catch (SmbException smb) {
      log.error(smb.getMessage(), smb);
    }
    return b;
  }

  private static SmbFile[] getFilesFromDir(UploadableSmbFile smbFile) throws SmbException {
    return Optional.ofNullable(smbFile.getRoute().listFiles()).orElseGet(() -> new SmbFile[0]);
  }
}
