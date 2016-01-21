/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package monitoring;

import conn.JavaToJavaClient;
import conn.JavaToJavaServerTimeOut;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextArea;
import monitoring.ServerRequester.SNMPOIDCollections.SNMPDEF;

/**
 *
 * @author cemakpolat
 */
public class ServerInterface {
    
    public ServerRequester snmp=new ServerRequester();
    public ScpFrom scp = new ScpFrom();

    public ScpTo scpTo = new ScpTo();
    public ArrayList<UseViOSUser> usevoisUsers=new ArrayList<UseViOSUser>();
   
    public ArrayList<MACVPNIPTuple> macVPNIPUserList=new ArrayList<MACVPNIPTuple>();

    public ArrayList<CPUObject> cpuInfo = new ArrayList<CPUObject>();
    public ArrayList<MemObject> memInfo= new ArrayList<MemObject>();
    public ArrayList<ProtocolObject> protocolsInfo = new ArrayList<ProtocolObject>();
    public ArrayList<NetworkInterfaces> netInfo=new ArrayList<NetworkInterfaces>();
    public int snmpRequestInterval=5000;//5sn
    public HostInfo host=new HostInfo();
    public JTextArea jtarea;
    int maxListSize=20;
    public String serverIP="192.168.1.1";
    public String status_reporter="";
    public String fileAppend="default";
    
    ArrayList<String> authenticatedMACs =new ArrayList<String>();
    List<ConnectedUsers> users = Collections.synchronizedList(new ArrayList<ConnectedUsers>());


    public ServerInterface(){
    
    }
    
    /////////////  CONTROL INTERFACE //////////
        
    public void loadDefaultUsers(){

         
       //read file from server
        getUSEVIOUSUSers();
       
    }
    
    public class UseViOSUser{
        public String userName="";
        public String MAC="";
    }
    public class MACVPNIPTuple{
        public String VPNIP="";
        public String MAC="";
    }
    
    public boolean contains(ArrayList<UseViOSUser> list,UseViOSUser user){
            boolean result=false;
            for(int i=0;i<list.size();i++){
                if(list.get(i).userName.equalsIgnoreCase(user.userName) || list.get(i).MAC.equalsIgnoreCase(user.MAC) ){
                    result=true;
                    break;
                }       
            }
            return result;    
    }
    
    public void getUSEVIOUSUSers() {
        try {
            scp.fetchUSEVIOUSServiceFile();
            scp.fetchDHCPVPNIPFile();
            readUSEVIOSFile();
            readDHCPVPNIPFile();
        } catch(Exception e){
            System.out.println(e +" in getUSEVIOUSUSers");
        }
    }
    
    
    //read usevious file
    public void readUSEVIOSFile() throws FileNotFoundException, IOException{
        BufferedReader br =null;
        usevoisUsers.clear();
        try {
            br = new BufferedReader(new FileReader(scp.file_serviceUsers_save));
            String line = br.readLine();
            while (line != null) {
                if(!line.equalsIgnoreCase("")){
                   String[] str=line.split(",");
                   if(str.length>1){
                        UseViOSUser user=new UseViOSUser();
                        user.MAC=str[1];
                        user.userName=str[0];
                        usevoisUsers.add(user);
                   }
                }
                line = br.readLine();
            }
        }catch (FileNotFoundException ex) {
            System.out.println(scp.file_serviceUsers_save+" File not found");
            //java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            System.out.println("IO exception");
            //java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
             if(br!=null){
                br.close();
            }
        }
    }
    
    // read dhcp file
    public void readDHCPVPNIPFile() throws FileNotFoundException, IOException{
        
        BufferedReader br =null;
        this.macVPNIPUserList.clear();
        try {
            br=new BufferedReader(new FileReader(scp.file_dhcpVPNIP_save));
            String line = br.readLine();
            while (line != null) {
                if(!line.equalsIgnoreCase("")){
                   String[] str=line.split(" ");
                   if(str.length>1){
                        
                       MACVPNIPTuple tuple=new MACVPNIPTuple();
                       tuple.MAC=str[0];
                       tuple.VPNIP=str[1];
                        
                       this.macVPNIPUserList.add(tuple);
                   }
                }
                line = br.readLine();
            }
        }catch (FileNotFoundException ex) {
            //java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex.getMessage());
            System.out.println(scp.file_dhcpVPNIP_save+" not found!");
        } catch (IOException ex) {
            //java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("IO Exxeption");
        }
         finally {
            if(br!=null){
                br.close();
            }
            
        }
    }
    
    
    public static boolean loadingStart=false;
    
