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
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class HttpFileServer {
    private final int port;
    private final Context context;
    private final MainActivity activity;
    private ServerSocket serverSocket;
    private boolean running = false;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private final List<SharedFile> sharedFiles = new ArrayList<>();
    public static class SharedFile {
        public String name;
        public Uri uri;
        public long size;
        public String id;
        public SharedFile(String name, Uri uri, long size) {
            this.name = name; this.uri = uri; this.size = size;
            this.id = UUID.randomUUID().toString().replace("-","").substring(0,8);
        }
    }
    public HttpFileServer(int port, MainActivity activity) {
        this.port = port;
        this.context = activity.getApplicationContext();
        this.activity = activity;
    }
    public void addSharedFile(SharedFile f) { synchronized(sharedFiles){ sharedFiles.add(f); } }
    public void clearSharedFiles() { synchronized(sharedFiles){ sharedFiles.clear(); } }
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        while (running) {
            try {
                Socket client = serverSocket.accept();
                executor.execute(() -> handleRequest(client));
            } catch (IOException e) {
                if (running) activity.appendLog("HTTP出错：" + e.getMessage());
            }
        }
    }
    public void stop() {
        running = false;
        try { if (serverSocket!=null && !serverSocket.isClosed()) serverSocket.close(); } catch (IOException e) {}
        executor.shutdown();
    }
    private void handleRequest(Socket client) {
        try {
            InputStream is = client.getInputStream();
            OutputStream os = client.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            String requestLine = reader.readLine();
            if (requestLine==null||requestLine.isEmpty()){client.close();return;}
            String[] parts = requestLine.split(" ");
            if (parts.length<2){client.close();return;}
            String method=parts[0], path=parts[1];
            String line; int contentLength=0; String contentType="";
            while ((line=reader.readLine())!=null && !line.isEmpty()) {
                int colon=line.indexOf(':');
                if (colon>0) {
                    String key=line.substring(0,colon).trim().toLowerCase();
                    String value=line.substring(colon+1).trim();
                    if (key.equals("content-length")) contentLength=Integer.parseInt(value);
                    if (key.equals("content-type")) contentType=value;
                }
            }
            String clientIp=client.getInetAddress().getHostAddress();
            if (method.equals("GET")&&path.equals("/")) serveHomePage(os);
            else if (method.equals("GET")&&path.startsWith("/download/")) serveFile(os,path.substring("/download/".length()),clientIp);
            else if (method.equals("POST")&&path.equals("/upload")) handleUpload(is,os,contentType,contentLength,clientIp);
            else send404(os);
            os.flush(); client.close();
        } catch (Exception e) {
            try{client.close();}catch(IOException ignored){}
        }
    }
    private void serveHomePage(OutputStream os) throws IOException {
        StringBuilder fl = new StringBuilder();
        synchronized(sharedFiles) {
            if (sharedFiles.isEmpty()) fl.append("<p style='color:#888;text-align:center'>暂无共享文件</p>");
            else for (SharedFile f : sharedFiles)
                fl.append("<div class='fi'><span class='fn'>").append(f.name).append("</span>")
                  .append("<span class='fs'>").append(formatSize(f.size)).append("</span>")
                  .append("<a href='/download/").append(f.id).append("' class='bd'>下载</a></div>");
        }
        String html="<!DOCTYPE html><html lang='zh-CN'><head><meta charset='UTF-8'>"
            +"<meta name='viewport' content='width=device-width,initial-scale=1'>"
            +"<title>局域网传输</title><style>"
            +"*{box-sizing:border-box;margin:0;padding:0}"
            +"body{font-family:-apple-system,sans-serif;background:#f0f2f5}"
            +".box{max-width:600px;margin:0 auto;padding:16px}"
            +"h1{color:#1a237e;font-size:20px;text-align:center;margin:16px 0}"
         
rm -f app/src/main/java/com/lantransfer/app/HttpFileServer.java
cat > app/src/main/java/com/lantransfer/app/HttpFileServer.java << 'EOF'
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
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class HttpFileServer {
    private final int port;
    private final Context context;
    private final MainActivity activity;
    private ServerSocket serverSocket;
    private boolean running = false;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private final List<SharedFile> sharedFiles = new ArrayList<>();
    public static class SharedFile {
        public String name; public Uri uri;
        public long size; public String id;
        public SharedFile(String name, Uri uri, long size) {
            this.name=name; this.uri=uri; this.size=size;
            this.id=UUID.randomUUID().toString().replace("-","").substring(0,8);
        }
    }
    public HttpFileServer(int port, MainActivity activity) {
        this.port=port;
        this.context=activity.getApplicationContext();
        this.activity=activity;
    }
    public void addSharedFile(SharedFile f){synchronized(sharedFiles){sharedFiles.add(f);}}
    public void clearSharedFiles(){synchronized(sharedFiles){sharedFiles.clear();}}
    public void start() throws IOException {
        serverSocket=new ServerSocket(port); running=true;
        while(running){
            try{Socket c=serverSocket.accept();executor.execute(()->handleRequest(c));}
            catch(IOException e){if(running)activity.appendLog("HTTP出错:"+e.getMessage());}
        }
    }
    public void stop(){
        running=false;
        try{if(serverSocket!=null&&!serverSocket.isClosed())serverSocket.close();}catch(IOException e){}
        executor.shutdown();
    }
    private void handleRequest(Socket client){
        try{
            InputStream is=client.getInputStream();
            OutputStream os=client.getOutputStream();
            BufferedReader r=new BufferedReader(new InputStreamReader(is,"UTF-8"));
            String rl=r.readLine();
            if(rl==null||rl.isEmpty()){client.close();return;}
            String[] p=rl.split(" ");
            if(p.length<2){client.close();return;}
            String method=p[0],path=p[1];
            String line; int cl=0; String ct="";
            while((line=r.readLine())!=null&&!line.isEmpty()){
                int c=line.indexOf(':');
                if(c>0){
                    String k=line.substring(0,c).trim().toLowerCase();
                    String v=line.substring(c+1).trim();
                    if(k.equals("content-length"))cl=Integer.parseInt(v);
                    if(k.equals("content-type"))ct=v;
                }
            }
            String ip=client.getInetAddress().getHostAddress();
            if(method.equals("GET")&&path.equals("/"))serveHomePage(os);
            else if(method.equals("GET")&&path.startsWith("/download/"))serveFile(os,path.substring(10),ip);
            else if(method.equals("POST")&&path.equals("/upload"))handleUpload(is,os,ct,cl,ip);
            else send404(os);
            os.flush(); client.close();
        }catch(Exception e){try{client.close();}catch(IOException ignored){}}
    }
    private void serveHomePage(OutputStream os) throws IOException {
        StringBuilder fl=new StringBuilder();
        synchronized(sharedFiles){
            if(sharedFiles.isEmpty())fl.append("<p style='color:#888'>暂无共享文件</p>");
            else for(SharedFile f:sharedFiles)
                fl.append("<div class='fi'><span class='fn'>").append(f.name)
                  .append("</span><span class='fs'>").append(formatSize(f.size))
                  .append("</span><a href='/download/").append(f.id)
                  .append("' class='bd'>下载</a></div>");
        }
        String html="<!DOCTYPE html><html lang='zh-CN'><head><meta charset='UTF-8'>"
            +"<meta name='viewport' content='width=device-width,initial-scale=1'>"
            +"<title>局域网传输</title><style>"
            +"*{box-sizing:border-box;margin:0;padding:0}"
            +"body{font-family:-apple-system,sans-serif;background:#f0f2f5}"
            +".box{max-width:600px;margin:0 auto;padding:16px}"
            +"h1{color:#1a237e;font-size:20px;text-align:center;margin:16px 0}"
            +".card{background:#fff;border-radius:12px;padding:16px;margin-bottom:16px}"
            +".card h2{font-size:15px;color:#283593;margin-bottom:10px}"
            +".up{border:2px dashed #7986cb;border-radius:8px;padding:20px;text-align:center}"
            +".btn{width:100%;padding:10px;border-radius:8px;background:#3f51b5;color:#fff;border:none;margin-top:8px;font-size:14px}"
            +".bd{background:#009688;padding:5px 12px;font-size:12px;border-radius:6px;color:#fff;text-decoration:none}"
            +".fi{display:flex;align-items:center;padding:8px 0;border-bottom:1px solid #eee;gap:8px}"
            +".fn{flex:1;font-size:13px;word-break:break-all}.fs{font-size:11px;color:#888}"
            +"#pg{display:none;margin-top:10px}progress{width:100%;height:8px}"
            +"#st{text-align:center;font-size:13px;margin-top:6px}"
            +"</style></head><body><div class='box'>"
            +"<h1>局域网文件传输</h1>"
            +"<div class='card'><h2>上传文件到Android</h2>"
            +"<div class='up'><input type='file' id='fi' multiple></div>"
            +"<button class='btn' onclick='upload()'>开始上传</button>"
            +"<div id='pg'><progress id='bar' value='0' max='100'></progress>"
            +"<p id='st'>上传中...</p></div></div>"
            +"<div class='card'><h2>下载Android共享的文件</h2>"+fl+"</div></div>"
            +"<script>function upload(){"
            +"var f=document.getElementById('fi').files;"
            +"if(!f.length){alert('请先选择文件');return;}"
            +"var fd=new FormData();"
            +"for(var i=0;i<f.length;i++)fd.append('file',f[i]);"
            +"var x=new XMLHttpRequest();x.open('POST','/upload');"
            +"x.upload.onprogress=function(e){if(e.lengthComputable){"
            +"document.getElementById('bar').value=Math.round(e.loaded/e.total*100);"
            +"document.getElementById('st').textContent='上传中...'+Math.round(e.loaded/e.total*100)+'%';}};"
            +"x.onload=function(){document.getElementById('st').textContent=x.status==200?'上传成功！':'上传失败';};"
            +"document.getElementById('pg').style.display='block';x.send(fd);}"
            +"</script></body></html>";
        byte[] body=html.getBytes("UTF-8");
        os.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: "+body.length+"\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
        os.write(body);
    }
    private void serveFile(OutputStream os, String fileId, String ip) throws IOException {
        SharedFile target=null;
        synchronized(sharedFiles){for(SharedFile f:sharedFiles)if(f.id.equals(fileId)){target=f;break;}}
        if(target==null){send404(os);return;}
        final SharedFile file=target;
        activity.appendLog("下载："+file.name+" 来自 "+ip);
        try(InputStream is=context.getContentResolver().openInputStream(file.uri)){
            String enc=URLEncoder.encode(file.name,"UTF-8").replace("+"," ");
            os.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename*=UTF-8''"+enc+"\r\nContent-Length: "+file.size+"\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
            byte[] buf=new byte[8192]; int read;
            while((read=is.read(buf))!=-1)os.write(buf,0,read);
            activity.appendLog("下载完成："+file.name);
        }catch(Exception e){activity.appendLog("下载出错："+e.getMessage());}
    }
    private void handleUpload(InputStream is, OutputStream os, String ct, int cl, String ip) throws IOException {
        if(!ct.contains("multipart/form-data")){sendResp(os,400,"Bad Request");return;}
        String boundary="";
        for(String s:ct.split(";")){s=s.trim();if(s.startsWith("boundary=")){boundary="--"+s.substring(9).trim();break;}}
        if(boundary.isEmpty()){sendResp(os,400,"No boundary");return;}
        byte[] data=new byte[cl]; int total=0;
        while(total<cl){int n=is.read(data,total,cl-total);if(n==-1)break;total+=n;}
        byte[] bb=boundary.getBytes("UTF-8");
        List<Integer> pos=findAll(data,bb);
        int saved=0;
        for(int i=0;i<pos.size()-1;i++){
            int start=pos.get(i)+bb.length+2;
            int end=pos.get(i+1)-2;
            byte[] sep="\r\n\r\n".getBytes("UTF-8");
            int sp=indexOf(data,sep,start,end);
            if(sp==-1)continue;
            String hdrs=new String(data,start,sp-start,"UTF-8");
            int bs=sp+4; int bl=end-bs;
            if(bl<=0)continue;
            String fn=null;
            for(String h:hdrs.split("\r\n"))
                if(h.toLowerCase().contains("content-disposition")&&h.contains("filename")){
                    int fi=h.indexOf("filename=\"");
                    if(fi>=0){int fe=h.indexOf("\"",fi+10);if(fe>fi+10)fn=h.substring(fi+10,fe);}
                }
            if(fn==null||fn.isEmpty())fn="file_"+System.currentTimeMillis();
            saveFile(fn,data,bs,bl); saved++;
            activity.appendLog("收到："+fn+" 来自 "+ip);
        }
        sendResp(os,saved>0?200:400,saved>0?"OK":"No files");
        if(saved>0)activity.appendLog("接收完成 "+saved+" 个文件");
    }
    private void saveFile(String name, byte[] data, int off, int len) throws IOException {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            ContentValues v=new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME,name);
            v.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/局域网传输");
            Uri uri=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
            if(uri!=null)try(OutputStream o=context.getContentResolver().openOutputStream(uri)){o.write(data,off,len);}
        }else{
            File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"局域网传输");
            if(!dir.exists())dir.mkdirs();
            try(FileOutputStream o=new FileOutputStream(new File(dir,name))){o.write(data,off,len);}
        }
    }
    private void sendResp(OutputStream os, int code, String body) throws IOException {
        byte[] b=body.getBytes("UTF-8");
        String s=code==200?"200 OK":"400 Bad Request";
        os.write(("HTTP/1.1 "+s+"\r\nContent-Type: text/plain\r\nContent-Length: "+b.length+"\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
        os.write(b);
    }
    private void send404(OutputStream os) throws IOException{sendResp(os,404,"Not Found");}
    private List<Integer> findAll(byte[] data,byte[] pat){
        List<Integer> r=new ArrayList<>();
        for(int i=0;i<=data.length-pat.length;i++){
            boolean m=true;
            for(int j=0;j<pat.length;j++)if(data[i+j]!=pat[j]){m=false;break;}
            if(m)r.add(i);
        }
        return r;
    }
    private int indexOf(byte[] data,byte[] pat,int from,int to){
        for(int i=from;i<=to-pat.length;i++){
            boolean m=true;
            for(int j=0;j<pat.length;j++)if(data[i+j]!=pat[j]){m=false;break;}
            if(m)return i;
        }
        return -1;
    }
    private String formatSize(long b){
        if(b<1024)return b+" B";
        else if(b<1048576)return String.format("%.1f KB",b/1024.0);
        else return String.format("%.1f MB",b/1048576.0);
    }
}
