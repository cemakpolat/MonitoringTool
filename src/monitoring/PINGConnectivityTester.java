package monitoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import javax.swing.JTextArea;

/**
 * Ping connectivity Tester is merely responsible for checking the connection
 * between the tester and testee. This could be easily stopped and restarted
 * again by considering the system resources.
 *
 * @author cemakpolat
 *
 */
public class PINGConnectivityTester {

    private static Thread connectivityChecker;
    public String IP = "";
    public boolean isConTestActive = true;
    public JTextArea jtext=null;

    
    PINGConnectivityTester() {
    }
    public void startConnectivityTester() {

        isConTestActive=true;
        connectivityChecker = new Thread(new Runnable() {
            public void run() {
                writeConsole("PING Test is started");
                while (isConTestActive) {
                    try {
                        Thread.sleep(100);
                        //writeConsole("PING Request is sent");
                           
                        
                        if(System.getProperty("os.name").startsWith("Windows")) {   
                                // For Windows
                                 //pb = new ProcessBuilder("ping","-n","2", IP);
                             pingHostByCommand(IP);
                             Thread.sleep(1000);

                        } else {
                                // For Linux and OSX
                               ProcessBuilder  pb = new ProcessBuilder("ping","-c","2", IP);
                                //pb = new ProcessBuilder(pingCommand, IP);     
                                 pb.redirectErrorStream(true);
                                Process process = pb.start();
                                BufferedReader br = new BufferedReader(
                                        new InputStreamReader(process.getInputStream()));

                                String line = "";
                                int failNumber = 0;
                                int successNumber = 0;

                                for (int i = 0; i < 5; i++) {
                                    if (((line = br.readLine()) != null)) {
                                        if (line.contains("timeout")) {
                                            failNumber = failNumber + 1;
                                        } else if (line.contains("No route")) {
                                            failNumber = failNumber + 1;
                                        } else if (line.contains("No Route")) {
                                            failNumber = failNumber + 1;
                                        } else if (line.contains("Unreachable")) {
                                            failNumber = failNumber + 1;
                                        } else if (line.contains("unreachable")) {
                                            failNumber = failNumber + 1;
                                        } else if (line.contains("time")
                                                && line.contains(" ms")) {
                                            successNumber = successNumber + 1;
                                            writeConsole(line);
                                        }
                                    }
                                }
                                writeConsole("PING is called");
                                process.destroy();
                                //writeConsole("Ping fail number is " + failNumber);
                                if (failNumber >= 4) {
                                    writeConsole("ping fail occured, possible connection issue!");

                                }
                        }
                        
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        });
        try {
            connectivityChecker.start();
        } catch (Exception e) {
            connectivityChecker.run();
        }

    }
    public boolean pingHostByCommand(String host){
    try{
        String strCommand = "";
        System.out.println("My OS :" + System.getProperty("os.name"));
        if(System.getProperty("os.name").startsWith("Windows")) {
            // construct command for Windows Operating system
            strCommand = "ping -n 1 " + host;
        } else {
            // construct command for Linux and OSX
            strCommand = "ping -c 1 " + host;
        }
//        System.out.println("Command: " + strCommand);
        // Execute the command constructed
        Process myProcess = Runtime.getRuntime().exec(strCommand);
        myProcess.waitFor();
        
        BufferedReader stdInput = new BufferedReader(new 
        InputStreamReader(myProcess.getInputStream()));

        BufferedReader stdError = new BufferedReader(new 
        InputStreamReader(myProcess.getErrorStream()));

        // read the output from the command
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            writeConsole(s);
        }

        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
        if(myProcess.exitValue() == 0) {
            return true;
        } else {
            return false;
        }
    } catch( Exception e ) {
        e.printStackTrace();
        return false;
    }
}

//public void pingHostByJavaClass(String host, int timeout){
//    try {            
//        boolean isreachable = InetAddress.getByName(host).isReachable(timeout);
//        System.out.println(isreachable);
//    } catch (IOException ex) {
//        ex.printStackTrace();
//    }
//}
    public void stopConnectivityTester() {
         writeConsole("PING Test is stopped");
        isConTestActive = false;
    }

    public void writeConsole(String message) {
        //System.out.println(PINGConnectivityTester.class.getName() + " " + message);
        if(this.jtext!=null){
           jtext.append(message+"\n");     
        }
    }
    public void setTextArea(JTextArea jtext){
        this.jtext=jtext;
    }
}
