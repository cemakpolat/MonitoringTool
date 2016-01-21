/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package monitoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

/**
 *
/*
 How to configuration at Openwrt

 The UDP packages can be dropped by firewall, therefore add some rules for openning the UDP ports 161 and 162
 (In my case I added the ports in /etc/config/init.d) In your case, /etc/config/firewall
 * In order to modify the snmpd configuration, please look at the file locating under /etc/config/snmpd
 You need to start snmpd and instal snmp packets along with snmp-utils(?)
 * start:/etc/init.d/snmpd start
 * enalbe:/etc/init.d/snmpd enable
 
 * @author cemakpolat
 */

public class ServerRequester {
   public String IP = "192.168.126.1";
   //SnmpUtility util = new SnmpUtility(VERSION_2C,IP);
  
   SNMPOIDCollections snmpcol=new SNMPOIDCollections();
   
   public static int VERSION_1 = SnmpConstants.version1;
   public static int VERSION_2C = SnmpConstants.version2c;
   public static int VERSION_3 = SnmpConstants.version3;
   int selectedSNMPVersion= ServerRequester.VERSION_2C;
    
   public void setSNMPVersion(int version){
      this.selectedSNMPVersion=version;
   }

    
    public ArrayList<String> sentRequest(String OID){
        return this.getWithWalk(new OID(OID));
    }
    
    
    /*This methof uses the snmpwalk method, therefore it can obtain more than one data*/
    public ArrayList<String> getWithWalk(OID oid){
      //  console(oid.toString());
       ArrayList<String> results=new ArrayList<String>();
       Utility util = new Utility(selectedSNMPVersion, IP);
      //  List<VariableBinding> vbs=util.walk(new OID("1.3.6.1.2.1.2.2.1.10"), "public");
       List<VariableBinding> vbs=util.walk(oid, "public");
         for (VariableBinding vb : vbs) {
            System.out.println(vb.getOid() + ":=  " + vb.getVariable().toString());
            results.add(vb.getVariable().toString());
            
        }
         return results;
    }
       public void console(String str) {
        System.out.println(str);
    }
    /*Through this method you can get the whole values of the given list*/
    
    public ArrayList<SNMPOIDCollections.SNMPOBJ> getList(ArrayList<SNMPOIDCollections.SNMPOBJ> oidList) {

        // ArrayList<SNMPOIDCollections.SNMPOBJ> results=new ArrayList<SNMPOIDCollections.SNMPOBJ>();

        ArrayList<SNMPOIDCollections.SNMPOBJ> results = oidList;

        Utility util = new Utility(selectedSNMPVersion, IP);
        List<VariableBinding> vbs = util.getList(new SNMPOIDCollections().getOIDList(oidList), "public");

        if (vbs.size() > 0) {

            for (int i = 0; i < vbs.size(); i++) {
                System.out.println(vbs.get(i).getOid() + ":=  " + vbs.get(i).getVariable().toString());
                results.get(i).value = vbs.get(i).getVariable().toString();

            }
        } else {
            System.out.println("CPU messages could not be received.");

        }
        return results;
    }

    
    public void runSNMPUtility() {
        Utility util = new Utility(selectedSNMPVersion, IP);
        List<OID> list=new ArrayList<OID>();
        list.add(new OID(".1.3.6.1.2.1.2.2.1.2"));
        List<VariableBinding> vbs=util.walk(new OID("1.3.6.1.2.1.2.2.1.10"), "public");
        
          for (VariableBinding vb : vbs) {
            System.out.println(vb.getOid() + ":=  " + vb.getVariable().toString());
        }
    }
    
       
      public class SNMPOIDCollections {

        public class SNMPOBJ {

            public OID oid;
            
            public String definition;
            public String value;

            public SNMPOBJ(OID oid, String def, String val) {
                this.oid = oid;
                this.definition = def;
                this.value = val;

            }
        }

        public SNMPOIDCollections() {
        }

