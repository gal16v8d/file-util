package co.com.gsdd.file.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.apache.commons.io.IOUtils;

import co.com.gsdd.constants.FileConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
public class FileLocker {

    private final String appName;

    public boolean isAppActive() {
        boolean response = true;
        File file = new File(appName + FileConstants.FILE_EXT);
        try (FileChannel channel = new RandomAccessFile(file, FileConstants.FILE_PERMISSION).getChannel();
                FileLock lock = channel.tryLock()) {
            if (lock == null) {
                closeLock(lock, channel);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownThread(lock, channel, file)));
            response = false;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return response;
    }

    public static void closeLock(FileLock lock, FileChannel channel) {
        if (lock != null) {
            try {
                lock.release();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        IOUtils.closeQuietly(channel);
    }

    @AllArgsConstructor
    public class ShutdownThread implements Runnable {

        private FileLock lock;
        private FileChannel channel;
        private File file;

        @Override
        public void run() {
            closeLock(lock, channel);
            FileUtil.deleteFile(file);
        }
    }

}
