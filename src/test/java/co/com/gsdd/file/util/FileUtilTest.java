package co.com.gsdd.file.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import co.com.gsdd.constants.NumericConstants;
import co.com.gsdd.exception.TechnicalException;

@ExtendWith(MockitoExtension.class)
public class FileUtilTest {

    private static final Long EIGHT_THOUSAND = 8000L;
    private static final String TEST_1 = "test_1_";
    private static final String TEST_ZIP = "test.zip";
    private static final String CYPHER = "comprimido";
    private static final String TXT = ".txt";

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void checkDirectoryTest(boolean createTempFile, @TempDir Path tempDir) {
        File f = tempDir.toFile();
        if (createTempFile) {
            f.mkdir();
        }
        Assertions.assertTrue(FileUtil.checkDirectory(f.getAbsolutePath()));
    }

    @Test
    public void checkDirectoryExcTest() {
        Assertions.assertThrows(TechnicalException.class, () -> FileUtil.checkDirectory(null));
    }

    @Test
    public void checkAvailableSpaceOnDirTest(@TempDir Path tempDir) {
        Assertions.assertTrue(FileUtil.checkAvailableSpaceOnDir(tempDir.toFile().getAbsolutePath(), EIGHT_THOUSAND));
    }

    @Test
    public void checkAvailableSpaceOnDirExcTest() {
        Assertions.assertThrows(TechnicalException.class,
                () -> FileUtil.checkAvailableSpaceOnDir(null, EIGHT_THOUSAND));
    }