        /*
         Memory
         Total Swap Size: .1.3.6.1.4.1.2021.4.3.0
         Available Swap Space: .1.3.6.1.4.1.2021.4.4.0
         Total RAM in machine: .1.3.6.1.4.1.2021.4.5.0
         Total RAM used: .1.3.6.1.4.1.2021.4.6.0
         Total RAM Free: .1.3.6.1.4.1.2021.4.11.0
         Total RAM Shared: .1.3.6.1.4.1.2021.4.13.0
         Total RAM Buffered: .1.3.6.1.4.1.2021.4.14.0
         Total Cached Memory: .1.3.6.1.4.1.2021.4.15.0
         */
        public ArrayList<SNMPOBJ> getMEMOIDs() {

            ArrayList<SNMPOBJ> list = new ArrayList<SNMPOBJ>();
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.4.3.0"), SNMPDEF.mem_totalSwapSize, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.4.4.0"), SNMPDEF.mem_availableSwapSpace, ""));

            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.4.5.0"), SNMPDEF.mem_totalRAMinMachine, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.4.6.0"), SNMPDEF.mem_totalUsedRAM, ""));

            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.4.11.0"), SNMPDEF.mem_totalFreeRAM, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.4.13.0"), SNMPDEF.mem_totalSharedRAM, ""));

            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.4.14.0"), SNMPDEF.mem_totalBufferedRAM, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.4.15.0"), SNMPDEF.mem_totalCachedMemory, ""));

            return list;
        }

        /*
         CPU
         1 minute Load: .1.3.6.1.4.1.2021.10.1.3.1
         5 minute Load: .1.3.6.1.4.1.2021.10.1.3.2
         15 minute Load: .1.3.6.1.4.1.2021.10.1.3.3

         percentage of user CPU time: .1.3.6.1.4.1.2021.11.9.0
         raw user cpu time: .1.3.6.1.4.1.2021.11.50.0
         percentages of system CPU time: .1.3.6.1.4.1.2021.11.10.0
         raw system cpu time: .1.3.6.1.4.1.2021.11.52.0
         percentages of idle CPU time: .1.3.6.1.4.1.2021.11.11.0
         raw idle cpu time: .1.3.6.1.4.1.2021.11.53.0
         raw nice cpu time: .1.3.6.1.4.1.2021.11.51.0
         */
        
        public ArrayList<SNMPOBJ> getCPUOIDs() {

            ArrayList<SNMPOBJ> list = new ArrayList<SNMPOBJ>();
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.10.1.3.1"), SNMPDEF.cpu_oneMinLoad, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.10.1.3.2"), SNMPDEF.cpu_fiveMinLoad, ""));

            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.10.1.3.3"), SNMPDEF.cpu_fifthenMinLoad, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.11.9.0"), SNMPDEF.cpu_userCPUTimePercentage, ""));

            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.11.50.0"), SNMPDEF.cpu_userRawCPUTime, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.11.10.0"), SNMPDEF.cpu_systemCPUTimePercentage, ""));

            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.11.52.0"), SNMPDEF.cpu_systemRawCPUTime, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.11.11.0"), SNMPDEF.cpu_idleCPUTimePercentage, ""));

            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.11.53.0"), SNMPDEF.cpu_idleRawCPUTime, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.4.1.2021.11.51.0"), SNMPDEF.cpu_niceRawCPUTime, ""));
            return list;
        }

        /*
         Network Interfaces and Bandwidth
         IP 
         IPIN/OUT Packets 
         IP-MIB::ipInDelivers.0 = Counter32: 97119        .1.3.6.1.2.1.4.9
         IP-MIB::ipOutRequests.0 = Counter32: 101800      .1.3.6.1.2.1.4.10

         IP Addresses

         .1.3.6.1.2.1.4 or .1.3.6.1.2.1.3.1
         ICM Packets
         .1.3.6.1.2.1.5
         UDP Packets
         .1.3.6.1.2.1.7
         TCP Packets
         .1.3.6.1.2.1.6

         SNMP Packets
         .1.3.6.1.2.1.11
         */
        
        
        public OID getOIDTree(){
            return new OID(".1.3.6");
        }
        
        //TODO: Add here also ip and mac address of the clients
        public ArrayList<SNMPOBJ> geProtocols_IP() {

            ArrayList<SNMPOBJ> list = new ArrayList<SNMPOBJ>();
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.4.3.0"), SNMPDEF.prot_ipInRequests, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.4.9.0"), SNMPDEF.prot_ipInDelivers, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.4.10.0"), SNMPDEF.prot_ipOutRequests, ""));
            
            return list;
        }

        public ArrayList<SNMPOBJ> geProtocols_UDP() {

            ArrayList<SNMPOBJ> list = new ArrayList<SNMPOBJ>();
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.7.1.0"), SNMPDEF.prot_udpInDatagram, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.7.4.0"), SNMPDEF.prot_udpOutDatagram, ""));
            return list;
        }

        public ArrayList<SNMPOBJ> geProtocols_TCP() {

            ArrayList<SNMPOBJ> list = new ArrayList<SNMPOBJ>();
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.6.10.0"), SNMPDEF.prot_tcpInSegment, ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.6.11.0"), SNMPDEF.prot_tcpOutSegment, ""));
            return list;
        }

        public ArrayList<SNMPOBJ> geProtocols_SNMP() {

            ArrayList<SNMPOBJ> list = new ArrayList<SNMPOBJ>();
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.4.9.0"), "", ""));
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.4.10.0"), "", ""));
            return list;

        }
        public ArrayList<SNMPOBJ> geProtocols_ICMP() {
            
            ArrayList<SNMPOBJ> list = new ArrayList<SNMPOBJ>();
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.5.1.0"), SNMPDEF.prot_icmpInPacket, ""));// icp in message
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.5.14.0"), SNMPDEF.prot_icmpOutPacket, "")); // icmp out messages
            return list;
        }
 
        /* Network Interfaces
         1.3.6.1.2.1.2.2.1.2	
         received bytes from these interfaces
         1.3.6.1.2.1.2.2.1.10
         transmitted bytes to these interfaces
         1.3.6.1.2.1.2.2.1.16
         */
        public OID getConnectedUsers(){
            return new OID(".1.3.6.1.2.1.3");
        }
        public OID getNetworkInterfaces() {
            return new OID(".1.3.6.1.2.1.2.2.1.2");
        }

        public OID getNetIntReceivedData() {
            return new OID(".1.3.6.1.2.1.2.2.1.10");
        }

        public OID getNetIntTransmittedData() {
            return new OID(".1.3.6.1.2.1.2.2.1.16");
        }

