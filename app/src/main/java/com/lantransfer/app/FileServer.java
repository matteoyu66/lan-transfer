package com.lantransfer.app;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class FileServer {
    private final int port;
    private final Context context;
    private final MainActivity activity;
    private ServerSocket serverSocket;
    private boolean running = false;
    private ExecutorService executor = Executors.newCachedThreadPool();
    public FileServer(int port, MainActivity activity) {
        this.port = port;
        this.context = activity.getApplicationContext();
        this.activity = activity;
    }
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        while (running) {
            try {
                Socket client = serverSocket.accept();
                executor.execute(() -> handleClient(client));
            } catch (IOException e) {
                if (running) activity.appendLog("接收连接出错：" + e.getMessage());
            }
        }
    }
    private void handleClient(Socket client) {
        String clientIp = client.getInetAddress().getHostAddress();
        activity.appendLog("收到来自 " + clientIp + " 的连接");
        try (DataInputStream dis = new DataInputStream(client.getInputStream())) {
            int nameLen = dis.readInt();
            byte[] nameBytes = new byte[nameLen];
            dis.readFully(nameBytes);
            String fileName = new String(nameBytes, "UTF-8");
            long fileSize = dis.readLong();
            activity.appendLog("接收文件：" + fileName);
            OutputStream os = getOutputStream(fileName);
            if (os == null) { activity.appendLog("无法创建文件"); return; }
            byte[] buffer = new byte[8192];
            long received = 0;
            int read;
            while (received < fileSize && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - received))) != -1) {
                os.write(buffer, 0, read);
                received += read;
            }
            os.flush(); os.close();
            activity.appendLog("接收完成：" + fileName);
        } catch (Exception e) {
            activity.appendLog("接收出错：" + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }
    private OutputStream getOutputStream(String fileName) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/局域网传输");
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) return context.getContentResolver().openOutputStream(uri);
            return null;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "局域网传输");
            if (!dir.exists()) dir.mkdirs();
            return new FileOutputStream(new File(dir, fileName));
        }
    }
    public void stop() {
        running = false;
        try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        executor.shutdown();
    }
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        else return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
