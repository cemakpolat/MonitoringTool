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
 * This program will demonstrate the file transfer from local to remote.
 *   $ CLASSPATH=.:../build javac ScpTo.java
 *   $ CLASSPATH=.:../build java ScpTo file1 user@remotehost:file2
 * You will be asked passwd. 
 * If everything works fine, a local file 'file1' will copied to
 * 'file2' on 'remotehost'.
 *
 */
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.logging.Level;
 
public class ScpTo{
    
    public  static String hostPassword = "secretPassword";
    public  String file_serviceUsers_save = "/root/serviceUsers.txt";
    public  String file_serviceUsers = "serviceUsers";
    public  String file_dhcpVPNIP_save = "/root/dhcpVPNIP.txt";
    public  String file_dhcpVPNIP = "dhcpVPNIP";
    
    public static String user = "root";
    public static String host = "";
    
    public void setFileNames(String append){
        file_serviceUsers_save=file_serviceUsers_save+"_"+append;
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

     public void sendMAC_VPNIPFileToServer() {
    cleanFileContent(this.file_dhcpVPNIP_save); //clean file

        FileInputStream fis = null;
        try {
            String rfile = file_dhcpVPNIP_save;
            String lfile = file_dhcpVPNIP;


            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);

            // username and password will be given via UserInfo interface.
            UserInfo ui = new MyUserInfo();
            session.setUserInfo(ui);
            session.connect();

            boolean ptimestamp = true;

            // exec 'scp -t rfile' remotely
            String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            if (checkAck(in) != 0) {
                System.exit(0);
            }

            File _lfile = new File(lfile);

            if (ptimestamp) {
                command = "T " + (_lfile.lastModified() / 1000) + " 0";
                // The access time should be sent here,
                // but it is not accessible with JavaAPI ;-<
                command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
                out.write(command.getBytes());
                out.flush();
                if (checkAck(in) != 0) {
                    System.exit(0);
                }
            }

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = _lfile.length();
            command = "C0644 " + filesize + " ";
            if (lfile.lastIndexOf('/') > 0) {
                command += lfile.substring(lfile.lastIndexOf('/') + 1);
            } else {
                command += lfile;
            }
            command += "\n";
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }

            // send a content of lfile
            fis = new FileInputStream(lfile);
            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) {
                    break;
                }
                out.write(buf, 0, len); //out.flush();
            }
            fis.close();
            fis = null;
            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }
            out.close();

            channel.disconnect();
            session.disconnect();

            //System.exit(0);
        } catch (Exception e) {
            System.out.println(e);
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception ee) {
            }
        }
    }
    
    public void sendServiceUsersFileToServer() {
    cleanFileContent(this.file_serviceUsers_save); //clean file

        FileInputStream fis = null;
        try {
            String rfile = file_serviceUsers_save;
            String lfile = file_serviceUsers;


            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);

            // username and password will be given via UserInfo interface.
            UserInfo ui = new MyUserInfo();
            session.setUserInfo(ui);
            session.connect();

            boolean ptimestamp = true;

            // exec 'scp -t rfile' remotely
            String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            if (checkAck(in) != 0) {
                System.exit(0);
            }

            File _lfile = new File(lfile);

            if (ptimestamp) {
                command = "T " + (_lfile.lastModified() / 1000) + " 0";
                // The access time should be sent here,
                // but it is not accessible with JavaAPI ;-<
                command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
                out.write(command.getBytes());
                out.flush();
                if (checkAck(in) != 0) {
                    System.exit(0);
                }
            }

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = _lfile.length();
            command = "C0644 " + filesize + " ";
            if (lfile.lastIndexOf('/') > 0) {
                command += lfile.substring(lfile.lastIndexOf('/') + 1);
            } else {
                command += lfile;
            }
            command += "\n";
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }

            // send a content of lfile
            fis = new FileInputStream(lfile);
            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) {
                    break;
                }
                out.write(buf, 0, len); //out.flush();
            }
            fis.close();
            fis = null;
            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }
            out.close();

            channel.disconnect();
            session.disconnect();

            //System.exit(0);
        } catch (Exception e) {
            System.out.println(e);
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception ee) {
            }
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
//  public static class MyUserInfo implements UserInfo, UIKeyboardInteractive{
//    public String getPassword(){ return passwd; }
//    public boolean promptYesNo(String str){
//      Object[] options={ "yes", "no" };
//      int foo=JOptionPane.showOptionDialog(null, 
//             str,
//             "Warning", 
//             JOptionPane.DEFAULT_OPTION, 
//             JOptionPane.WARNING_MESSAGE,
//             null, options, options[0]);
//       return foo==0;
//    }
//  
//    String passwd;
//    JTextField passwordField=(JTextField)new JPasswordField(20);
// 
//    public String getPassphrase(){ return null; }
//    public boolean promptPassphrase(String message){ return true; }
//    public boolean promptPassword(String message){
//      Object[] ob={passwordField}; 
//      int result=
//	  JOptionPane.showConfirmDialog(null, ob, message,
//					JOptionPane.OK_CANCEL_OPTION);
//      if(result==JOptionPane.OK_OPTION){
//	passwd=passwordField.getText();
//	return true;
//      }
//      else{ return false; }
//    }
//    public void showMessage(String message){
//      JOptionPane.showMessageDialog(null, message);
//    }
//    final GridBagConstraints gbc = 
//      new GridBagConstraints(0,0,1,1,1,1,
//                             GridBagConstraints.NORTHWEST,
//                             GridBagConstraints.NONE,
//                             new Insets(0,0,0,0),0,0);
//    private Container panel;
//    public String[] promptKeyboardInteractive(String destination,
//                                              String name,
//                                              String instruction,
//                                              String[] prompt,
//                                              boolean[] echo){
//      panel = new JPanel();
//      panel.setLayout(new GridBagLayout());
// 
//      gbc.weightx = 1.0;
//      gbc.gridwidth = GridBagConstraints.REMAINDER;
//      gbc.gridx = 0;
//      panel.add(new JLabel(instruction), gbc);
//      gbc.gridy++;
// 
//      gbc.gridwidth = GridBagConstraints.RELATIVE;
// 
//      JTextField[] texts=new JTextField[prompt.length];
//      for(int i=0; i<prompt.length; i++){
//        gbc.fill = GridBagConstraints.NONE;
//        gbc.gridx = 0;
//        gbc.weightx = 1;
//        panel.add(new JLabel(prompt[i]),gbc);
// 
//        gbc.gridx = 1;
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.weighty = 1;
//        if(echo[i]){
//          texts[i]=new JTextField(20);
//        }
//        else{
//          texts[i]=new JPasswordField(20);
//        }
//        panel.add(texts[i], gbc);
//        gbc.gridy++;
//      }
// 
//      if(JOptionPane.showConfirmDialog(null, panel, 
//                                       destination+": "+name,
//                                       JOptionPane.OK_CANCEL_OPTION,
//                                       JOptionPane.QUESTION_MESSAGE)
//         ==JOptionPane.OK_OPTION){
//        String[] response=new String[prompt.length];
//        for(int i=0; i<prompt.length; i++){
//          response[i]=texts[i].getText();
//        }
//	return response;
//      }
//      else{
//        return null;  // cancel
//      }
//    }
//  }
}