//        public OID getHOSTOIDs() {
//            
//            return new OID(".1.3.6.1.2.1.4.3");
//        }

        public ArrayList<SNMPOBJ> getHOSTOIDs() {
            
            ArrayList<SNMPOBJ> list = new ArrayList<SNMPOBJ>();
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.25.1.1.0"), SNMPDEF.host_sysUpTime, ""));// icp in message
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.25.1.2.0"), SNMPDEF.host_sysDate, ""));// icp in message
            list.add(new SNMPOBJ(new OID(".1.3.6.1.2.1.25.1.4.0"), SNMPDEF.host_info, ""));// icp in message

            return list;
        }
 
        public ArrayList<OID> getOIDList(ArrayList<SNMPOBJ> list) {
            ArrayList<OID> oidList = new ArrayList<OID>();
            for (int i = 0; i < list.size(); i++) {
                oidList.add(list.get(i).oid);
            }
            return oidList;
        }
        
        public class SNMPDEF {

            public final static String cpu_oneMinLoad = "One Minute CPU Load";
            public final static String cpu_fiveMinLoad = "Five Minute CPU Load";
            public final static String cpu_fifthenMinLoad="Fifthen Minute CPU Load";
            public final static String cpu_userCPUTimePercentage="Percentage of User CPU Time";
            public final static String cpu_userRawCPUTime="Raw User CPU Time";
            public final static String cpu_systemCPUTimePercentage="Percentage of System CPU Time";
            public final static String cpu_systemRawCPUTime="Raw System CPU Time";
            public final static String cpu_idleCPUTimePercentage="Percentage of Idle CPU Time";
            public final static String cpu_idleRawCPUTime="Raw Idle CPU Time";
            public final static String cpu_niceRawCPUTime="Raw Nice CPU Time";
            
            public final static String mem_totalSwapSize= "Total Swap Size";
            public final static String mem_availableSwapSpace= "Available Swap Space";
            public final static String mem_totalRAMinMachine= "Total RAM in Machine";
            public final static String mem_totalUsedRAM= "Total RAM used";
            public final static String mem_totalFreeRAM= "Total RAM free";
            public final static String mem_totalSharedRAM= "Total RAM Shared";
            public final static String mem_totalBufferedRAM= "Total RAM Buffered";
            public final static String mem_totalCachedMemory= "Total Cached Memory";
            
            public final static String prot_ipInRequests="IP In Received Packets";
            public final static String prot_ipInDelivers="IP In Delivered Packets";
            public final static String prot_ipOutRequests="IP Out Requested Packets";

            public final static String prot_icmpInPacket="ICMP Received Packets";
            public final static String prot_icmpOutPacket="ICMP Sent Packets";
            public final static String prot_tcpInSegment="TCP Received Segments";
            public final static String prot_tcpOutSegment="TCP Sent Segments";
            public final static String prot_udpInDatagram="UDP Received Datagrams";
            public final static String prot_udpOutDatagram="UDP Sent Datagrams";
            
            public final static String host_info="Host Information";
            public final static String host_sysUpTime="System Uptime";
            public final static String host_sysDate="System Date";

            //add also here snmp
                  
        }
    }
}
