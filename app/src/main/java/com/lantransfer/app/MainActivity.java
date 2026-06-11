package com.lantransfer.app;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE=1,PICK_SHARE=2,PERM_REQ=3;
    private static final int TCP_PORT=9527,HTTP_PORT=9528;
    private TextView tvIp,tvStatus,tvLog,tvUrl;
    private EditText etIp;
    private Button btnStartTcp,btnStopTcp,btnStartHttp,btnStopHttp,btnSelect,btnSend,btnShare;
    private ProgressBar progress;
    private ScrollView scroll;
    private FileServer tcpServer;
    private HttpFileServer httpServer;
    private boolean tcpOn=false,httpOn=false;
    private Uri fileUri; private String fileName; private long fileSize;
    private ExecutorService exec=Executors.newCachedThreadPool();
    @Override
    protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        tvIp=findViewById(R.id.tv_my_ip);
        tvStatus=findViewById(R.id.tv_status);
        tvLog=findViewById(R.id.tv_log);
        tvUrl=findViewById(R.id.tv_http_url);
        etIp=findViewById(R.id.et_target_ip);
        btnStartTcp=findViewById(R.id.btn_start_server);
        btnStopTcp=findViewById(R.id.btn_stop_server);
        btnStartHttp=findViewById(R.id.btn_start_http);
        btnStopHttp=findViewById(R.id.btn_stop_http);
        btnSelect=findViewById(R.id.btn_select_file);
        btnSend=findViewById(R.id.btn_send);
        btnShare=findViewById(R.id.btn_share_file);
        progress=findViewById(R.id.progress_bar);
        scroll=findViewById(R.id.scroll_view);
        tvIp.setText("本机IP："+getIp());
        btnStartTcp.setOnClickListener(v->startTcp());
        btnStopTcp.setOnClickListener(v->stopTcp());
        btnStartHttp.setOnClickListener(v->startHttp());
        btnStopHttp.setOnClickListener(v->stopHttp());
        btnSelect.setOnClickListener(v->pickFile(PICK_FILE));
        btnSend.setOnClickListener(v->sendFile());
        btnShare.setOnClickListener(v->pickFile(PICK_SHARE));
        btnStopTcp.setEnabled(false);
        btnStopHttp.setEnabled(false);
        btnSend.setEnabled(false);
        reqPerms();
    }
    private void startTcp(){
        if(tcpOn)return;
        tcpServer=new FileServer(TCP_PORT,this);
        exec.execute(()->{try{tcpServer.start();}catch(Exception e){runOnUiThread(()->appendLog("TCP失败:"+e.getMessage()));}});
        tcpOn=true;
        tvStatus.setText("状态：接收已开启(端口"+TCP_PORT+")");
        btnStartTcp.setEnabled(false);btnStopTcp.setEnabled(true);
        appendLog("Android传输服务已开启");
    }
    private void stopTcp(){
        if(tcpServer!=null)tcpServer.stop();
        tcpOn=false;
        tvStatus.setText("状态：空闲");
        btnStartTcp.setEnabled(true);btnStopTcp.setEnabled(false);
        appendLog("Android传输服务已停止");
    }
    private void startHttp(){
        if(httpOn)return;
        httpServer=new HttpFileServer(HTTP_PORT,this);
        exec.execute(()->{try{httpServer.start();}catch(Exception e){runOnUiThread(()->appendLog("HTTP失败:"+e.getMessage()));}});
        httpOn=true;
        String url="http://"+getIp()+":"+HTTP_PORT;
        tvUrl.setText("iOS访问："+url);
        tvUrl.setVisibility(View.VISIBLE);
        btnStartHttp.setEnabled(false);btnStopHttp.setEnabled(true);
        appendLog("网页服务已开启："+url);
    }
    private void stopHttp(){
        if(httpServer!=null){httpServer.stop();httpServer=null;}
        httpOn=false;
        tvUrl.setVisibility(View.GONE);
        btnStartHttp.setEnabled(true);btnStopHttp.setEnabled(false);
        appendLog("网页服务已停止");
    }
    private void pickFile(int req){
        if(req==PICK_SHARE&&!httpOn){Toast.makeText(this,"请先开启网页服务",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i,"选择文件"),req);
    }
    private void sendFile(){
        String ip=etIp.getText().toString().trim();
        if(ip.isEmpty()){Toast.makeText(this,"请输入对方IP",Toast.LENGTH_SHORT).show();return;}
        if(fileUri==null){Toast.makeText(this,"请先选择文件",Toast.LENGTH_SHORT).show();return;}
        btnSend.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        appendLog("发送："+fileName+" -> "+ip);
        exec.execute(()->{
            try(InputStream is=getContentResolver().openInputStream(fileUri);
                Socket sock=new Socket(ip,TCP_PORT);
                DataOutputStream dos=new DataOutputStream(sock.getOutputStream())){
                byte[] nb=fileName.getBytes("UTF-8");
                dos.writeInt(nb.length);dos.write(nb);dos.writeLong(fileSize);
                byte[] buf=new byte[8192]; long sent=0; int read;
                while((read=is.read(buf))!=-1){
                    dos.write(buf,0,read);sent+=read;
                    final int pct=(int)(sent*100/fileSize);
                    runOnUiThread(()->progress.setProgress(pct));
                }
                dos.flush();
                runOnUiThread(()->{
                    progress.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    appendLog("发送完成："+fileName);
                    Toast.makeText(this,"发送成功！",Toast.LENGTH_SHORT).show();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    progress.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    appendLog("发送失败："+e.getMessage());
                });
            }
        });
    }
    @Override
    protected void onActivityResult(int req, int res, Intent data){
        super.onActivityResult(req,res,data);
        if(res!=RESULT_OK||data==null)return;
        Uri uri=data.getData();
        if(req==PICK_FILE){
            fileUri=uri;fileName=getName(uri);fileSize=getSize(uri);
            btnSend.setEnabled(true);
            appendLog("已选择："+fileName+"("+fmtSize(fileSize)+")");
        }else if(req==PICK_SHARE){
            String n=getName(uri);long sz=getSize(uri);
            if(httpServer!=null){
                httpServer.addSharedFile(new HttpFileServer.SharedFile(n,uri,sz));
                appendLog("已共享："+n+"(iOS可下载)");
                Toast.makeText(this,"已共享，iOS刷新网页下载",Toast.LENGTH_LONG).show();
            }
        }
    }
    public void appendLog(String msg){
        runOnUiThread(()->{
            tvLog.append(msg+"\n");
            scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
        });
    }
    private String getIp(){
        try{
            WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wi=wm.getConnectionInfo();int ip=wi.getIpAddress();
            if(ip!=0)return String.format("%d.%d.%d.%d",ip&0xff,ip>>8&0xff,ip>>16&0xff,ip>>24&0xff);
            for(NetworkInterface ni:java.util.Collections.list(NetworkInterface.getNetworkInterfaces()))
                for(InetAddress a:java.util.Collections.list(ni.getInetAddresses()))
                    if(!a.isLoopbackAddress()&&a instanceof Inet4Address)return a.getHostAddress();
        }catch(Exception e){e.printStackTrace();}
        return "未知";
    }
    private String getName(Uri uri){
        String n="文件";
        try(Cursor c=getContentResolver().query(uri,null,null,null,null)){
            if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}
        }catch(Exception e){}
        return n;
    }
    private long getSize(Uri uri){
        long s=0;
        try(Cursor c=getContentResolver().query(uri,null,null,null,null)){
            if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.SIZE);if(i>=0)s=c.getLong(i);}
        }catch(Exception e){}
        return s;
    }
    private String fmtSize(long b){
        if(b<1024)return b+"B";
        else if(b<1048576)return String.format("%.1fKB",b/1024.0);
        else return String.format("%.1fMB",b/1048576.0);
    }
    private void reqPerms(){
        List<String> p=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            for(String x:new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_AUDIO})
                if(ContextCompat.checkSelfPermission(this,x)!=PackageManager.PERMISSION_GRANTED)p.add(x);
        }else{
            for(String x:new String[]{Manifest.pe
cat >> app/src/main/java/com/lantransfer/app/MainActivity.java << 'EOF'
    private void pickFile(int req){
        if(req==PICK_SHARE&&!httpOn){Toast.makeText(this,"请先开启网页服务",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i,"选择文件"),req);
    }
    private void sendFile(){
        String ip=etIp.getText().toString().trim();
        if(ip.isEmpty()){Toast.makeText(this,"请输入对方IP",Toast.LENGTH_SHORT).show();return;}
        if(fileUri==null){Toast.makeText(this,"请先选择文件",Toast.LENGTH_SHORT).show();return;}
        btnSend.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        appendLog("发送："+fileName+" -> "+ip);
        exec.execute(()->{
            try(InputStream is=getContentResolver().openInputStream(fileUri);
                Socket sock=new Socket(ip,TCP_PORT);
                DataOutputStream dos=new DataOutputStream(sock.getOutputStream())){
                byte[] nb=fileName.getBytes("UTF-8");
                dos.writeInt(nb.length);dos.write(nb);dos.writeLong(fileSize);
                byte[] buf=new byte[8192];long sent=0;int read;
                while((read=is.read(buf))!=-1){
                    dos.write(buf,0,read);sent+=read;
                    final int pct=(int)(sent*100/fileSize);
                    runOnUiThread(()->progress.setProgress(pct));
                }
                dos.flush();
                runOnUiThread(()->{
                    progress.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    appendLog("发送完成："+fileName);
                    Toast.makeText(this,"发送成功！",Toast.LENGTH_SHORT).show();
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    progress.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    appendLog("发送失败："+e.getMessage());
                });
            }
        });
    }
    @Override
    protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(res!=RESULT_OK||data==null)return;
        Uri uri=data.getData();
        if(req==PICK_FILE){
            fileUri=uri;fileName=getName(uri);fileSize=getSize(uri);
            btnSend.setEnabled(true);
            appendLog("已选择："+fileName+"("+fmtSize(fileSize)+")");
        }else if(req==PICK_SHARE){
            String n=getName(uri);long sz=getSize(uri);
            if(httpServer!=null){
                httpServer.addSharedFile(new HttpFileServer.SharedFile(n,uri,sz));
                appendLog("已共享："+n+"(iOS可下载)");
                Toast.makeText(this,"已共享，iOS刷新网页下载",Toast.LENGTH_LONG).show();
            }
        }
    }
    public void appendLog(String msg){
        runOnUiThread(()->{
            tvLog.append(msg+"\n");
            scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
        });
    }
    private String getIp(){
        try{
            WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
            int ip=wm.getConnectionInfo().getIpAddress();
            if(ip!=0)return String.format("%d.%d.%d.%d",ip&0xff,ip>>8&0xff,ip>>16&0xff,ip>>24&0xff);
            for(NetworkInterface ni:java.util.Collections.list(NetworkInterface.getNetworkInterfaces()))
                for(InetAddress a:java.util.Collections.list(ni.getInetAddresses()))
                    if(!a.isLoopbackAddress()&&a instanceof Inet4Address)return a.getHostAddress();
        }catch(Exception e){}
        return "未知";
    }
    private String getName(Uri uri){
        String n="文件";
        try(Cursor c=getContentResolver().query(uri,null,null,null,null)){
            if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}
        }catch(Exception e){}
        return n;
    }
    private long getSize(Uri uri){
        long s=0;
        try(Cursor c=getContentResolver().query(uri,null,null,null,null)){
            if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.SIZE);if(i>=0)s=c.getLong(i);}
        }catch(Exception e){}
        return s;
    }
    private String fmtSize(long b){
        if(b<1024)return b+"B";
        else if(b<1048576)return String.format("%.1fKB",b/1024.0);
        else return String.format("%.1fMB",b/1048576.0);
    }
    private void reqPerms(){
        List<String> p=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            for(String x:new String[]{Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_AUDIO})
                if(ContextCompat.checkSelfPermission(this,x)!=PackageManager.PERMISSION_GRANTED)p.add(x);
        }else{
            for(String x:new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE})
                if(ContextCompat.checkSelfPermission(this,x)!=PackageManager.PERMISSION_GRANTED)p.add(x);
        }
        if(!p.isEmpty())ActivityCompat.requestPermissions(this,p.toArray(new String[0]),PERM_REQ);
    }
    @Override
    protected void onDestroy(){super.onDestroy();stopTcp();stopHttp();exec.shutdown();}
}
