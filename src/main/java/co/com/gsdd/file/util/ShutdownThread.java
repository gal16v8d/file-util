package co.com.gsdd.file.util;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ShutdownThread implements Runnable {

    private FileLock lock;
    private FileChannel channel;
    private File file;

    @Override
    public void run() {
        FileLocker.closeLock(lock, channel);
        FileUtil.deleteFile(file);
    }
}
