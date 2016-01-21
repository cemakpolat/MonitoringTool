/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package monitoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

/**
 *
 * @author cemakpolat
 */
public class IPERF {
    
    private static Thread iperfRunner;
    public String IP = "";
    public boolean isActive = true;
    public String selectedProtocol="TCP";
    public int selectedDuration=40;
    public int selectedDurationInSecond=1000;
    public String selectedBandwidth="1m";
    public String bandwidthEnabled="- b";
    public String updateDuration="2";
    private static Process process=null;
    public JTextArea jtextarea=null;
    public String iperfCommand="iperf";
    
    IPERF() {
    }
    public void stopIPERF(){
        process.destroy();
    }
    public void startIPERF() {
        iperfRunner = new Thread(new Runnable() {
            public void run() {
                writeConsole("IPERF Test is started");
                selectedDurationInSecond = selectedDurationInSecond * selectedDuration;
		Properties sysprops = System.getProperties();
		String osName = ((String)sysprops.get("os.name")).toLowerCase();
		if (new File("bin/iperf.exe").exists() && (osName.matches(".*win.*") || osName.matches(".*microsoft.*")))
		{		
                    iperfCommand="bin/iperf.exe";
                }else{
                    writeConsole("iperf.exe doesn't exist!");
                }
                try {
                    String[] command={};
                    //Give here the selected profile parameters 
                    if (selectedProtocol.endsWith("UDP")) {
                      String[]  command1={iperfCommand, "-c", IP, "-t", ""+selectedDuration,"-i",updateDuration,bandwidthEnabled,selectedBandwidth };
                      command=command1;
                    }else{
                         String[] command2 = {iperfCommand, "-c", IP, "-t", ""+selectedDuration,"-i",updateDuration};
                         command=command2;
                    }
                    //.String[] command = {"iperf", "-c", "192.168.178.48", "-t", ""+selectedDuration,"-i",updateDuration,bandwidthEnabled,selectedBandwidth };
                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);// or use another
                    process = pb.start();
                    
                    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line = "";
                    System.out.printf("Output of running %s is:\n",Arrays.toString(command));
                    while (((line = br.readLine()) != null)) {
                        writeConsole(line);
                    }

                }  catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } 
                writeConsole("IPERF Test is finished");

            }
        });
        try {
            iperfRunner.start();
        } catch (Exception e) {
            iperfRunner.run();
        }
    }
    //                    Timer t = new Timer();
//                    t.schedule(new TimerTask() {
//                        @Override
//                        public void run() {
//                            process.destroy();
//                        }
//                    }, selectedDurationInSecond);   // it will kill the process after 5 seconds (if it's not finished yet).
//                   // int i = process.waitFor();
//                    t.cancel();

    public void startIPERFTester() {
        iperfRunner = new Thread(new Runnable() {
            public void run() {
                writeConsole("IPERF Test is started");
                while (isActive) {
                    try {
                        Thread.sleep(100);
                        //Give here the selected profile parameters 
                        if(selectedProtocol.endsWith("TCP")){
                            bandwidthEnabled="";
                            selectedBandwidth="";
                        }else{
                            bandwidthEnabled="b";
                        }
                        
                        ProcessBuilder pb = new ProcessBuilder("iperf","c", IP,bandwidthEnabled,selectedBandwidth,"t",selectedDuration+"");
                        pb.redirectErrorStream(true);// or use another
                        // thread for
                        // detecting error
                        final Process process = pb.start();
                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));
                        
                        //Change this part
                        String line = "";
                        int failNumber = 0;
                        
                        for (int i = 0; i < 5; i++) {
                            if (((line = br.readLine()) != null)) {
                                if (line.contains(" Connection refused")) {
                                    failNumber = failNumber + 1;
                                } else if (line.contains("connect failed")) {
                                    failNumber = failNumber + 1;
                                } else if (line.contains("No Route")) {
                                    failNumber = failNumber + 1;
                                } else if (line.contains("Unreachable")) {
                                    failNumber = failNumber + 1;
                                } else if (line.contains("unreachable")) {
                                    failNumber = failNumber + 1;
                                } else if (line.contains("time")
                                        && line.contains(" ms")) {
                                    
                                }
                            }
                        }

                        Timer t = new Timer();
                        t.schedule(new TimerTask() {

                            @Override
                            public void run() {
                                process.destroy();
                            }
                        }, 5000);   // it will kill the process after 5 seconds (if it's not finished yet).
                        int i = process.waitFor();
                        t.cancel();
                        
                        
                        process.destroy();
                        if (failNumber >= 4) {
                            writeConsole("Iperf fail occured, possible connection issue!");

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
            iperfRunner.start();
        } catch (Exception e) {
            iperfRunner.run();
        }
    }

    public void stopIperfTester() {
        isActive = false;
    }

    public void setTextArea(JTextArea jtext){
    this.jtextarea=jtext;
    }
    public void writeConsole(String message) {
        //System.out.println(PINGConnectivityTester.class.getName() + " " + message);
        if(this.jtextarea!=null){
              this.jtextarea.append(message+"\n");
        }else{
             System.out.println("Text are is null!");
        }
    }
}
