package com.gsdd.file.util;

import com.gsdd.constants.FileConstants;
import com.gsdd.constants.GralConstants;
import com.gsdd.constants.NumericConstants;
import com.gsdd.exception.TechnicalException;
import com.gsdd.file.util.model.UploadableFtpFile;
import com.gsdd.validatorutil.ValidatorUtil;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

@Slf4j
@UtilityClass
public final class FtpUtil {

  public static boolean connect(UploadableFtpFile ftpFile, FTPClient client) {
    try {
      client.connect(ftpFile.getServer(), ftpFile.getPort());
      int replyCode = client.getReplyCode();
      if (!FTPReply.isPositiveCompletion(replyCode)) {
        return false;
      }
      boolean success = client.login(ftpFile.getUser(), ftpFile.getPass());
      if (!success) {
        return false;
      }
      client.enterLocalPassiveMode();
      client.setFileType(FTP.BINARY_FILE_TYPE);
      // control FTP timeout
      client.setControlKeepAliveTimeout(null);
      client.setKeepAlive(true);
      return true;
    } catch (Exception ioe) {
      throw new TechnicalException(ioe);
    }
  }

  public static void disconnect(FTPClient client) {
    if (client.isConnected()) {
      try {
        client.logout();
        client.disconnect();
      } catch (IOException ioe) {
        throw new TechnicalException(ioe);
      }
    }
  }

