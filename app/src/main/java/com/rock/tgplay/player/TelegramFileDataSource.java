package com.rock.tgplay.player;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.media3.common.C;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;

import com.rock.tgplay.player.ChunkProgressView;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.io.IOException;
import java.io.RandomAccessFile;

@UnstableApi
public class TelegramFileDataSource extends BaseDataSource {

    private static int CHNUNK_SIZE = 1024 * 1024 * 5;
    private final Client client;
    private final int fileId;
    private final ChunkProgressView cpv;

    private byte[] buffer;
    private int bufferLength;
    private int readPosition;

    private long dataStartOffset;
    private long dataEndOffset; // exclusive
    private boolean opened = false;
    private long fileSize;

    public TelegramFileDataSource(Client client, int fileId, long fileSize, ChunkProgressView chunkProgressView) {
        super(true);
        this.client = client;
        this.fileId = fileId;
        this.fileSize = fileSize;
        this.cpv = chunkProgressView;
        chunkProgressView.setFileSize(fileSize);
        if(fileSize > 600 * 1024 * 1024){
            Toast.makeText(chunkProgressView.getContext(), "Requires IPV6 to play this video if not available then enable vpn", Toast.LENGTH_LONG).show();
            client.send(new TdApi.SetOption("prefer_ipv6", new TdApi.OptionValueBoolean(true)), object1 -> {
            });
        }
        calculateDynamicChunkSize();
    }

    private void calculateDynamicChunkSize() {
        CHNUNK_SIZE = (int) Math.max(CHNUNK_SIZE, fileSize / (50 * 1024 * 1024));
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        dataStartOffset = dataSpec.position;
        Log.e("TAG", "dataStartOffset: " + dataStartOffset);
        long requestedLength = (dataSpec.length == C.LENGTH_UNSET) ? getFileSize() - dataStartOffset : dataSpec.length;
        requestedLength = Math.min(requestedLength, CHNUNK_SIZE);
        Log.e("TAG", "requestedLength: " + requestedLength);
        // download extra 1MB (1*1024*1024 bytes) ahead
        long downloadLength = requestedLength;
        long fileSize = getFileSize();
        if (dataStartOffset + downloadLength > fileSize) {
            downloadLength = fileSize - dataStartOffset;
        }
        Log.e("TAG", "downloadLength: " + downloadLength);

        buffer = downloadData(dataStartOffset, downloadLength);
        if (buffer == null || buffer.length == 0) {
            throw new IOException("Failed to download Telegram file chunk");
        }

        bufferLength = buffer.length;
        readPosition = 0;
        dataEndOffset = dataStartOffset + bufferLength;

        opened = true;
        transferStarted(dataSpec);
        return bufferLength;
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        if (length == 0) return 0;
        if (readPosition >= bufferLength) return C.RESULT_END_OF_INPUT;

        int bytesToRead = Math.min(length, bufferLength - readPosition);
        System.arraycopy(buffer, readPosition, target, offset, bytesToRead);
        readPosition += bytesToRead;
        bytesTransferred(bytesToRead);
        return bytesToRead;
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public void close() throws IOException {
        if (opened) {
            opened = false;
            buffer = null;
        }
    }

    private long getFileSize() {
        return fileSize;
    }

    private byte[] downloadData(long offset, long length) throws IOException {

        Log.e("TAG", "downloading Partial Data: " + offset + " length  " + length);
        final byte[][] resultHolder = new byte[1][];
        final Object lock = new Object();
        if (cpv != null) cpv.downloadingChunk(offset, length);
        TdApi.DownloadFile downloadFile = new TdApi.DownloadFile(fileId, 32, offset, (int) length, true);
        client.send(downloadFile, object -> {
            if (object instanceof TdApi.File) {
                TdApi.File file = (TdApi.File) object;
                if (file.local.path != null) {
                    try {
                        byte[] data = readFileBytesRange(file.local.path, offset, (int) length);
                        if (cpv != null) {
                            cpv.downloadedChunk(offset, length);
                            cpv.clearDownloadingChunk();
                        }

                        synchronized (lock) {
                            resultHolder[0] = data;
                            lock.notify();
                        }
                    } catch (IOException e) {
                        synchronized (lock) {
                            resultHolder[0] = null;
                            lock.notify();
                        }
                    }
                } else {
                    synchronized (lock) {
                        resultHolder[0] = null;
                        lock.notify();
                    }
                }
            } else {
                synchronized (lock) {
                    resultHolder[0] = null;
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            try {
                lock.wait(30000); // wait max 30 seconds
            } catch (InterruptedException e) {
                throw new IOException("Interrupted while downloading", e);
            }
        }
        return resultHolder[0];
    }

    private byte[] readFileBytesRange(String path, long offset, int length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            byte[] buffer = new byte[length];
            raf.seek(offset);
            int bytesRead = raf.read(buffer, 0, length);
            if (bytesRead < length) {
                // If fewer bytes read, maybe end of file or incomplete download
                byte[] partial = new byte[bytesRead];
                System.arraycopy(buffer, 0, partial, 0, bytesRead);
                return partial;
            }
            return buffer;
        }
    }

    public static class Factory implements DataSource.Factory {
        private final Client client;
        private final int fileId;
        private final long fileSize;
        private ChunkProgressView chunkProgressView;

        public Factory(Client client, int fileId, long fileSize) {
            this.client = client;
            this.fileId = fileId;
            this.fileSize = fileSize;
        }

        @Override
        public DataSource createDataSource() {
            return new TelegramFileDataSource(client, fileId, fileSize, chunkProgressView);
        }

        public void setChunkProgressView(ChunkProgressView chunkProgressView) {
            this.chunkProgressView = chunkProgressView;
        }
    }
}
