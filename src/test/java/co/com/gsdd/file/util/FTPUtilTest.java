package co.com.gsdd.file.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import co.com.gsdd.constants.FileConstants;
import co.com.gsdd.exception.TechnicalException;
import co.com.gsdd.file.util.model.UploadableFTPFile;

@ExtendWith(MockitoExtension.class)
class FTPUtilTest {

    @Mock
    private FTPClient ftpClient;
    private static final String DIR_TEST = "Test";
    private static final String[] REPLY_DATA = { "This", "is a", "test" };

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void disconnectFTPWhenNoConnectedTest() throws IOException {
        Mockito.doReturn(false).when(ftpClient).isConnected();
        FTPUtil.disconnect(ftpClient);
        Mockito.verify(ftpClient, Mockito.never()).logout();
        Mockito.verify(ftpClient, Mockito.never()).disconnect();
    }

    @Test
    public void disconnectFTPWhenConnectedTest() throws IOException {
        Mockito.doReturn(true).when(ftpClient).isConnected();
        Mockito.doReturn(true).when(ftpClient).logout();
        Mockito.doNothing().when(ftpClient).disconnect();
        FTPUtil.disconnect(ftpClient);
        Mockito.verify(ftpClient).logout();
        Mockito.verify(ftpClient).disconnect();
    }

    @Test
    public void disconnectFTPExcTest() throws IOException {
        Mockito.doReturn(true).when(ftpClient).isConnected();
        Mockito.doThrow(new IOException()).when(ftpClient).logout();
        Assertions.assertThrows(TechnicalException.class, () -> FTPUtil.disconnect(ftpClient));
    }

    @Test
    public void checkDirectoryExcTest() throws IOException {
        Mockito.doThrow(new IOException()).when(ftpClient).retrieveFileStream(DIR_TEST);
        Assertions.assertThrows(TechnicalException.class, () -> FTPUtil.checkDirectory(null, ftpClient, DIR_TEST));
    }

    @Test
    public void checkDirectoryISTest(@Mock InputStream is) throws IOException {
        Mockito.doNothing().when(is).close();
        Mockito.doReturn(is).when(ftpClient).retrieveFileStream(DIR_TEST);
        Assertions.assertTrue(FTPUtil.checkDirectory(null, ftpClient, DIR_TEST));
        Mockito.verify(is).close();
    }

    @Test
    public void checkDirectoryNoISTest() throws IOException {
        Mockito.doReturn(null).when(ftpClient).retrieveFileStream(DIR_TEST);
        Mockito.doReturn(FileConstants.FTP550).when(ftpClient).getReplyCode();
        Mockito.doReturn(true).when(ftpClient).makeDirectory(DIR_TEST);
        Assertions.assertTrue(FTPUtil.checkDirectory(getFTPFileInstance(false), ftpClient, DIR_TEST));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void checkDirectoryInputYReplyTest(boolean withReplyData, @Mock InputStream is) throws IOException {
        Mockito.doReturn(is).when(ftpClient).retrieveFileStream(DIR_TEST);
        Mockito.doReturn(FileConstants.FTP550).when(ftpClient).getReplyCode();
        if (withReplyData) {
            Logger rootLogger = (Logger) LogManager.getLogger(FTPUtil.class);
            rootLogger.setLevel(Level.INFO);
            Mockito.doReturn(REPLY_DATA).when(ftpClient).getReplyStrings();
        }
        Mockito.doReturn(true).when(ftpClient).makeDirectory(DIR_TEST);
        Assertions.assertTrue(FTPUtil.checkDirectory(getFTPFileInstance(withReplyData), ftpClient, DIR_TEST));
        Mockito.verify(is).close();
    }

    private UploadableFTPFile getFTPFileInstance(boolean reply) {
        UploadableFTPFile dto = new UploadableFTPFile();
        dto.setEnableReply(reply);
        return dto;
    }

    @Test
    public void deleteEmptyFilesTest() throws IOException {
        Mockito.doReturn(arrangeFTPFile(true)).when(ftpClient).listFiles(Mockito.anyString());
        Mockito.doReturn(true).when(ftpClient).deleteFile(Mockito.anyString());
        FTPUtil.deleteEmptyFiles(ftpClient, DIR_TEST);
        Mockito.verify(ftpClient).listFiles(Mockito.anyString());
        Mockito.verify(ftpClient).deleteFile(Mockito.anyString());
    }

    @Test
    public void deleteEmptyFilesWithEmptyListTest() throws IOException {
        Mockito.doReturn(arrangeFTPFile(false)).when(ftpClient).listFiles(Mockito.anyString());
        FTPUtil.deleteEmptyFiles(ftpClient, DIR_TEST);
        Mockito.verify(ftpClient).listFiles(Mockito.anyString());
        Mockito.verify(ftpClient, Mockito.never()).deleteFile(Mockito.anyString());
    }

    @Test
    public void deleteEmptyFilesNoZeroSizeListTest() throws IOException {
        List<FTPFile> ftpFiles = Stream.of(arrangeFTPFile(false)).filter(ftpFile -> ftpFile.getSize() > 0L)
                .collect(Collectors.toList());
        Mockito.doReturn(ftpFiles.toArray(new FTPFile[ftpFiles.size()])).when(ftpClient).listFiles(Mockito.anyString());
        FTPUtil.deleteEmptyFiles(ftpClient, DIR_TEST);
        Mockito.verify(ftpClient).listFiles(Mockito.anyString());
        Mockito.verify(ftpClient, Mockito.never()).deleteFile(Mockito.anyString());
    }

    @Test
    public void deleteOldFilesTest() throws IOException {
        Mockito.doReturn(arrangeFTPFile(true)).when(ftpClient).listFiles(Mockito.anyString());
        Mockito.doReturn(true).when(ftpClient).deleteFile(Mockito.anyString());
        FTPUtil.deleteOldFiles(ftpClient, DIR_TEST, 2);
        Mockito.verify(ftpClient).listFiles(Mockito.anyString());
        Mockito.verify(ftpClient, Mockito.times(2)).deleteFile(Mockito.anyString());
    }

    @Test
    public void deleteOldFilesWithEmptyListTest() throws IOException {
        Mockito.doReturn(arrangeFTPFile(false)).when(ftpClient).listFiles(Mockito.anyString());
        FTPUtil.deleteOldFiles(ftpClient, DIR_TEST, 0);
        Mockito.verify(ftpClient).listFiles(Mockito.anyString());
        Mockito.verify(ftpClient, Mockito.never()).deleteFile(Mockito.anyString());
    }

    private FTPFile[] arrangeFTPFile(boolean withElements) {
        List<FTPFile> ftpFiles = new ArrayList<>();
        if (withElements) {
            ftpFiles.add(createFTPFile("File 0.txt", 0L, FTPFile.FILE_TYPE));
            ftpFiles.add(createFTPFile("File 1.txt", 1L, FTPFile.FILE_TYPE));
            ftpFiles.add(createFTPFile("Folder", 0L, FTPFile.DIRECTORY_TYPE));
            ftpFiles.add(createFTPFile("File 2.txt", 2L, FTPFile.FILE_TYPE));
            ftpFiles.add(createFTPFile(".bash_profile", 1L, FTPFile.SYMBOLIC_LINK_TYPE));
        }
        return ftpFiles.toArray(new FTPFile[ftpFiles.size()]);
    }

    private FTPFile createFTPFile(String name, long size, int type) {
        FTPFile file = new FTPFile();
        file.setType(type);
        file.setName(name);
        file.setSize(size);
        file.setTimestamp(Calendar.getInstance());
        return file;
    }

}