  /**
   * Check directory if not exists it try to create it.
   *
   * @see <a href=
   *     "http://www.codejava.net/java-se/networking/ftp/java-ftp-create-directory-example">codejava.net</a>
   * @since 1.0
   * @param ftpFile connection data
   * @param client
   * @param ftpDir directory to check
   * @return true if exists.
   */
  public static boolean checkDirectory(UploadableFtpFile ftpFile, FTPClient client, String ftpDir) {
    boolean exists = true;
    try (InputStream is = client.retrieveFileStream(ftpDir)) {
      int returnCode = client.getReplyCode();
      if (is == null || returnCode == FileConstants.FTP550) {
        exists = client.makeDirectory(ftpDir);
        showServerReply(ftpFile, client);
      }
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
    return exists;
  }

  public static boolean checkAvailableSpaceOnDir(FTPClient client, String ftpDir, Long minSize) {
    boolean check = false;
    try {
      FTPFile[] files = client.listDirectories(ftpDir);
      if (!ValidatorUtil.isNullOrEmpty(files)) {
        check =
            ByteConverter.MIN_AVAILABLE_SIZE.test(files[NumericConstants.ZERO].getSize(), minSize);
      }
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
    return check;
  }

  /**
   * Get files on a directory sorted by last modification.
   *
   * @param client
   * @param route
   * @return
   */
  public static List<FTPFile> getFilesSortedByLastModification(FTPClient client, String route) {
    List<FTPFile> ftpFiles = new ArrayList<>();
    try {
      ftpFiles =
          Arrays.stream(getFilesFromDir(client, route))
              .filter(ftpFile -> !ftpFile.isDirectory())
              .collect(Collectors.toList());
      Comparator<FTPFile> ftpFileComparator = Comparator.comparing(FTPFile::getTimestamp);
      ftpFiles.sort(ftpFileComparator);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return ftpFiles;
  }

  /**
   * Transfer files through FTP using inputstream
   *
   * @see <a href=
   *     "http://www.codejava.net/java-se/networking/ftp/java-ftp-file-upload-tutorial-and-example">codejava.net</a>
   * @param client
   * @param route
   * @param ftpRoute
   * @return
   */
  public static boolean transferFileIs(FTPClient client, String route, String ftpRoute) {
    try (InputStream is = new FileInputStream(route)) {
      return client.storeFile(ftpRoute, is);
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  /**
   * Transfer files through FTP using outputstream and sends noop for avoid disconnection, this
   * method should be used for big files.
   *
   * @see <a href=
   *     "http://www.codejava.net/java-se/networking/ftp/java-ftp-file-upload-tutorial-and-example">codejava.net</a>
   * @param ftpFile
   * @param client
   * @param route
   * @param ftpRoute
   * @param transferSpeed
   * @param printStep
   * @return
   */
  public static boolean transferFileOS(
      UploadableFtpFile ftpFile,
      FTPClient client,
      String route,
      String ftpRoute,
      int transferSpeed,
      int printStep) {
    try (InputStream is = new FileInputStream(route);
        OutputStream os = client.storeFileStream(ftpRoute)) {
      byte[] bytesIn = new byte[transferSpeed];
      int read = NumericConstants.ZERO;
      int count = NumericConstants.ZERO;
      int sum = NumericConstants.ZERO;
      while ((read = is.read(bytesIn)) != NumericConstants.MINUS_ONE) {
        os.write(bytesIn, NumericConstants.ZERO, read);
        sum += read;
        if (count == NumericConstants.ZERO || (count % printStep) == NumericConstants.ZERO) {
          client.sendNoOp();
          showProgress(ftpRoute, sum);
        }
        count++;
        os.flush();
      }
      showServerReply(ftpFile, client);
      return client.completePendingCommand();
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  /**
   * It allows to get/download a file from FTP.
   *
   * @see <a href=
   *     "http://www.codejava.net/java-se/networking/ftp/java-ftp-file-download-tutorial-and-example">codejava.net</a>
   * @param client
   * @param route
   * @param ftpRoute
   * @return
   */
  public static boolean receiveFile(FTPClient client, String route, String ftpRoute) {
    boolean received;
    try (FileOutputStream fos = new FileOutputStream(route);
        BufferedOutputStream bos = new BufferedOutputStream(fos)) {
      received = client.retrieveFile(ftpRoute, bos);
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
    return received;
  }

  /**
   * Delete 0B size file in a directory.
   *
   * @param client
   * @param directory
   */
  public static void deleteEmptyFiles(FTPClient client, String directory) {
    try {
      List<FTPFile> ftpFiles = getFilesSortedByLastModification(client, directory);
      for (FTPFile ftp : ftpFiles) {
        if (ftp.getSize() == NumericConstants.ZERO && ftp.isFile()) {
          client.deleteFile(directory + ftp.getName());
        }
      }
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  /**
   * Allows to delete the oldest files in a directory.
   *
   * @see <a href=
   *     "http://www.codejava.net/java-se/networking/ftp/delete-a-file-on-a-ftp-server">codejava.net</a>
   * @param client
   * @param directory
   * @return
   */
  public static boolean deleteOldFiles(FTPClient client, String directory, int backup) {
    boolean deleted;
    try {
      List<FTPFile> ftpFiles = getFilesSortedByLastModification(client, directory);
      int currentSize = ftpFiles.size();
      int size = ftpFiles.size();
      for (FTPFile ftp : ftpFiles) {
        if (size == backup) {
          break;
        }
        if (ftp.isFile() && client.deleteFile(directory + ftp.getName())) {
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
   * Get list of files from a directory.
   *
   * @param client
   * @param route
   * @return
   * @throws IOException
   */
  private static FTPFile[] getFilesFromDir(FTPClient client, String route) throws IOException {
    return Optional.ofNullable(client.listFiles(route)).orElseGet(() -> new FTPFile[0]);
  }

  /**
   * Show the FTP messages just if logger is at INFO level.
   *
   * @param ftpFile
   * @param client
   */
  private static void showServerReply(UploadableFtpFile ftpFile, FTPClient client) {
    if (log.isInfoEnabled() && ftpFile.isEnableReply()) {
      String[] replies = client.getReplyStrings();
      if (!ValidatorUtil.isNullOrEmpty(replies)) {
        for (String aReply : replies) {
          log.info("{}", GralConstants.SYS_OUT + aReply);
        }
      }
    }
  }

  /**
   * Shows the progress for upload operation just if log is at INFO level
   *
   * @param ftpRoute
   * @param sum
   */
  private static void showProgress(String ftpRoute, int sum) {
    if (log.isInfoEnabled()) {
      StringBuilder progress = new StringBuilder();
      progress.append(ftpRoute);
      progress.append(GralConstants.COLON);
      progress.append(ByteConverter.readableFileSize((long) sum));
      progress.append("\n");
      log.info("{}", progress);
    }
  }
}
