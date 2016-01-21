/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package monitoring;

/**
 *
 * @author cemakpolat
 */
/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate the file transfer from remote to local
 *   $ CLASSPATH=.:../build javac ScpFrom.java
 *   $ CLASSPATH=.:../build java ScpFrom user@remotehost:file1 file2
 * You will be asked passwd. 
 * If everything works fine, a file 'file1' on 'remotehost' will copied to
 * local 'file1'.
 *
 */


import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScpFrom{

    public static String hostPassword = "secretPassword";
    
    public static String file_authenticated = "/root/Authenticated.txt";
    public  String file_authenticated_save = "Authenticated";
    
    public static String file_dhcp = "/var/run/dnsmasq.leases";
    public  String file_dhcp_save = "dhcp";
    
    public  String file_serviceUsers = "/root/serviceUsers.txt"; 
    public  String file_serviceUsers_save = "serviceUsers";
    
    public  String file_dhcpVPNIP ="/root/dhcpVPNIP.txt";
    public  String file_dhcpVPNIP_save =  "dhcpVPNIP";
    
    public  String user = "root";
    public  String host = "";

    public void setFileNames(String append){
        file_authenticated_save=file_authenticated_save+"_"+append;
        file_dhcp_save=file_dhcp_save+"_"+append;
    }
    public void setHostIP(String ip) {
        this.host = ip;
    }
 


    public void cleanFileContent(String file){
        File f = new File(file);
        if(f.exists() && !f.isDirectory()){
            PrintWriter writer;
            try {
                writer = new PrintWriter(file);
                writer.print("");
                writer.close();
            } catch (FileNotFoundException ex) {
                java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
     
    }

  
  public  void fetchDHCPFile(){
     
    cleanFileContent(this.file_dhcp_save); //clean file
    FileOutputStream fos=null;
    try{

      String rfile=file_dhcp;
      String lfile=file_dhcp_save;

      String prefix=null;
      if(new File(lfile).isDirectory()){
        prefix=lfile+File.separator;
      }

      JSch jsch=new JSch();
      Session session=jsch.getSession(user, host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      // exec 'scp -f rfile' remotely
      String command="scp -f "+rfile;
      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out=channel.getOutputStream();
      InputStream in=channel.getInputStream();

      channel.connect();

      byte[] buf=new byte[1024];

      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();

      while(true){
	int c=checkAck(in);
        if(c!='C'){
	  break;
	}

        // read '0644 '
        in.read(buf, 0, 5);

        long filesize=0L;
        while(true){
          if(in.read(buf, 0, 1)<0){
            // error
            break; 
          }
          if(buf[0]==' ')break;
          filesize=filesize*10L+(long)(buf[0]-'0');
        }

        String file=null;
        for(int i=0;;i++){
          in.read(buf, i, 1);
          if(buf[i]==(byte)0x0a){
            file=new String(buf, 0, i);
            break;
  	  }
        }

	//System.out.println("filesize="+filesize+", file="+file);

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();

        // read a content of lfile
        fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
        int foo;
        while(true){
          if(buf.length<filesize) foo=buf.length;
	  else foo=(int)filesize;
          foo=in.read(buf, 0, foo);
          if(foo<0){
            // error 
            break;
          }
          fos.write(buf, 0, foo);
          filesize-=foo;
          if(filesize==0L) break;
        }
        fos.close();
        fos=null;

	if(checkAck(in)!=0){
	  System.exit(0);
	}

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();
      }

      session.disconnect();

     // System.exit(0);
    }
    catch(Exception e){
      System.out.println(e+" fetchDHCPFile");
      try{if(fos!=null)fos.close();}catch(Exception ee){}
    }
  }

  
   
  public void fetchAuthenticatedUserFile(){
    
    cleanFileContent(this.file_authenticated_save); //clean file
    FileOutputStream fos=null;
    try{


      String rfile=file_authenticated;//"/root/Authenticated.txt";
      String lfile=file_authenticated_save;

      String prefix=null;
      if(new File(lfile).isDirectory()){
        prefix=lfile+File.separator;
      }

      JSch jsch=new JSch();
      Session session=jsch.getSession(user, host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      // exec 'scp -f rfile' remotely
      String command="scp -f "+rfile;
      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out=channel.getOutputStream();
      InputStream in=channel.getInputStream();

      channel.connect();

      byte[] buf=new byte[1024];

      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();

      while(true){
	int c=checkAck(in);
        if(c!='C'){
	  break;
	}

        // read '0644 '
        in.read(buf, 0, 5);

        long filesize=0L;
        while(true){
          if(in.read(buf, 0, 1)<0){
            // error
            break; 
          }
          if(buf[0]==' ')break;
          filesize=filesize*10L+(long)(buf[0]-'0');
        }

        String file=null;
        for(int i=0;;i++){
          in.read(buf, i, 1);
          if(buf[i]==(byte)0x0a){
            file=new String(buf, 0, i);
            break;
  	  }
        }

	//System.out.println("filesize="+filesize+", file="+file);

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();

        // read a content of lfile
        fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
        int foo;
        while(true){
          if(buf.length<filesize) foo=buf.length;
	  else foo=(int)filesize;
          foo=in.read(buf, 0, foo);
          if(foo<0){
            // error 
            break;
          }
          fos.write(buf, 0, foo);
          filesize-=foo;
          if(filesize==0L) break;
        }
        fos.close();
        fos=null;

	if(checkAck(in)!=0){
	  System.exit(0);
	}

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();
      }

      session.disconnect();

     // System.exit(0);
    }
    catch(Exception e){
      System.out.println(e+" fetchAuthenticatedUserFile");
      try{if(fos!=null)fos.close();}catch(Exception ee){}
    }
  }

  
   public void fetchDHCPVPNIPFile(){
    
    
    cleanFileContent(this.file_dhcpVPNIP_save); //clean file
    FileOutputStream fos=null;
    try{


      String rfile=file_dhcpVPNIP;//"/root/Authenticated.txt";
      String lfile=file_dhcpVPNIP_save;

      String prefix=null;
      if(new File(lfile).isDirectory()){
        prefix=lfile+File.separator;
      }

      JSch jsch=new JSch();
      Session session=jsch.getSession(user, host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      // exec 'scp -f rfile' remotely
      String command="scp -f "+rfile;
      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out=channel.getOutputStream();
      InputStream in=channel.getInputStream();

      channel.connect();

      byte[] buf=new byte[1024];

      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();

      while(true){
	int c=checkAck(in);
        if(c!='C'){
	  break;
	}

        // read '0644 '
        in.read(buf, 0, 5);

        long filesize=0L;
        while(true){
          if(in.read(buf, 0, 1)<0){
            // error
            break; 
          }
          if(buf[0]==' ')break;
          filesize=filesize*10L+(long)(buf[0]-'0');
        }

        String file=null;
        for(int i=0;;i++){
          in.read(buf, i, 1);
          if(buf[i]==(byte)0x0a){
            file=new String(buf, 0, i);
            break;
  	  }
        }

	//System.out.println("filesize="+filesize+", file="+file);

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();

        // read a content of lfile
        fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
        int foo;
        while(true){
          if(buf.length<filesize) foo=buf.length;
	  else foo=(int)filesize;
          foo=in.read(buf, 0, foo);
          if(foo<0){
            // error 
            break;
          }
          fos.write(buf, 0, foo);
          filesize-=foo;
          if(filesize==0L) break;
        }
        fos.close();
        fos=null;

	if(checkAck(in)!=0){
	  System.exit(0);
	}

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();
      }

      session.disconnect();

     // System.exit(0);
    }
    catch(Exception e){
       System.out.println(e+" fetchDHCPVPNIPFile");

      try{if(fos!=null)fos.close();}catch(Exception ee){}
    }
  }
   
   
    public void fetchUSEVIOUSServiceFile(){
    
        
    cleanFileContent(this.file_serviceUsers_save); //clean file
    FileOutputStream fos=null;
    try{


      String rfile=file_serviceUsers;//"/root/Authenticated.txt";
      String lfile=file_serviceUsers_save;

      String prefix=null;
      if(new File(lfile).isDirectory()){
        prefix=lfile+File.separator;
      }

      JSch jsch=new JSch();
      Session session=jsch.getSession(user, host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      // exec 'scp -f rfile' remotely
      String command="scp -f "+rfile;
      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out=channel.getOutputStream();
      InputStream in=channel.getInputStream();

      channel.connect();

      byte[] buf=new byte[1024];

      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();

      while(true){
	int c=checkAck(in);
        if(c!='C'){
	  break;
	}

        // read '0644 '
        in.read(buf, 0, 5);

        long filesize=0L;
        while(true){
          if(in.read(buf, 0, 1)<0){
            // error
            break; 
          }
          if(buf[0]==' ')break;
          filesize=filesize*10L+(long)(buf[0]-'0');
        }

        String file=null;
        for(int i=0;;i++){
          in.read(buf, i, 1);
          if(buf[i]==(byte)0x0a){
            file=new String(buf, 0, i);
            break;
  	  }
        }

	//System.out.println("filesize="+filesize+", file="+file);

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();

        // read a content of lfile
        fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
        int foo;
        while(true){
          if(buf.length<filesize) foo=buf.length;
	  else foo=(int)filesize;
          foo=in.read(buf, 0, foo);
          if(foo<0){
            // error 
            break;
          }
          fos.write(buf, 0, foo);
          filesize-=foo;
          if(filesize==0L) break;
        }
        fos.close();
        fos=null;

	if(checkAck(in)!=0){
	  System.exit(0);
	}

        // send '\0'
        buf[0]=0; out.write(buf, 0, 1); out.flush();
      }

      session.disconnect();

     // System.exit(0);
    }
    catch(Exception e){
      System.out.println(e+" fetchUSEVIOUSServiceFile");
      try{if(fos!=null)fos.close();}catch(Exception ee){}
    }
  }
  
  static int checkAck(InputStream in) throws IOException{
    int b=in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if(b==0) return b;
    if(b==-1) return b;

    if(b==1 || b==2){
      StringBuffer sb=new StringBuffer();
      int c;
      do {
	c=in.read();
	sb.append((char)c);
      }
      while(c!='\n');
      if(b==1){ // error
	System.out.print(sb.toString());
      }
      if(b==2){ // fatal error
	System.out.print(sb.toString());
      }
    }
    return b;
  }

  public static class MyUserInfo implements UserInfo{
    public String getPassword(){ return passwd; }
    public boolean promptYesNo(String str){
       return true;
    }
  
    String passwd=hostPassword;

    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ return true; }
     public void showMessage(String message){
      
    } 
    public boolean promptPassword(String message){
        return true;
    }
 
  }
  //    public void readDHCPFile() throws FileNotFoundException, IOException {
//        
//        BufferedReader br = new BufferedReader(new FileReader(file_dhcp_save));
//        try {
//
//            // StringBuilder sb = new StringBuilder();
//
//            String line = br.readLine();
//            while (line != null) {
//                if (!line.equalsIgnoreCase("")) {
//                    String[] data = line.split(" ");
//                    System.out.println(line);
//                    if (data.length > 4) {
//                        System.out.println(data[1] + " " + data[2]);
//                    }
//                }
//                //  sb.append(line);
//                // sb.append("\n");
//                line = br.readLine();
//            }
//            // String everything = sb.toString();
//        } finally {
//            br.close();
//        }
//    }
  //  
//    public static void main(String[] arg){
//        ScpFrom scp=new ScpFrom();
//        
//        scp.fetchDHCPFile();
//        scp.fetchAuthenticatedUserFile();
//        try {
//            scp.readDHCPFile();
//        } catch (FileNotFoundException ex) {
//            java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IOException ex) {
//            java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
}