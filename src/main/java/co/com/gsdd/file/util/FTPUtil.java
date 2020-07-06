package co.com.gsdd.file.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import co.com.gsdd.constants.FileConstants;
import co.com.gsdd.constants.GralConstants;
import co.com.gsdd.constants.NumericConstants;
import co.com.gsdd.exception.TechnicalException;
import co.com.gsdd.file.util.model.UploadableFTPFile;
import co.com.gsdd.validatorutil.ValidatorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FTPUtil {

    public static boolean connect(UploadableFTPFile ftpo, FTPClient client) {
        try {
            client.connect(ftpo.getServer(), ftpo.getPort());
            int replyCode = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                return false;
            }
            boolean success = client.login(ftpo.getUser(), ftpo.getPass());
            if (!success) {
                return false;
            }
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            // control FTP timeout
            client.setControlKeepAliveTimeout(NumericConstants.MINUS_ONE);
            client.setKeepAlive(true);
            return true;
        } catch (Exception ioe) {
            throw new TechnicalException(ioe);
        }
    }

    public static void disconnect(FTPClient cliente) {
        if (cliente.isConnected()) {
            try {
                cliente.logout();
                cliente.disconnect();
            } catch (IOException ioe) {
                throw new TechnicalException(ioe);
            }
        }
    }

    /**
     * Check directory if not exists it try to create it.
     * 
     * @see <a href= "http://www.codejava.net/java-se/networking/ftp/java-ftp-create-directory-example">codejava.net</a>
     * @since 1.0
     * @param ftpo
     *            connection data
     * @param client
     * @param ftpDir
     *            directory to check
     * @return true if exists.
     */
    public static boolean checkDirectory(UploadableFTPFile ftpo, FTPClient client, String ftpDir) {
        boolean exists = true;
        try (InputStream is = client.retrieveFileStream(ftpDir)) {
            int returnCode = client.getReplyCode();
            if (is == null || returnCode == FileConstants.FTP550) {
                exists = client.makeDirectory(ftpDir);
                showServerReply(ftpo, client);
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
                check = ByteConversor.getMinAvailableSize(files[NumericConstants.ZERO].getSize(), minSize);
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
            ftpFiles = Arrays.asList(getFilesFromDir(client, route)).stream().filter(ftpFile -> !ftpFile.isDirectory())
                    .collect(Collectors.toList());
            Comparator<FTPFile> ftpFileComparator = (FTPFile p1, FTPFile p2) -> p1.getTimestamp()
                    .compareTo(p2.getTimestamp());
            Collections.sort(ftpFiles, ftpFileComparator);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return ftpFiles;
    }

    /**
     * Transfer files through FTP using inputstream
     * 
     * @see <a href=
     *      "http://www.codejava.net/java-se/networking/ftp/java-ftp-file-upload-tutorial-and-example">codejava.net</a>
     * @param client
     * @param route
     * @param ftpRoute
     * @return
     */
    public static boolean transferFileIS(FTPClient client, String route, String ftpRoute) {
        try (InputStream is = new FileInputStream(new File(route))) {
            return client.storeFile(ftpRoute, is);
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    /**
     * Transfer files through FTP using outputstream and sends noop for avoid disconnection, this method should be used
     * for big files.
     * 
     * @see <a href=
     *      "http://www.codejava.net/java-se/networking/ftp/java-ftp-file-upload-tutorial-and-example">codejava.net</a>
     * @param ftpo
     * @param client
     * @param route
     * @param ftpRoute
     * @param transferSpeed
     * @param printStep
     * @return
     */
    public static boolean transferFileOS(UploadableFTPFile ftpo, FTPClient client, String route, String ftpRoute,
            int transferSpeed, int printStep) {
        try (InputStream is = new FileInputStream(new File(route));
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
            showServerReply(ftpo, client);
            return client.completePendingCommand();
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    /**
     * It allows to get/download a file from FTP.
     * 
     * @see <a href=
     *      "http://www.codejava.net/java-se/networking/ftp/java-ftp-file-download-tutorial-and-example">codejava.net</a>
     * @param client
     * @param route
     * @param ftpRoute
     * @return
     */
    public static boolean receiveFile(FTPClient client, String route, String ftpRoute) {
        boolean received = false;
        try (FileOutputStream fos = new FileOutputStream(new File(route));
                BufferedOutputStream bos = new BufferedOutputStream(fos); OutputStream os = bos) {
            received = client.retrieveFile(ftpRoute, os);
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
     * @see <a href= "http://www.codejava.net/java-se/networking/ftp/delete-a-file-on-a-ftp-server">codejava.net</a>
     * @param client
     * @param directory
     * @return
     */
    public static boolean deleteOldFiles(FTPClient client, String directory, int backup) {
        boolean deleted = false;
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
     * @param ftpo
     * @param cliente
     */
    private static void showServerReply(UploadableFTPFile ftpo, FTPClient client) {
        if (log.isInfoEnabled() && ftpo.isEnableReply()) {
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
            progress.append(ByteConversor.readableFileSize((long) sum));
            progress.append("\n");
            log.info("{}", progress);
        }
    }

}