    public void sendUSEViOSUsersToServer() {
        Thread loop = new Thread(new Runnable() {
            @Override
            public void run() {
               // if(loadingStart){
                   writeUSEVIOSUsersToList(); 
               // }
                loadingStart=true;
                scpTo.sendServiceUsersFileToServer();
            }
        });
        loop.start();

    }
    public static boolean loadingStartForMACVPN=false;

    public void sendMACVPNIPUserListToServer() {
        Thread loop = new Thread(new Runnable() {
            @Override
            public void run() {
               // if(loadingStartForMACVPN){
                   writeMACVPNIPUserListToList(); 
                //}
                loadingStartForMACVPN=true;
                scpTo.sendMAC_VPNIPFileToServer();
            }
        });
        loop.start();

    }
    
    
      public void writeUSEVIOSUsersToList() {

        PrintWriter writer=null;
        try {
            writer = new PrintWriter(scp.file_serviceUsers_save, "UTF-8");

            for(int i=0;i<this.usevoisUsers.size();i++){
             //   console(this.usevoisUsers.get(i).userName+","+this.usevoisUsers.get(i).MAC);
                 writer.println(this.usevoisUsers.get(i).userName+","+this.usevoisUsers.get(i).MAC);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServerInterface.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ServerInterface.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(writer!=null){
                writer.close();
            }
        }
    }
    
    public void writeMACVPNIPUserListToList() {

        PrintWriter writer=null;
        try {
            writer = new PrintWriter(scpTo.file_dhcpVPNIP, "UTF-8");
            //String str="";


            for(int i=0;i<this.macVPNIPUserList.size();i++){
                
               // str=str+this.usevoisUsers.get(i).userName+","+this.usevoisUsers.get(i).MAC+"\n";
                 writer.println(this.macVPNIPUserList.get(i).MAC+" "+this.macVPNIPUserList.get(i).VPNIP);
            }
            //writer.println(str);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ServerInterface.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ServerInterface.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if(writer!=null){
                writer.close();
            }
        }
    }
    ///////////////////////////////////////////
    
    public void setTerm(String append){
        fileAppend=append;
        scp.setFileNames(append);
    }
    
