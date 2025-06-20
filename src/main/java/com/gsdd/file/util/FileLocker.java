package com.gsdd.file.util;

import com.gsdd.constants.FileConstants;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
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
    try (RandomAccessFile raf = new RandomAccessFile(file, FileConstants.FILE_PERMISSION);
        FileChannel channel = raf.getChannel();
        FileLock lock = channel.tryLock()) {
      if (lock == null) {
        closeLock(null, channel);
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
    IoUtils.closeQuietly(channel);
  }

  @AllArgsConstructor
  public static class ShutdownThread implements Runnable {

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