    private void assertDirectoryContent(String filePath, int filesOnDirLength) {
        File[] filesOnDir = new File(filePath).listFiles();
        Assertions.assertNotNull(filesOnDir);
        Assertions.assertEquals(filesOnDirLength, filesOnDir.length);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void deleteEmptyFilesTest(boolean createTempFile, @TempDir Path tempDir) throws IOException {
        String filePath = getFilePath(createTempFile, tempDir);
        FileUtil.deleteEmptyFiles(filePath);
        assertDirectoryContent(filePath, 0);
    }

    @Test
    public void deleteEmptyFilesFolderTest(@TempDir Path tempDir) throws IOException {
        File folder = tempDir.resolve("test").toFile();
        folder.mkdir();
        String filePath = tempDir.toFile().getAbsolutePath();
        FileUtil.deleteEmptyFiles(filePath);
        assertDirectoryContent(filePath, 1);
    }

    @Test
    public void deleteEmptyFilesNotEmptyTest(@TempDir Path tempDir) throws IOException {
        File file = tempDir.toFile();
        File tmpFile = File.createTempFile(TEST_1, TXT, file);
        String filePath = file.getAbsolutePath();
        writeOnFile(tmpFile);
        FileUtil.deleteEmptyFiles(filePath);
        assertDirectoryContent(filePath, 1);
    }

    @Test
    public void deleteEmptyFilesExcTest() {
        Assertions.assertThrows(TechnicalException.class, () -> FileUtil.deleteEmptyFiles(null));
    }

    @Test
    public void deleteFileNotExistsTest(@Mock File file) {
        Mockito.doReturn(false).when(file).exists();
        Assertions.assertFalse(FileUtil.deleteFile(file));
    }

    @Test
    public void deleteFileNullTest() {
        Assertions.assertFalse(FileUtil.deleteFile(null));
    }

    @Test
    public void deleteFileExcFalseTest(@Mock File file) {
        Mockito.doReturn(true).when(file).exists();
        Mockito.doThrow(new RuntimeException()).when(file).toPath();
        Assertions.assertFalse(FileUtil.deleteFile(file));
    }

    @Test
    public void getLastModifiedFileExcTest() {
        Assertions.assertThrows(TechnicalException.class, () -> FileUtil.getLastModifiedFile(null));
    }

    @Test
    public void getLastModifiedFileNotEmptyTest(@TempDir Path tempDir) throws IOException {
        File resultFile = FileUtil.getLastModifiedFile(getFilePath(true, tempDir));
        Assertions.assertTrue(resultFile.getName().startsWith(TEST_1));
        Assertions.assertTrue(resultFile.getName().endsWith(TXT));
    }

    @Test
    public void getLastModifiedFileEmptyTest(@TempDir Path tempDir) {
        Assertions.assertNull(FileUtil.getLastModifiedFile(tempDir.toFile().getAbsolutePath()));
    }

    @Test
    public void deleteOldFilesExcTest() {
        Assertions.assertThrows(TechnicalException.class, () -> FileUtil.deleteOldFiles(null, 0));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void deleteOldFilesTest(boolean createTempFile, @TempDir Path tempDir) throws IOException {
        String filePath = getFilePath(createTempFile, tempDir);
        boolean deleted = FileUtil.deleteOldFiles(filePath, 0);
        assertDirectoryContent(filePath, 0);
        Assertions.assertTrue(createTempFile ? deleted : !deleted);
    }

    @Test
    public void deleteOldFilesWithBackTest(@TempDir Path tempDir) throws IOException {
        String filePath = getFilePath(true, tempDir);
        boolean deleted = FileUtil.deleteOldFiles(filePath, 1);
        assertDirectoryContent(filePath, 1);
        Assertions.assertFalse(deleted);
    }

    @Test
    public void zipFileEmptyTest(@TempDir Path tempDir) throws IOException {
        String filePath = getFilePath(true, tempDir);
        File[] filesOnDir = new File(filePath).listFiles();
        boolean zip = FileUtil.zipFile(filePath + File.separator + TEST_ZIP, filesOnDir[0].getAbsolutePath(), 4096);
        File f = new File(filePath + File.separator + TEST_ZIP);
        Assertions.assertTrue(f.exists());
        Assertions.assertTrue(f.isFile());
        Assertions.assertTrue(zip);
    }

    @Test
    public void zipFileUsingPassExcTest(@TempDir Path tempDir) throws IOException {
        String filePath = tempDir.toFile().getAbsolutePath() + File.separator + TEST_ZIP;
        Assertions.assertThrows(TechnicalException.class, () -> FileUtil.zipFileUsingPass(filePath, null, CYPHER));
    }

    @Test
    public void zipFileNotEmptyTest(@TempDir Path tempDir) throws IOException {
        String filePath = getFilePath(true, tempDir);
        File f = new File(filePath + File.separator + TEST_1 + TXT);
        writeOnFile(f);
        boolean zip = FileUtil.zipFile(filePath + File.separator + TEST_ZIP, filePath + File.separator + TEST_1 + TXT,
                4096);
        File fz = new File(filePath + File.separator + TEST_ZIP);
        Assertions.assertTrue(fz.exists());
        Assertions.assertTrue(fz.isFile());
        Assertions.assertTrue(zip);
    }

    @Test
    public void zipFileExcTest() {
        Assertions.assertThrows(TechnicalException.class, () -> FileUtil.zipFile(null, null, 4096));
    }

    @Test
    public void zipFileUsingPassTest(@TempDir Path tempDir) throws IOException {
        File file = tempDir.toFile();
        File.createTempFile(TEST_1, TXT, file);
        String filePath = file.getAbsolutePath();
        List<File> filesToAdd = new ArrayList<>();
        filesToAdd.add(new File(filePath));
        String path = filePath.substring(0, filePath.lastIndexOf(File.separatorChar)) + File.separator + TEST_ZIP;
        FileUtil.zipFileUsingPass(path, filesToAdd, CYPHER);
        File f = new File(path);
        Assertions.assertTrue(f.exists());
        Assertions.assertTrue(f.isFile());
    }

    private String getFilePath(boolean createTempFile, Path tempDir) throws IOException {
        File file = tempDir.toFile();
        if (createTempFile) {
            File.createTempFile(TEST_1, TXT, file);
        }
        return file.getAbsolutePath();
    }

    private void writeOnFile(File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));) {
            for (int i = 0; i < NumericConstants.TEN; i++) {
                bw.write(TEST_1 + TXT);
                bw.newLine();
            }
        }
    }
}