    public void getConnectedUsersOverSCP() {
  
        try {
            scp.fetchAuthenticatedUserFile();
            scp.fetchDHCPFile();
            readMACFile();
            readDHCPFile();
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void readMACFile() throws FileNotFoundException, IOException {
        BufferedReader br = null;
        try {
           br = new BufferedReader(new FileReader(scp.file_authenticated_save));

            authenticatedMACs.clear();
            String line = br.readLine();
            while (line != null) {
                if (!line.equalsIgnoreCase("")) {
                    if (validateMAC(line)) {
                        authenticatedMACs.add(line);
                    }
                }

                line = br.readLine();
            }
        }catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally {
            if(br!=null){
                br.close();
            } 
        }
    }

    public boolean isClientAuthenticated(String mac) {

        for (int i = 0; i < this.authenticatedMACs.size(); i++) {
            if (this.authenticatedMACs.get(i).equalsIgnoreCase(mac)) {
                return true;
            }
        }
        return false;
    }
    List<ConnectedUsers> tempUserList = new ArrayList<ConnectedUsers>();

    public void updateUserList() {
        synchronized (users) {
            tempUserList = users;
        }
    }
    public void readDHCPFile() throws FileNotFoundException, IOException {
        BufferedReader br = null;
        try {
             br = new BufferedReader(new FileReader(scp.file_dhcp_save));
            // StringBuilder sb = new StringBuilder();
            synchronized (users) {
                String line = br.readLine();
                users.clear();// clear all usrs
                while (line != null) {
                    if (!line.equalsIgnoreCase("")) {
                        String[] data = line.split(" ");
                        if (data.length > 4) {
                            if (isClientAuthenticated(data[1])) {
                                if (validateMAC(data[1]) && validateIP(data[2])) {
                                    ConnectedUsers client = new ConnectedUsers();
                                    client.mac = data[1];
                                    client.ip = data[2];
                                    users.add(client);
                                }
                            }
                        }
                    }
                    //  sb.append(line);
                    // sb.append("\n");
                    line = br.readLine();
                }
            }
            // String everything = sb.toString();
        }catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ScpFrom.class.getName()).log(Level.SEVERE, null, ex);
        } 
        finally {
            if(br!=null){
                br.close();
            } 
        }
        //console("UserList:"+users.size());

    }
    private static final String PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    public static boolean validateIP(final String ip) {

        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
    private static final String MAC_PATTERN = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";

    public static boolean validateMAC(final String mac) {
        Pattern pattern = Pattern.compile(MAC_PATTERN);
        Matcher matcher = pattern.matcher(mac);
        return matcher.matches();
    }
 //   public void getFileOverSCP(){

    
    /**
     * Verifies whether a string contains a valid mac addres, a valid mac address consists of
     * 6 pairs of [A-F,a-f,0-9] separated by ':', e.g. <code>0A:F6:33:19:DE:2A</code>.
     *
     * @param mac String containing the possible mac address
     * @return true If the specified string contains a valid mac address, false otherwise.
     */
    protected static boolean isValidMac(String mac) {
        if (mac.length() != 17) {
            return false;
        }
        char[] chars = mac.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (i % 3 == 2) {
                if (':' != c) {
                    return false;
                }
            }
            else if (!(('0' <= c) && (c <= '9')) &&
                     !(('a' <= c) && (c <= 'f')) &&
                     !(('A' <= c) && (c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
    
    
    
    
    /////////////////////////////////////////////////////
    

    public void stopSNMPRequester() {
        this.writeStatusReport("Server Communication is stopping ...");
        this.state_snmp_loop = false;

        // Addition for connected users
        /*
            this.periodClientStateRequesterState = false;
            this.startServerState = false;
         */
    }

    public void startSNMPRequester() {
        this.writeStatusReport("Server Communication is starting ...");
        this.state_snmp_loop = true;
        this.runSNMPRerequestsLoop();

        //Additional for connected users
        /*
            this.startServerState = true;
            this.getConnectedUsers();
            this.startServer();
         */
    }

    public void setSNMPRequesterIP(String IP) {
        snmp.IP = IP;
        this.serverIP = IP;
        //console("Host IP:"+IP);
        scp.setHostIP(IP);
        scpTo.setHostIP(IP);
    }

    public static void writeStatusReport(String str){
        synchronized(MainFrame.jLabel_status_report) {
            MainFrame.jLabel_status_report.setText(str); 
         }
    }
    
    public void setSNMPRequester(String snmp_version){
        if(snmp_version.contains("SNMPv1")){
            snmp.setSNMPVersion(ServerRequester.VERSION_1);
        }else if(snmp_version.contains("SNMPv2")){
            snmp.setSNMPVersion(ServerRequester.VERSION_2C);
        }else if(snmp_version.contains("SNMPv3")){
            snmp.setSNMPVersion(ServerRequester.VERSION_3);
        }
    }
    
    public String getSNMPRequestOutput(String OID) {
        String result = "";
        ArrayList<String> simpleSNMPRequest = snmpRequest(OID);

        for (int i = 0; i < simpleSNMPRequest.size(); i++) {
            result = result + simpleSNMPRequest.get(i) + "\n";
        }
        if (simpleSNMPRequest.isEmpty()) {
            result = "There is nothing to show!";
        }
        return result;
    }
    
    
    public  ArrayList<String> snmpRequest(String OID){
        ArrayList<String> simpleSNMPRequest=new ArrayList<String>();

        simpleSNMPRequest.clear();
        simpleSNMPRequest=snmp.sentRequest(OID);
        return simpleSNMPRequest;
    }
    
    /**
     * Future improvement Point
     */    
    
    ArrayList<String> snmpTree=new ArrayList<String>();
    public void getWholeSNMPTree(){       
        snmpTree=snmp.getWithWalk(snmp.snmpcol.getNetIntTransmittedData()); 
        
    }    
        
    public boolean state_snmp_loop=true;
    public void runSNMPRerequestsLoop() {
        Thread loop = new Thread(new Runnable() {
            @Override
            public void run() {
                 console("Server requests are started.");
                while (state_snmp_loop) {
                    writeStatusReport("CPU Info is requested ...");
                    getCPUInfo();
                    sleep(100);
                    writeStatusReport("Memory Info is requested ...");
                    getMemInfo();
                    sleep(100);
                    writeStatusReport("Network Info is requested ...");
                    getNetInfo();
                    sleep(100);
                    writeStatusReport("Proto Info is requested ...");
                    getProtoInfo();
                    sleep(100);
                    writeStatusReport("Host Info is requested ...");
                    getHostInfo();
                    
                   // getConnectedUsers(); // This works with other device without the mikrokernel PC
                    getConnectedUsersOverSCP();
                    
                    sleep(snmpRequestInterval);

                }
                writeStatusReport("Server requests are stopped.");
                console("Server requests are stopped.");
            }
        });
        loop.start();
    }
    public void sleep(int duration){
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Logger.getLogger(ServerInterface.class.getName()).log(Level.SEVERE, null, ex);
        }
    
    }
    
    public void getCPUInfo() {
       CPUObject obj=getRequiredCPUContent(snmp.getList(snmp.snmpcol.getCPUOIDs()));
       if(obj!=null){
            addInList(obj);
       }
    }
    public void console(String str){
        this.jtarea.append(str+"\n");
    };
    
    
    void addInList(Object obj){            

        if(obj instanceof CPUObject){

            if(this.cpuInfo.size()<maxListSize){
                this.cpuInfo.add((CPUObject)obj);
            }else{
                this.cpuInfo.remove(0);
                this.cpuInfo.add((CPUObject)obj);
            }
        }else if(obj instanceof MemObject){
             if(this.memInfo.size()<maxListSize){
                this.memInfo.add((MemObject)obj);
            }else{
                this.memInfo.remove(0);
                this.memInfo.add((MemObject)obj);
            }
        
        }else if(obj instanceof NetworkInterfaces){
             if(this.netInfo.size()<maxListSize){
                this.netInfo.add((NetworkInterfaces)obj);
            }else{
                this.netInfo.remove(0);
                this.netInfo.add((NetworkInterfaces)obj);
            }
        
        }else if(obj instanceof ProtocolObject){
             if(this.protocolsInfo.size()<maxListSize){
                this.protocolsInfo.add((ProtocolObject)obj);
            }else{
                this.protocolsInfo.remove(0);
                this.protocolsInfo.add((ProtocolObject)obj);
            }
        }else{
            console("Unknown object!");
        }
    }
    
    public CPUObject getRequiredCPUContent(ArrayList<ServerRequester.SNMPOIDCollections.SNMPOBJ> a){
        
       CPUObject obj=new CPUObject();

       for(int i=0;i<a.size();i++){
           if(!a.get(i).value.contains("noSuchInstance")){
              //   console("Obtained value:"+a.get(i).value.toString()+" "+a.get(i).definition.toString());
                if(a.get(i).definition.contains(SNMPDEF.cpu_oneMinLoad)){
                    obj.oneMinLoad=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_fiveMinLoad)){
                    obj.fiveMinLoad=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_fifthenMinLoad)){
                    obj.fifthenMinLoad=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_idleCPUTimePercentage)){
                    obj.idleCPUTimePercentage=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_idleRawCPUTime)){
                    obj.idleRawCPUTime=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_niceRawCPUTime)){
                    obj.niceRawCPUTime=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_systemCPUTimePercentage)){
                    obj.systemCPUTimePercentage=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_systemRawCPUTime)){
                    obj.systemRawCPUTime=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_userCPUTimePercentage)){
                    obj.userCPUTimePercentage=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.cpu_userRawCPUTime)){
                    obj.userRawCPUTime=a.get(i).value;
                }
           }
       }
       return obj;
    }
    
    
    public void getMemInfo() {        
       MemObject obj=getRequiredMemContent(snmp.getList(snmp.snmpcol.getMEMOIDs()));
       if(obj!=null){
            addInList(obj);
       }
    }

    public MemObject getRequiredMemContent(ArrayList<ServerRequester.SNMPOIDCollections.SNMPOBJ> a) {
        MemObject obj = new MemObject();
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).value.contains("noSuchInstance")) {
             //   console(a.get(i).value.toString());
                if (a.get(i).definition.contains(SNMPDEF.mem_availableSwapSpace)) {
                    obj.availableSwapSpace = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.mem_totalBufferedRAM)) {
                    obj.totalBufferedRAM = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.mem_totalCachedMemory)) {
                    obj.totalCachedMemory = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.mem_totalFreeRAM)) {
                    obj.totalFreeRAM = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.mem_totalRAMinMachine)) {
                    obj.totalRAMinMachine = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.mem_totalSharedRAM)) {
                    obj.totalSharedRAM = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.mem_totalSwapSize)) {
                    obj.totalSwapSize = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.mem_totalUsedRAM)) {
                    obj.totalUsedRAM = a.get(i).value;
                }
            }
        }
        return obj;
    }
    public void getConnectedUsers(){
        users.clear();
        ArrayList<String> userstemp=snmp.getWithWalk(snmp.snmpcol.getConnectedUsers());
        
        if(userstemp.size()>0){
            int count=userstemp.size()/3;
            
            if(count>0){ 
                for(int i=0;i<count;i++){
                  userstemp.remove(0);
                }
                int userInfo=userstemp.size();
                for(int i=0;i<userInfo/2;i++){
                    //c
                    ConnectedUsers user=new ConnectedUsers();
                    user.mac=userstemp.get(i);
                    user.ip=userstemp.get(i+(userInfo/2));  
                    users.add(user);
                }
            }
        }else{
            console("Any user is connected!");
        }
    }
    public void fillNull(NetworkInterfaces nit){
    
        ArrayList<String> list1=new ArrayList<String>();
        for(int i=0;i<nit.interfaces.size();i++){
            list1.add("0");
            
        }
        nit.transmitted=list1;
        nit.received=list1;
        
    }
    public  ArrayList<String> getIncreaseInInteraces(ArrayList<String> list1,ArrayList<String> list2){
        
        ArrayList<String> result=new ArrayList<String>();
        long diff=0;
        if(list1.size()==list2.size()){
            for(int i=0;i<list1.size();i++){
                diff=Long.parseLong(list1.get(i))-Long.parseLong(list2.get(i));
                result.add(""+diff);
            }
        }else{
            for(int i=0;i<list1.size();i++){
                result.add(""+diff);
            }
            console("Provided interfaces are not common, previous Net Interface Count:"+ list1.size()+" Current interface:"+list2.size());
            console("Therefore an empty list is being retured!");
        }
        return result;
        
    }
    
    public boolean firstLoop=true;
    public void getNetInfo() {
        NetworkInterfaces nit = new NetworkInterfaces();
        nit.interfaces= snmp.getWithWalk(snmp.snmpcol.getNetworkInterfaces());
        if(nit.interfaces.size()>0){
        if(!firstLoop){
             // console("Get Net Info");
              nit.transmitted=snmp.getWithWalk(snmp.snmpcol.getNetIntTransmittedData());
              nit.received=snmp.getWithWalk(snmp.snmpcol.getNetIntReceivedData()); 
              nit.previousReceived=nit.received;
              nit.previousTransmitted=nit.transmitted;
              
              nit.transmitted=getIncreaseInInteraces(nit.transmitted,netInfo.get(netInfo.size()-1).previousTransmitted);
              nit.received=getIncreaseInInteraces(nit.received,netInfo.get(netInfo.size()-1).previousReceived);
              
            
                      
        }else{
             nit.previousTransmitted=snmp.getWithWalk(snmp.snmpcol.getNetIntTransmittedData());
             nit.previousReceived=snmp.getWithWalk(snmp.snmpcol.getNetIntReceivedData()); 
             fillNull(nit);
             this.firstLoop=false;
             //console("First Loop");
        }
                addInList(nit);
        }else{
            console("The network interfaces couldn't be obtained!");
        }
    }
    public void printList(ArrayList<String> list,String goal){
        console("===== "+goal+" =====");
        for(int i=0;i<list.size();i++){
            console(list.get(i).toString());
        }
        console("==========================");
    }
    

    public void getHostInfo() {
        ArrayList<ServerRequester.SNMPOIDCollections.SNMPOBJ> temp = null;
        temp = snmp.getList(snmp.snmpcol.getHOSTOIDs());

        host = getRequiredHostContent(temp);
        if (host == null) {
            console("Host informations are null");

        }
    }
    
    public HostInfo getRequiredHostContent(ArrayList<ServerRequester.SNMPOIDCollections.SNMPOBJ> a) {
        HostInfo obj = new HostInfo();
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).value.contains("noSuchInstance")) {
               // console("host:"+a.get(i).value.toString());
                if (a.get(i).definition.contains(SNMPDEF.host_info)) {
                    obj.info = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.host_sysDate)) {
                    obj.sysDate = a.get(i).value;
                } else if (a.get(i).definition.contains(SNMPDEF.host_sysUpTime)) {
                    obj.sysUpTime = a.get(i).value;
                } else {
                    console("Host: unknown properties");
                }
            }
        }
        return obj;
    }
    
    public void getProtoInfo() {         
        
        ArrayList<ServerRequester.SNMPOIDCollections.SNMPOBJ> a=null;
        ArrayList<ServerRequester.SNMPOIDCollections.SNMPOBJ> temp=null;
        
        temp=snmp.getList(snmp.snmpcol.geProtocols_IP());
        if(temp!=null || temp.size()>0){
            a=temp;
        }
        temp=snmp.getList(snmp.snmpcol.geProtocols_TCP());
        if(temp!=null || temp.size()>0){
            a.addAll(temp);
        }
        temp=snmp.getList(snmp.snmpcol.geProtocols_UDP());
         if(temp!=null || temp.size()>0){
            a.addAll(temp);
        }
        temp=snmp.getList(snmp.snmpcol.geProtocols_ICMP());
        if(temp!=null || temp.size()>0){
            a.addAll(temp);
        }
        
        ProtocolObject obj=null;
        obj=getRequiredProtocolsContent(a);
        if(obj!=null){
            addInList(obj);
        }
    }
    
    
    public ProtocolObject getRequiredProtocolsContent(ArrayList<ServerRequester.SNMPOIDCollections.SNMPOBJ> a){
       ProtocolObject obj=new ProtocolObject();
       for(int i=0;i<a.size();i++){
            if (!a.get(i).value.contains("noSuchInstance")) {
                //console(a.get(i).value.toString());
                if(a.get(i).definition.contains(SNMPDEF.prot_icmpInPacket)){
                    obj.icmpInPacket=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.prot_icmpOutPacket)){
                    obj.icmpOutPacket=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.prot_ipInRequests)){
                    obj.ipInRequests=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.prot_ipInDelivers)){
                    obj.ipInDelivers=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.prot_ipOutRequests)){
                    obj.ipOutRequests=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.prot_tcpInSegment)){
                    obj.tcpInSegment=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.prot_tcpOutSegment)){
                    obj.tcpOutSegment=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.prot_udpInDatagram)){
                    obj.udpInDatagram=a.get(i).value;
                }else if(a.get(i).definition.contains(SNMPDEF.prot_udpOutDatagram)){
                    obj.udpOutDatagram=a.get(i).value;
                }
           }
       }
       return obj;
    }
    


    void setTextArea(JTextArea jtextarea) {       
        this.jtarea=jtextarea;
    }
    
    public class CPUObject {
      
        public String oneMinLoad="0";
        public String fiveMinLoad="0";
        public String fifthenMinLoad="0";
        public String userCPUTimePercentage="0";
        public String userRawCPUTime="0";
        public String systemCPUTimePercentage="0";
        public String systemRawCPUTime="0";
        public String idleCPUTimePercentage="0";
        public String idleRawCPUTime="0";
        public String niceRawCPUTime="0";
        
        public CPUObject() {}
      
    }

    public class MemObject {
        
        public String totalSwapSize="0";
        public String availableSwapSpace="0";
        public String totalRAMinMachine="0";
        public String totalUsedRAM="0";
        public String totalFreeRAM="0";
        public String totalSharedRAM="0";
        public String totalBufferedRAM="0";
        public String totalCachedMemory="0";
        public MemObject() { }
    }

        /*Network Interfaces
		 	1.3.6.1.2.1.2.2.1.2	
        received bytes from these interfaces
    			1.3.6.1.2.1.2.2.1.10
	transmitted bytes to these interfaces
			1.3.6.1.2.1.2.2.1.16
        */
    
   
    public class ConnectedUsers {

        String mac = "";
        String ip = "";

        public ConnectedUsers() {
        }
    }

    public class NetworkInterfaces{
        
        //previous results
        public  ArrayList<String> previousTransmitted =new ArrayList<String>();
        public ArrayList<String> previousReceived=new ArrayList<String>();
        
        public ArrayList<String> interfaces=new ArrayList<String>();
        public ArrayList<String> transmitted=new ArrayList<String>();
        public ArrayList<String> received=new ArrayList<String>();
        public NetworkInterfaces(){}
    }

    public class ProtocolObject {
        
        public String ipInRequests="0";
        public String ipOutRequests="0";
        public String ipInDelivers="0";
        
        public String icmpInPacket="0";
        public String icmpOutPacket="0";
        
        public String tcpInSegment="0";
        public String tcpOutSegment="0";
        
        public String udpInDatagram="0";
        public String udpOutDatagram="0";
        public String udpLocalPorts="0";
        public String snmp;
        public ProtocolObject(){}
        
    }
    
    public class HostInfo{
        String info="0";
        String sysUpTime="0";
        String sysDate="0";
        public HostInfo(){}
    }
    

    /////////////// SERVER CLIENT COMMUNICATION //////////
    
    

    public static String AUTHENTICATEDUSERS = "1";
    public static JavaToJavaClient mainClient;
    public static JavaToJavaServerTimeOut accessPointMessageServer;
   
    public static int APSocketNumber = 33111;
    public static int APListenetrServerSN = 33112;
    public ArrayList<ConnectedUsers> authUsers = new ArrayList<ConnectedUsers>();
    
    /**
     * Start Server Communication between Client and AP
     */
    public boolean startServerState=true;
    public void startServer() {
        Thread serverThread = null;
        try {
            console("Creating the Server for Connected Users");

            accessPointMessageServer = new JavaToJavaServerTimeOut(APListenetrServerSN);
            serverThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        console("Socket connection to AP is being opened...");
                        while (startServerState) {

                            String message = accessPointMessageServer.getMessage();
                            console("Received Message: " + message);
                            // force to hand over due to unappropriate conf
                            String[] mess = message.split("=");
                            if (mess.length > 1) {
                                if (!mess[1].equalsIgnoreCase("")) {
                                    if (mess[0].contains("ConnectedUsers")) {
                                        fillAuthenticatedUsers(mess[1]);
                                    } else {
                                        console("Unknown Message is received! \n Message Content: " + mess[1]);
                                    }
                                } else {
                                }
                            } else {
                                console("Connected User Messages Content is empty!");
                            }
                        }
                        console("Socket connection to AP is being closed...");
                        accessPointMessageServer.serverSocket.close();
                    } catch (SocketException e) {
                        console("Socket exception occured, in 2 seconds socket connection is going to be established.");
                        startServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                        startServer();
                    }
                }
            });
            serverThread.start();
        } catch (Exception e) {
            serverThread.run();
        }
    }
    /*
     * Send Request To Server
     */
    protected void sendRequestToAP(String str) {
        // open a socket connection to Measurement Point
        String ourResponse = str;
       // System.out.println("send Results: " + str);

        try {
            mainClient = new JavaToJavaClient(APSocketNumber);// we need to //
           
            // Port number
            console("IPAddress of Server " + serverIP);
            
            long QoSvalue = mainClient.send(ourResponse, serverIP);
            if (QoSvalue < 0) {
                console("No connection ");
            } else if (QoSvalue == 1) {
                console("Socket connection timeout, continuing ");
            } else if (QoSvalue == 2) {
                console("Socket connection error, continuing ");
            } else {
                console("Successfully sended");
            }
        } catch (Exception e) {
            System.out.println("Problem occurred by sending test results");
        }
    }
     /**
      *Start to get the connected clients on Server. 
      */
    public boolean periodClientStateRequesterState=true;
    public void getConnectedClients() {
        console("Connected Users Request is activated.");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (periodClientStateRequesterState) {
                    try {
                        Thread.sleep(snmpRequestInterval);
                        sendRequestToAP(AUTHENTICATEDUSERS);
                    } catch (Exception e) {
                        console(e.getMessage());
                    }
                }
            }
        });
        thread.start();
    }

    public void fillAuthenticatedUsers(String str) {
        String[] obj = str.split(";");

        for (int j = 0; j < obj.length; j++) 
        {
            ConnectedUsers user = new ConnectedUsers();
            String[] val = obj[j].split(",");
            if (val.length > 1) {
                user.mac = val[0];
                user.ip = val[1];
            }else{
                console("Connected Users' Message is empty!");
            }
            authUsers.add(user);
        }
    }
    
}
