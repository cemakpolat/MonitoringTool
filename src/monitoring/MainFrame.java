/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package monitoring;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import monitoring.ServerInterface.MACVPNIPTuple;
import monitoring.ServerInterface.UseViOSUser;
import org.jdesktop.xswingx.PromptSupport;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author cemakpolat
 */
public class MainFrame extends javax.swing.JFrame {

    /*
     private static SNMPFrame instance = null;
   
     public static SNMPFrame getInstance() {
     if(instance == null) {
     instance = new SNMPFrame();
     }
     return instance;
     }
     */
    public void resizeWindow() {
//      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//      setBounds(0,0,screenSize.width, screenSize.height);
//       setVisible(true);
//      
//        this.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                setSize(new Dimension(900, getHeight()));
//                super.componentResized(e);
//            }
//        });    
    }
    public final static int ALLENABLE = 1001;
    public final static int ALLDISABLE = 1000;
    ServerInterface snmpInterface = new ServerInterface();
    ServerInterface snmpInterface_public = new ServerInterface();
    public String defaultIPAddress_public = "192.168.127.1";
    public String defaultIPAddress = "192.168.126.1";
    public int graphRedrawDuration = 4000;
    public static String status_report = "";
    public String publicTerm="public";
    public String privateTerm="private";
    
    public DefaultListModel listModelForNI = new DefaultListModel();
    public DefaultListModel listModelForNI_public = new DefaultListModel();
    public DefaultListModel listModelForNI_public_usevios = new DefaultListModel();
    public DefaultListModel listModelForNI_public_mac_vpnip = new DefaultListModel();
    
    /*Thread cancelling: Once this parameter is set to true, the all threads running in each tab will contiously work,
     othwise they will be cancelled while passing from one tab to another tab. The essential goal here is to minimize the
     graph draw functions for each tab, hence consuming much few resource.
     */
    public boolean threadCancelling = false;
    public boolean threadCancelling_public = false;
    public boolean startButtonClicked = false;          // for hindering any action while changing tabs
    public boolean startButtonClicked_public = false;   // for hindering any action while changing tabs

    public void consoleOutputs() {
        jTextArea_console_outputs.setEditable(false);
        PrintStream printStream = new PrintStream(new CustomOutputStream(jTextArea_console_outputs));
        System.setOut(printStream);
        System.setErr(printStream);
        this.snmpInterface.setTextArea(this.jTextArea_privateOS);
        this.snmpInterface_public.setTextArea(this.jTextArea_publicOs);

    }

    public MainFrame() {
      //  setResizable(false);
   
        initComponents();
        this.consoleOutputs();
        this.prepareConfigParameters();
        this.prepareConfigParameters_public();

        this.threadCancelling = true;
        this.threadCancelling_public = true;
        
        this.jComboBox_iperf_bw.setEnabled(false);
        this.jComboBox_iperf_bw1.setEnabled(false);
        
        this.snmpInterface.setTerm(this.privateTerm);
        this.snmpInterface_public.setTerm(this.publicTerm);
        
        
        //Control Interface
         Thread controlInterface = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    console("Current users data are being fetched from server...");
                    controlInteface();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        controlInterface.start();
        
        
     
        
    
    }
    

    public void writeStatusReport(String str) {
        synchronized (MainFrame.jLabel_status_report) {
            MainFrame.jLabel_status_report.setText(str);
        }
    }
    
    public void loadUsers(){
    
        //Load default user configurations
        this.snmpInterface_public.loadDefaultUsers();
        
        listModelForNI_public_usevios.clear();
        listModelForNI_public_mac_vpnip.clear();
        
        for (int i = 0; i <   this.snmpInterface_public.usevoisUsers.size(); i++) {
             console("Control Interface:"+ this.snmpInterface_public.usevoisUsers.get(i).userName.toString());
            listModelForNI_public_usevios.addElement(this.snmpInterface_public.usevoisUsers.get(i).userName);
        }
        this.jList_ci_usevios_enabled_users.setModel(listModelForNI_public_usevios);

        for (int i = 0; i <   this.snmpInterface_public.macVPNIPUserList.size(); i++) {
             console("Control Interface 2:"+ this.snmpInterface_public.macVPNIPUserList.get(i).MAC.toString());
            listModelForNI_public_mac_vpnip.addElement(this.snmpInterface_public.macVPNIPUserList.get(i).MAC+"-"+this.snmpInterface_public.macVPNIPUserList.get(i).VPNIP);
        }        
        this.jList_ci_usevios_mac_vpnip.setModel(this.listModelForNI_public_mac_vpnip);
    }

    public void controlInteface(){
        PromptSupport.setPrompt("userID,MAC", jTextField_userID_control_interface);
        PromptSupport.setPrompt("MAC", this.jTextField_control_interface_MAC);
        PromptSupport.setPrompt("VPN IP", this.jTextField_control_interface_vpnip);
        jScrollPane7.setBorder(null);
        
        loadUsers();
    }

    
    //PUBLIC OS
    public void startManageComponents_public() {
        console("public START MANAGEMENT");
        this.manage_cpu_mem_public();
        this.manage_network_interface_public();
        this.manage_protocols_public();
        this.manage_host_info_public();
    }

    public void startComponents_public() {
        
        this.startButtonClicked_public = true;
        this.disableTabDataTraffic_public(ALLENABLE);
        this.snmpInterface_public.startSNMPRequester();

        if (threadCancelling_public) {
            console("Thread cancelling mode is enabled, all threads will be working without interruption");
            this.startManageComponents_public();
        } else {
             console("Started!!!!");
            this.disableTabDataTraffic_public(ALLDISABLE);
        }
    }

    public void stopComponents_public() {
        this.startButtonClicked_public = false;//CHANGE
        this.snmpInterface_public.stopSNMPRequester();
        this.disableTabDataTraffic_public(ALLDISABLE);
    }
    
    public boolean state_mamange_cpu_mem_public = true;
    public void manage_cpu_mem_public() {
        console("public START MANAGEMENT 2" );
        Thread cpuGraph = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    console("CPU and MEM Management Object is started");
                    while (state_mamange_cpu_mem_public) {
                        drawMemChange_public();
                        drawCPUChange_public();
                        fillCPUDataInTextLabels_public();
                        fillMemDataInTextLabels_public();
                        Thread.sleep(graphRedrawDuration);
                    }
                    console("CPU and MEM management object is stopped");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cpuGraph.start();
    }

    public void fillCPUDataInTextLabels_public() {
        ServerInterface.CPUObject obj = null;
        if (snmpInterface_public.cpuInfo.size() > 0) {
            obj = snmpInterface_public.cpuInfo.get(snmpInterface_public.cpuInfo.size() - 1);
            if (obj != null) {
                this.jbl_cpu_15min_load_val1.setText(obj.fifthenMinLoad);
                this.jbl_cpu_1min_load_val1.setText(obj.oneMinLoad);
                this.jbl_cpu_5min_load_val1.setText(obj.fiveMinLoad);
                this.jbl_cpu_per_user_cpu_val1.setText(obj.userCPUTimePercentage);
                this.jbl_cpu_per_idle_cpu_val1.setText(obj.idleCPUTimePercentage);
                this.jbl_cpu_per_sys_cpu_val1.setText(obj.systemCPUTimePercentage);
                this.jbl_cpu_raw_user_cpu_val1.setText(obj.userRawCPUTime);
                this.jbl_cpu_raw_idle_cpu_val1.setText(obj.idleRawCPUTime);
                this.jbl_cpu_raw_nice_cpu_val1.setText(obj.niceRawCPUTime);
            }
        }
    }

    public void drawCPUChange_public() {

        console("CPU Data Change is being drawn");
        try {
            jPanel9_cpu_graph1.removeAll();

            XYSeries team1_xy_data_1 = new XYSeries("CPU 1 min Load");
            XYSeries team1_xy_data_2 = new XYSeries("CPU 5 min Load");
            XYSeries team1_xy_data_3 = new XYSeries("CPU 15 min Load");

            for (int i = 0; i < snmpInterface_public.cpuInfo.size(); i++) {
                if ((!snmpInterface_public.cpuInfo.get(i).oneMinLoad.equalsIgnoreCase("") && !snmpInterface_public.cpuInfo.get(i).fiveMinLoad.equalsIgnoreCase("")
                        && !snmpInterface_public.cpuInfo.get(i).fifthenMinLoad.equalsIgnoreCase("")) && !snmpInterface_public.cpuInfo.get(i).fifthenMinLoad.equalsIgnoreCase("noSuchObject")
                         && !snmpInterface_public.cpuInfo.get(i).fifthenMinLoad.equalsIgnoreCase("Null")) {
                    team1_xy_data_1.add(i, Double.parseDouble(snmpInterface_public.cpuInfo.get(i).oneMinLoad));
                    team1_xy_data_2.add(i, Double.parseDouble(snmpInterface_public.cpuInfo.get(i).fiveMinLoad));
                    team1_xy_data_3.add(i, Double.parseDouble(snmpInterface_public.cpuInfo.get(i).fifthenMinLoad));
                } else {
                    team1_xy_data_1.add(i, 0);
                    team1_xy_data_2.add(i, 0);
                    team1_xy_data_3.add(i, 0);
                }
            }


            /* Add all XYSeries to XYSeriesCollection */
            //XYSeriesCollection implements XYDataset
            XYSeriesCollection my_data_series = new XYSeriesCollection();
            // add series using addSeries method
            my_data_series.addSeries(team1_xy_data_1);
            my_data_series.addSeries(team1_xy_data_2);
            my_data_series.addSeries(team1_xy_data_3);

            XYLineAndShapeRenderer dot = new XYLineAndShapeRenderer();
            NumberAxis xax = new NumberAxis("Measurements");
            NumberAxis yax = new NumberAxis("CPU Load in s");
            XYPlot plot = new XYPlot(my_data_series, xax, yax, dot);
            //Use createXYLineChart to create the chart
            JFreeChart chart2 = new JFreeChart(plot);



            jPanel9_cpu_graph1.setLayout(new java.awt.BorderLayout());
            ChartPanel CP = new ChartPanel(chart2);
            jPanel9_cpu_graph1.add(CP, BorderLayout.CENTER);
            // jPanel1.updateUI();
            jPanel9_cpu_graph1.validate();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void fillMemDataInTextLabels_public() {
        console("Memory Datas are assigned to the relevant labels");
        ServerInterface.MemObject obj = null;
        if (snmpInterface_public.memInfo.size() > 0) {
            obj = snmpInterface_public.memInfo.get(snmpInterface_public.memInfo.size() - 1);
            if (obj != null) {
                this.jbl_mem_avai_swap_space_val1.setText(obj.availableSwapSpace);
                this.jbl_mem_total_cached_mem_val1.setText(obj.totalCachedMemory);
                this.jbl_mem_total_ram_buffered_val1.setText(obj.totalBufferedRAM);
                this.jbl_mem_total_ram_free_val1.setText(obj.totalFreeRAM);
                this.jbl_mem_total_ram_shared_val1.setText(obj.totalSharedRAM);
                this.jbl_mem_total_ram_used_val1.setText(obj.totalUsedRAM);
                this.jbl_mem_total_ram_val1.setText(obj.totalRAMinMachine);
                this.jbl_mem_total_swap_size_val1.setText(obj.totalSwapSize);
            }
        }

    }

    public void drawMemChange_public() {
        console("Memory Change is being drawn");
        try {
            jPanel9_mem_graph1.removeAll();

            XYSeries team1_xy_data_1 = new XYSeries("Total Used Ram");
            XYSeries team1_xy_data_2 = new XYSeries("Total Free Ram");
            //  XYSeries team1_xy_data_3 = new XYSeries("Total Cached Memory");

            if (snmpInterface_public.memInfo.size() > 0) {
                for (int i = 0; i < snmpInterface_public.memInfo.size(); i++) {
                    if (!snmpInterface_public.memInfo.get(i).totalUsedRAM.equalsIgnoreCase("") && !snmpInterface_public.memInfo.get(i).totalUsedRAM.equalsIgnoreCase("noSuchObject")
                            && !snmpInterface_public.memInfo.get(i).totalUsedRAM.equalsIgnoreCase("Null")) {
                        team1_xy_data_1.add(i, Integer.parseInt(snmpInterface_public.memInfo.get(i).totalUsedRAM));
                        team1_xy_data_2.add(i, Integer.parseInt(snmpInterface_public.memInfo.get(i).totalFreeRAM));
                    } else {
                        team1_xy_data_1.add(i, 0);
                        team1_xy_data_2.add(i, 0);
                    }
                }


                /* Add all XYSeries to XYSeriesCollection */
                //XYSeriesCollection implements XYDataset
                XYSeriesCollection my_data_series = new XYSeriesCollection();
                // add series using addSeries method
                my_data_series.addSeries(team1_xy_data_1);
                my_data_series.addSeries(team1_xy_data_2);
                //        my_data_series.addSeries(team1_xy_data_3);

                XYLineAndShapeRenderer dot = new XYLineAndShapeRenderer();
                NumberAxis xax = new NumberAxis("Measurements");
                NumberAxis yax = new NumberAxis("RAM Usage in Byte");
                XYPlot plot = new XYPlot(my_data_series, xax, yax, dot);
                //Use createXYLineChart to create the chart
                JFreeChart chart = new JFreeChart(plot);

                jPanel9_mem_graph1.setLayout(new java.awt.BorderLayout());
                ChartPanel CP = new ChartPanel(chart);
                jPanel9_mem_graph1.add(CP, BorderLayout.CENTER);
                // jPanel1.updateUI();
                jPanel9_mem_graph1.validate();

            } else {
                console("There is any element in the memory list");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    // call firstly 
    public boolean firstSelectedNetInt_public = true;
    public boolean state_manage_network_interface_public = true;

    public void manage_network_interface_public() {
        console("manange_networ_interfaces");
        Thread netInfoFiller = new Thread(new Runnable() {
            @Override
            public void run() {
                console("Network Interfaces Management Object is started");
                state_network_graph_public = true;
                drawNetworkChange_public();
                while (state_manage_network_interface_public) {
                    try {
                        fillNetIntTableInTextLabels_public();
                        Thread.sleep(graphRedrawDuration);
                    } catch (Exception e) {
                        //SNMPFrame.class.console(e.getMessage());
                    }
                }
                console("Network Interfaces Management object is stopped!");
                state_network_graph_public = false; // TODO: The place of that could be changed.
            }
        });
        netInfoFiller.start();

    }

    private void fillNIList_public(ArrayList<String> iface) {
        listModelForNI_public.clear();
        for (int i = 0; i < iface.size(); i++) {
            // console("NI:"+ iface.get(i).toString());
            listModelForNI_public.addElement(iface.get(i));
            //jTable1.getModel().setValueAt(val, row, column);
            //jTable1.getModel().setValueAt(0, 0, 0);
            //jTable1.getModel().setValueAt(5, 0, 1);
            //jTable1.getModel().setValueAt(0, 3, 2);
        }
        jList1_network_interfaces1.setModel(listModelForNI_public);
        jList1_network_interfaces1.setSelectedIndex(0);
    }
    public boolean state_public = true;

    public void fillNetIntTableInTextLabels_public() {
        try {
            jTable2.removeAll();
            if (snmpInterface_public.netInfo.size() > 0) {
                ArrayList<String> iface = new ArrayList<String>();
                ArrayList<String> recv = new ArrayList<String>();
                ArrayList<String> trans = new ArrayList<String>();

                iface.addAll(snmpInterface_public.netInfo.get(snmpInterface_public.netInfo.size() - 1).interfaces);
                recv.addAll(snmpInterface_public.netInfo.get(snmpInterface_public.netInfo.size() - 1).received);
                trans.addAll(snmpInterface_public.netInfo.get(snmpInterface_public.netInfo.size() - 1).transmitted);

                int col = 0;
                for (int i = 0; i < iface.size(); i++) {

                    jTable2.getModel().setValueAt(iface.get(i), i, col);
                    jTable2.getModel().setValueAt(recv.get(i), i, col + 1);
                    jTable2.getModel().setValueAt(trans.get(i), i, col + 2);
                }
                if (listModelForNI_public.getSize() != iface.size()) {
                    fillNIList_public(iface); //first time is required!
                }

            } else {
                console("The list related to the network has any elements");
            }

        } catch (Exception e) {
        }

    }
    //TODO: change this function in a way that enables updating easily the graph while selecting other interfaces.
    public boolean state_network_graph_public = true;

    public void drawNetworkChange_public() {
        Thread thread;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                console("Network Interface Graph Thread is started");
                while (state_network_graph_public) {
                    try {
                        jPanel15_selcted_nic_graph1.removeAll();
                        int selectedInterface = jList1_network_interfaces1.getSelectedIndex();
                        // console("Selected index: " + selectedInterface);
                        if (selectedInterface >= 0) {
                            XYSeries team1_xy_data_1 = new XYSeries("Received Data in Bytes/s");
                            XYSeries team1_xy_data_2 = new XYSeries("Transmitted Data in Bytes/s");

                            for (int i = 0; i < snmpInterface_public.netInfo.size(); i++) {
                                // console("Received " + snmpInterface.netInfo.get(i).received.get(selectedInterface));
                                team1_xy_data_1.add(i, Long.parseLong(snmpInterface_public.netInfo.get(i).received.get(selectedInterface)));
                                team1_xy_data_2.add(i, Long.parseLong(snmpInterface_public.netInfo.get(i).transmitted.get(selectedInterface)));

                            }

                            /* Add all XYSeries to XYSeriesCollection */
                            //XYSeriesCollection implements XYDataset
                            XYSeriesCollection my_data_series = new XYSeriesCollection();
                            // add series using addSeries method
                            my_data_series.addSeries(team1_xy_data_1);
                            my_data_series.addSeries(team1_xy_data_2);

                            XYLineAndShapeRenderer dot = new XYLineAndShapeRenderer();
                            NumberAxis xax = new NumberAxis("Measurements");
                            NumberAxis yax = new NumberAxis("NetInt. Byte in s");
                            XYPlot plot = new XYPlot(my_data_series, xax, yax, dot);


                            //Use createXYLineChart to create the chart
                            JFreeChart chart2 = new JFreeChart(plot);

                            //JFreeChart XYLineChart = ChartFactory.createXYLineChart("Team - Number of Wins", "Year", "Win Count", my_data_series, PlotOrientation.VERTICAL, true, true, false);

                            jPanel15_selcted_nic_graph1.setLayout(new java.awt.BorderLayout());


                            ChartPanel CP = new ChartPanel(chart2);
                            //CP.setSize(440, 270);
                            CP.setMaximumDrawHeight(400);
                            CP.setMaximumSize(new Dimension(440, 270));
                            jPanel15_selcted_nic_graph1.add(CP, BorderLayout.CENTER);

                            // jPanel1.updateUI();
                            jPanel15_selcted_nic_graph1.validate();
                            sleep(2000);
                        }

                    } catch (Exception e) {
                    }
                }
                console("Network Graph Thread is cancelled");
            }
        });
        thread.start();
    }

    public void fillConnectedUsersTable_public() {
        try {

            DefaultTableModel tableModel = (DefaultTableModel) jTable_connected_users1.getModel();
            tableModel.setRowCount(0);
            this.snmpInterface_public.updateUserList();

            tableModel.setRowCount(this.snmpInterface_public.tempUserList.size());
            console("\n PUBLIC List size users"+this.snmpInterface_public.tempUserList.size());
           // jTable_connected_users1.removeAll();
            if (snmpInterface_public.users.size() > 0) {
                int col = 0;
                for (int i = 0; i < snmpInterface_public.tempUserList.size(); i++) {
                    this.jTable_connected_users1.getModel().setValueAt(i, i, col);
                    
                    jTable_connected_users1.getModel().setValueAt(snmpInterface_public.tempUserList.get(i).ip, i, col + 1);
                    jTable_connected_users1.getModel().setValueAt(snmpInterface_public.tempUserList.get(i).mac, i, col + 2);
                }

            } else {
                console("\nAny connected user is detected!\n");
            }

            } catch (Exception e) {
        }
    }
    
    public boolean state_manage_protocols_public = true;
    public void manage_protocols_public() {
        Thread protocols = new Thread(new Runnable() {
            @Override
            public void run() {
                console("Protocol Management Object is started");
                while (state_manage_protocols_public) {
                    ServerInterface.ProtocolObject obj = null;
                    fillConnectedUsersTable_public(); //Connec ted Users
                    if (snmpInterface_public.protocolsInfo.size() > 0) {
                        obj = snmpInterface_public.protocolsInfo.get(snmpInterface_public.protocolsInfo.size() - 1);
                        if (obj != null) {

                            fillProtocol_IPDataInTextLabels_public(obj);
                            fillProtocol_ICMPDataInTextLabels_public(obj);
                            fillProtocol_UDPDataInTextLabels_public(obj);
                            fillProtocol_TCPDataInTextLabels_public(obj);
                            //  fillProtocol_SNMPDataInTextLabels(obj);
                        } else {
                            console("Object is nul1!");
                        }
                    } else {
                        console("No elements in the protocol object");
                    }
                    try {
                        Thread.sleep(graphRedrawDuration);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                console("Protocol Management Object is stopped");
            }
        });
        protocols.start();
    }

    public void fillProtocol_UDPDataInTextLabels_public(ServerInterface.ProtocolObject obj) {
        this.jLabel_in_udp_data_val1.setText(obj.udpInDatagram);
        this.jLabel_out_udp_data_val1.setText(obj.udpOutDatagram);
    }

    public void fillProtocol_ICMPDataInTextLabels_public(ServerInterface.ProtocolObject obj) {
        this.jLabel_in_icmp_mes_val1.setText(obj.icmpInPacket);
        this.jLabel_out_icmp_mes_val1.setText(obj.icmpOutPacket);
    }

    public void fillProtocol_IPDataInTextLabels_public(ServerInterface.ProtocolObject obj) {
        this.jLabel_ip_delivered_val1.setText(obj.ipInDelivers);
        this.jLabel_ip_received_val1.setText(obj.ipInRequests);
        this.jLabel_ip_requested_val1.setText(obj.ipOutRequests);
    }

    public void fillProtocol_TCPDataInTextLabels_public(ServerInterface.ProtocolObject obj) {
        this.jLabel_in_tcp_seg_val1.setText(obj.tcpInSegment);
        this.jLabel_out_tcp_seg_val1.setText(obj.tcpOutSegment);

    }

    public void fillProtocol_SNMPDataInTextLabels_public(ServerInterface.ProtocolObject obj) {
        //this.console("These protocol values haven't been yet implemented");
    }
    //TODO: More data should be added to there.
    public boolean state_host_info_public = true;

    public void manage_host_info_public() {
        Thread hostInfo = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    console("Host Management Object is started");
                    while (state_host_info_public) {
                        Thread.sleep(2000);
                        // console("Date"+snmpInterface.host.sysDate);
                        jLabel_sys_date_val1.setText(snmpInterface_public.host.sysDate);
                        jLabel_sys_uptime_val1.setText(snmpInterface_public.host.sysUpTime);
                        //jTextPane_sys_init_param_val1.setText(snmpInterface_public.host.info);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                console("Host Management Object is stopped");
            }
        });
        hostInfo.start();
    }

    /**
     *
     */
    public void prepareConfigParameters_public() {

        this.jTextField_ipaddress_val1.setText(defaultIPAddress_public);
        this.snmpInterface_public.setSNMPRequesterIP(defaultIPAddress_public);
        this.buttonGroup2.clearSelection();
        this.buttonGroup2.setSelected(this.jRadioButton_snmpv1_publicOS.getModel(), true);//CHANGE

    }

    public void disableAllTabs_public() {
        console("All interfaces are deactivated.");
        this.state_mamange_cpu_mem_public = false;
        this.state_manage_network_interface_public = false;
        this.state_manage_protocols_public = false;
        this.state_host_info_public = false;

        //Additional for graph
        state_network_graph_public = false;
    }

    public void enableAllTabs_public() {
        console("All interfaces are activated");
        this.state_mamange_cpu_mem_public = true;
        this.state_manage_network_interface_public = true;
        this.state_manage_protocols_public = true;
        this.state_host_info_public = true;
        //Additional graph
        state_network_graph_public = true;
    }

    private void disableTabDataTraffic_public(int selectedTab) {
        // console("Selected Tab"+ selectedTab);
        switch (selectedTab) {
            case 0:
                console("CPU and MEM are activated.");
                if (this.state_mamange_cpu_mem_public != true) {
                    this.state_mamange_cpu_mem_public = true;
                    this.manage_cpu_mem_public();

                }
                this.state_manage_network_interface_public = false;
                this.state_manage_protocols_public = false;
                this.state_host_info_public = false;

                //Additional for graph
                state_network_graph_public = false;

                break;

            case 1:
                console("Network Interface is activated.");
                if (state_manage_network_interface_public != true) {
                    this.state_manage_network_interface_public = true;
                    this.manage_network_interface_public();
                }
                this.state_mamange_cpu_mem_public = false;
                this.state_manage_protocols_public = false;
                this.state_host_info_public = false;

                break;
            case 2:
                console("Protocol is activated.");
                if (this.state_manage_protocols_public != true) {
                    this.state_manage_protocols_public = true;
                    this.manage_protocols_public();
                }
                this.state_host_info_public = false;
                this.state_mamange_cpu_mem_public = false;
                this.state_manage_network_interface_public = false;
                //Additional for graph
                state_network_graph_public = false;
                break;
            case 3:
                console("Host is activated.");
                if (this.state_host_info_public != true) {
                    this.state_host_info_public = true;
                    this.manage_host_info_public();
                }
                this.state_manage_protocols_public = false;
                this.state_mamange_cpu_mem_public = false;
                this.state_manage_network_interface_public = false;
                //Additional 
                state_network_graph_public = false;
                break;

            case 4:
                disableAllTabs_public();
                break;
            case 5:
                disableAllTabs_public();
                break;
            case ALLENABLE:
                enableAllTabs_public();
                break;
            case ALLDISABLE:
                disableAllTabs_public();
                break;
            default:
                console("The selected tab number is unknown!");
                break;
        };

    }

    ////////////    PRIVATE OS   /////////////
    public void startManageComponents() {
        this.manage_cpu_mem();
        this.manage_network_interface();
        this.manage_protocols();
        this.manage_host_info();
    }

    public void startComponents() {
        this.startButtonClicked = true;
        this.disableTabDataTraffic(ALLENABLE);
        this.snmpInterface.startSNMPRequester();

        if (threadCancelling) {
            console("Thread cancelling mode is enabled, all threads will be working without interruption");
            this.startManageComponents();
        } else {
            this.disableTabDataTraffic(ALLDISABLE);
        }
    }

    public void stopComponents() {
        this.startButtonClicked = false;
        this.snmpInterface.stopSNMPRequester();
        this.disableTabDataTraffic(ALLDISABLE);
    }

    public void sleep(long second) {
        try {
            Thread.sleep(second);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean state_mamange_cpu_mem = true;

    public void manage_cpu_mem() {
        Thread cpuGraph = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    console("CPU and MEM Management Object is started");
                    while (state_mamange_cpu_mem) {
                        drawMemChange();
                        drawCPUChange();
                        fillCPUDataInTextLabels();
                        fillMemDataInTextLabels();
                        Thread.sleep(graphRedrawDuration);
                    }
                    console("CPU and MEM management object is stopped");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cpuGraph.start();
    }

    public void fillCPUDataInTextLabels() {
        ServerInterface.CPUObject obj = null;
        if (snmpInterface.cpuInfo.size() > 0) {
            obj = snmpInterface.cpuInfo.get(snmpInterface.cpuInfo.size() - 1);
            if (obj != null) {
                this.jbl_cpu_15min_load_val.setText(obj.fifthenMinLoad);
                this.jbl_cpu_1min_load_val.setText(obj.oneMinLoad);
                this.jbl_cpu_5min_load_val.setText(obj.fiveMinLoad);
                this.jbl_cpu_per_user_cpu_val.setText(obj.userCPUTimePercentage);
                this.jbl_cpu_per_idle_cpu_val.setText(obj.idleCPUTimePercentage);
                this.jbl_cpu_per_sys_cpu_val.setText(obj.systemCPUTimePercentage);
                this.jbl_cpu_raw_user_cpu_val.setText(obj.userRawCPUTime);
                this.jbl_cpu_raw_idle_cpu_val.setText(obj.idleRawCPUTime);
                this.jbl_cpu_raw_nice_cpu_val.setText(obj.niceRawCPUTime);
            }
        }
    }

    public void drawCPUChange() {

        console("CPU Data Change is being drawn");
        try {
            jPanel9_cpu_graph.removeAll();

            XYSeries team1_xy_data_1 = new XYSeries("CPU 1 min Load");
            XYSeries team1_xy_data_2 = new XYSeries("CPU 5 min Load");
            XYSeries team1_xy_data_3 = new XYSeries("CPU 15 min Load");

            for (int i = 0; i < snmpInterface.cpuInfo.size(); i++) {
                if (!snmpInterface.cpuInfo.get(i).oneMinLoad.equalsIgnoreCase("") && !snmpInterface.cpuInfo.get(i).fiveMinLoad.equalsIgnoreCase("")
                        && !snmpInterface.cpuInfo.get(i).fifthenMinLoad.equalsIgnoreCase("") &&  !snmpInterface.cpuInfo.get(i).fifthenMinLoad.equalsIgnoreCase("noSuchObject")
                         &&  !snmpInterface.cpuInfo.get(i).fifthenMinLoad.equalsIgnoreCase("Null")) {
                    team1_xy_data_1.add(i, Double.parseDouble(snmpInterface.cpuInfo.get(i).oneMinLoad));
                    team1_xy_data_2.add(i, Double.parseDouble(snmpInterface.cpuInfo.get(i).fiveMinLoad));
                    team1_xy_data_3.add(i, Double.parseDouble(snmpInterface.cpuInfo.get(i).fifthenMinLoad));
                } else {
                    team1_xy_data_1.add(i, 0);
                    team1_xy_data_2.add(i, 0);
                    team1_xy_data_3.add(i, 0);
                }
            }


            /* Add all XYSeries to XYSeriesCollection */
            //XYSeriesCollection implements XYDataset
            XYSeriesCollection my_data_series = new XYSeriesCollection();
            // add series using addSeries method
            my_data_series.addSeries(team1_xy_data_1);
            my_data_series.addSeries(team1_xy_data_2);
            my_data_series.addSeries(team1_xy_data_3);

            XYLineAndShapeRenderer dot = new XYLineAndShapeRenderer();
            NumberAxis xax = new NumberAxis("Measurements");
            NumberAxis yax = new NumberAxis("CPU Load in s");
            XYPlot plot = new XYPlot(my_data_series, xax, yax, dot);
            //Use createXYLineChart to create the chart
            JFreeChart chart2 = new JFreeChart(plot);



            jPanel9_cpu_graph.setLayout(new java.awt.BorderLayout());
            ChartPanel CP = new ChartPanel(chart2);
            jPanel9_cpu_graph.add(CP, BorderLayout.CENTER);
            // jPanel1.updateUI();
            jPanel9_cpu_graph.validate();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void fillMemDataInTextLabels() {
        console("Memory Datas are assigned to the relevant labels");
        ServerInterface.MemObject obj = null;
        if (snmpInterface.memInfo.size() > 0) {
            obj = snmpInterface.memInfo.get(snmpInterface.memInfo.size() - 1);
            if (obj != null) {
                this.jbl_mem_avai_swap_space_val.setText(obj.availableSwapSpace);
                this.jbl_mem_total_cached_mem_val.setText(obj.totalCachedMemory);
                this.jbl_mem_total_ram_buffered_val.setText(obj.totalBufferedRAM);
                this.jbl_mem_total_ram_free_val.setText(obj.totalFreeRAM);
                this.jbl_mem_total_ram_shared_val.setText(obj.totalSharedRAM);
                this.jbl_mem_total_ram_used_val.setText(obj.totalUsedRAM);
                this.jbl_mem_total_ram_val.setText(obj.totalRAMinMachine);
                this.jbl_mem_total_swap_size_val.setText(obj.totalSwapSize);
            }
        }

    }

    public void drawMemChange() {
        console("Memory Change is being drawn");
        try {
            jPanel9_mem_graph.removeAll();

            XYSeries team1_xy_data_1 = new XYSeries("Total Used Ram");
            XYSeries team1_xy_data_2 = new XYSeries("Total Free Ram");
            //  XYSeries team1_xy_data_3 = new XYSeries("Total Cached Memory");

            if (snmpInterface.memInfo.size() > 0) {
                for (int i = 0; i < snmpInterface.memInfo.size(); i++) {
                    if (!snmpInterface.memInfo.get(i).totalUsedRAM.equalsIgnoreCase("") && !snmpInterface.memInfo.get(i).totalUsedRAM.equalsIgnoreCase("noSuchObject")
                            && !snmpInterface.memInfo.get(i).totalUsedRAM.equalsIgnoreCase("Null")) {
                        team1_xy_data_1.add(i, Integer.parseInt(snmpInterface.memInfo.get(i).totalUsedRAM));
                        team1_xy_data_2.add(i, Integer.parseInt(snmpInterface.memInfo.get(i).totalFreeRAM));
                    } else {
                        team1_xy_data_1.add(i, 0);
                        team1_xy_data_2.add(i, 0);
                    }
                }


                /* Add all XYSeries to XYSeriesCollection */
                //XYSeriesCollection implements XYDataset
                XYSeriesCollection my_data_series = new XYSeriesCollection();
                // add series using addSeries method
                my_data_series.addSeries(team1_xy_data_1);
                my_data_series.addSeries(team1_xy_data_2);
                //        my_data_series.addSeries(team1_xy_data_3);

                XYLineAndShapeRenderer dot = new XYLineAndShapeRenderer();
                NumberAxis xax = new NumberAxis("Measurements");
                NumberAxis yax = new NumberAxis("RAM Usage in Byte");
                XYPlot plot = new XYPlot(my_data_series, xax, yax, dot);
                //Use createXYLineChart to create the chart
                JFreeChart chart = new JFreeChart(plot);

                jPanel9_mem_graph.setLayout(new java.awt.BorderLayout());
                ChartPanel CP = new ChartPanel(chart);
                jPanel9_mem_graph.add(CP, BorderLayout.CENTER);
                // jPanel1.updateUI();
                jPanel9_mem_graph.validate();

            } else {
                console("There is any element in the memory list");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    // call firstly 
    public boolean firstSelectedNetInt = true;
    public boolean state_manage_network_interface = true;

    public void manage_network_interface() {
        console("manange_networ_interfaces");
        Thread netInfoFiller = new Thread(new Runnable() {
            @Override
            public void run() {
                console("Network Interfaces Management Object is started");
                state_network_graph = true;
                drawNetworkChange();
                while (state_manage_network_interface) {
                    try {
                        fillNetIntTableInTextLabels();
                        Thread.sleep(graphRedrawDuration);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                console("Network Interfaces Management object is stopped!");
                state_network_graph = false; // TODO: The place of that could be changed.
            }
        });
        netInfoFiller.start();

    }

    private void fillNIList(ArrayList<String> iface) {
        listModelForNI.clear();
        for (int i = 0; i < iface.size(); i++) {
            // console("NI:"+ iface.get(i).toString());
            listModelForNI.addElement(iface.get(i));
            //jTable1.getModel().setValueAt(val, row, column);
            //jTable1.getModel().setValueAt(0, 0, 0);
            //jTable1.getModel().setValueAt(5, 0, 1);
            //jTable1.getModel().setValueAt(0, 3, 2);
        }
        jList1_network_interfaces.setModel(listModelForNI);
        jList1_network_interfaces.setSelectedIndex(0);
    }
    public boolean state = true;

    public void fillNetIntTableInTextLabels() {
        try {
            jTable1.removeAll();
            if (snmpInterface.netInfo.size() > 0) {
                ArrayList<String> iface = new ArrayList<String>();
                ArrayList<String> recv = new ArrayList<String>();
                ArrayList<String> trans = new ArrayList<String>();

                iface.addAll(snmpInterface.netInfo.get(snmpInterface.netInfo.size() - 1).interfaces);
                recv.addAll(snmpInterface.netInfo.get(snmpInterface.netInfo.size() - 1).received);
                trans.addAll(snmpInterface.netInfo.get(snmpInterface.netInfo.size() - 1).transmitted);

                int col = 0;
                for (int i = 0; i < iface.size(); i++) {

                    jTable1.getModel().setValueAt(iface.get(i), i, col);
                    jTable1.getModel().setValueAt(recv.get(i), i, col + 1);
                    jTable1.getModel().setValueAt(trans.get(i), i, col + 2);
                }
                if (listModelForNI.getSize() != iface.size()) {
                    fillNIList(iface); //first time is required!
                }

            } else {
                console("The list related to the network has any elements");
            }

        } catch (Exception e) {
        }

    }
    //TODO: change this function in a way that enables updating easily the graph while selecting other interfaces.
    public boolean state_network_graph = true;

    public void drawNetworkChange() {
        Thread thread;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                console("Network Interface Graph Thread is started");
                while (state_network_graph) {
                    try {
                        jPanel15_selcted_nic_graph.removeAll();
                        int selectedInterface = jList1_network_interfaces.getSelectedIndex();
                        // console("Selected index: " + selectedInterface);
                        if (selectedInterface >= 0) {
                            XYSeries team1_xy_data_1 = new XYSeries("Received Data in Bytes/s");
                            XYSeries team1_xy_data_2 = new XYSeries("Transmitted Data in Bytes/s");

                            for (int i = 0; i < snmpInterface.netInfo.size(); i++) {
                                // console("Received " + snmpInterface.netInfo.get(i).received.get(selectedInterface));
                                team1_xy_data_1.add(i, Long.parseLong(snmpInterface.netInfo.get(i).received.get(selectedInterface)));
                                team1_xy_data_2.add(i, Long.parseLong(snmpInterface.netInfo.get(i).transmitted.get(selectedInterface)));

                            }

                            /* Add all XYSeries to XYSeriesCollection */
                            //XYSeriesCollection implements XYDataset
                            XYSeriesCollection my_data_series = new XYSeriesCollection();
                            // add series using addSeries method
                            my_data_series.addSeries(team1_xy_data_1);
                            my_data_series.addSeries(team1_xy_data_2);

                            XYLineAndShapeRenderer dot = new XYLineAndShapeRenderer();
                            NumberAxis xax = new NumberAxis("Measurements");
                            NumberAxis yax = new NumberAxis("NetInt. Byte in s");
                            XYPlot plot = new XYPlot(my_data_series, xax, yax, dot);


                            //Use createXYLineChart to create the chart
                            JFreeChart chart2 = new JFreeChart(plot);

                            //JFreeChart XYLineChart = ChartFactory.createXYLineChart("Team - Number of Wins", "Year", "Win Count", my_data_series, PlotOrientation.VERTICAL, true, true, false);

                            jPanel15_selcted_nic_graph.setLayout(new java.awt.BorderLayout());


                            ChartPanel CP = new ChartPanel(chart2);
                            //CP.setSize(440, 270);
                            CP.setMaximumDrawHeight(400);
                            CP.setMaximumSize(new Dimension(440, 270));
                            jPanel15_selcted_nic_graph.add(CP, BorderLayout.CENTER);

                            // jPanel1.updateUI();
                            jPanel15_selcted_nic_graph.validate();
                            sleep(2000);
                        }

                    } catch (Exception e) {
                    }
                }
                console("Network Graph Thread is cancelled");
            }
        });
        thread.start();
    }

    public void fillConnectedUsersTable() {
        try {
            DefaultTableModel tableModel = (DefaultTableModel) jTable_connected_users.getModel();
            tableModel.setRowCount(0);
            this.snmpInterface.updateUserList();
            tableModel.setRowCount(this.snmpInterface.tempUserList.size());
            jTable_connected_users.removeAll();
            console("\n PRIVATE List size users"+this.snmpInterface.tempUserList.size());
            if (snmpInterface.users.size() > 0) {
                int col = 0;
                for (int i = 0; i < snmpInterface.tempUserList.size(); i++) {
                    this.jTable_connected_users.getModel().setValueAt(i, i, col);
                    jTable_connected_users.getModel().setValueAt(snmpInterface.tempUserList.get(i).ip, i, col + 1);
                    jTable_connected_users.getModel().setValueAt(snmpInterface.tempUserList.get(i).mac, i, col + 2);
                }

            } else {
                console("The list related to the user state has any elements");
            }
            //

        } catch (Exception e) {
        }

    }
    public boolean state_manage_protocols = true;

    public void manage_protocols() {
        Thread protocols = new Thread(new Runnable() {
            @Override
            public void run() {
                console("Protocol Management Object is started");
                while (state_manage_protocols) {
                    ServerInterface.ProtocolObject obj = null;
                    fillConnectedUsersTable(); //Connec ted Users
                    if (snmpInterface.protocolsInfo.size() > 0) {
                        obj = snmpInterface.protocolsInfo.get(snmpInterface.protocolsInfo.size() - 1);
                        if (obj != null) {

                            fillProtocol_IPDataInTextLabels(obj);
                            fillProtocol_ICMPDataInTextLabels(obj);
                            fillProtocol_UDPDataInTextLabels(obj);
                            fillProtocol_TCPDataInTextLabels(obj);
                            //  fillProtocol_SNMPDataInTextLabels(obj);
                        } else {
                            console("Object is nul1!");
                        }
                    } else {
                        console("No elements in the protocol object");
                    }
                    try {
                        Thread.sleep(graphRedrawDuration);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                console("Protocol Management Object is stopped");
            }
        });
        protocols.start();
    }

    public void fillProtocol_UDPDataInTextLabels(ServerInterface.ProtocolObject obj) {
        this.jLabel_in_udp_data_val.setText(obj.udpInDatagram);
        this.jLabel_out_udp_data_val.setText(obj.udpOutDatagram);
    }

    public void fillProtocol_ICMPDataInTextLabels(ServerInterface.ProtocolObject obj) {
        this.jLabel_in_icmp_mes_val.setText(obj.icmpInPacket);
        this.jLabel_out_icmp_mes_val.setText(obj.icmpOutPacket);
    }

    public void fillProtocol_IPDataInTextLabels(ServerInterface.ProtocolObject obj) {
        this.jLabel_ip_delivered_val.setText(obj.ipInDelivers);
        this.jLabel_ip_received_val.setText(obj.ipInRequests);
        this.jLabel_ip_requested_val.setText(obj.ipOutRequests);
    }

    public void fillProtocol_TCPDataInTextLabels(ServerInterface.ProtocolObject obj) {
        this.jLabel_in_tcp_seg_val.setText(obj.tcpInSegment);
        this.jLabel_out_tcp_seg_val.setText(obj.tcpOutSegment);

    }

    public void fillProtocol_SNMPDataInTextLabels(ServerInterface.ProtocolObject obj) {
        //this.console("These protocol values haven't been yet implemented");
    }
    //TODO: More data should be added to there.
    public boolean state_host_info = true;

    public void manage_host_info() {
        Thread hostInfo = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    console("Host Management Object is started");
                    while (state_host_info) {
                        Thread.sleep(2000);
                        // console("Date"+snmpInterface.host.sysDate);
                        jLabel_sys_date_val.setText(snmpInterface.host.sysDate);
                        jLabel_sys_uptime_val.setText(snmpInterface.host.sysUpTime);
                       // jTextPane_sys_init_param_val.setText(snmpInterface.host.info);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                console("Host Management Object is stopped");
            }
        });
        hostInfo.start();
    }

    /**
     *
     */
    public void prepareConfigParameters() {

        this.jTextField_ipaddress_val.setText(defaultIPAddress);
        this.snmpInterface.setSNMPRequesterIP(this.defaultIPAddress);
        //this.jTextField_ipaddress_val1.setText(defaultIPAddress_public);

        this.buttonGroup1.clearSelection();
        //this.buttonGroup2.clearSelection();
        this.buttonGroup1.setSelected(this.jRadioButton_snmp_v1_privateOS.getModel(), true);
        //this.buttonGroup2.setSelected(this.jRadioButton_snmpv1_publicOS.getModel(), true);

    }

    public void disableAllTabs() {
        console("All interfaces are deactivated.");
        this.state_mamange_cpu_mem = false;
        this.state_manage_network_interface = false;
        this.state_manage_protocols = false;
        this.state_host_info = false;
        //Additional for graph
        state_network_graph = false;
    }

    public void enableAllTabs() {
        console("All interfaces are activated");
        this.state_mamange_cpu_mem = true;
        this.state_manage_network_interface = true;
        this.state_manage_protocols = true;
        this.state_host_info = true;
        //Additional graph
        state_network_graph = true;
    }

    private void disableTabDataTraffic(int selectedTab) {
        // console("Selected Tab"+ selectedTab);
        switch (selectedTab) {
            case 0:
                console("CPU and MEM are activated.");
                if (this.state_mamange_cpu_mem != true) {
                    this.state_mamange_cpu_mem = true;
                    this.manage_cpu_mem();

                }
                this.state_manage_network_interface = false;
                this.state_manage_protocols = false;
                this.state_host_info = false;

                //Additional for graph
                state_network_graph = false;

                break;

            case 1:
                console("Network Interface is activated.");
                if (state_manage_network_interface != true) {
                    this.state_manage_network_interface = true;
                    this.manage_network_interface();
                }
                this.state_mamange_cpu_mem = false;
                this.state_manage_protocols = false;
                this.state_host_info = false;

                break;
            case 2:
                console("Protocol is activated.");
                if (this.state_manage_protocols != true) {
                    this.state_manage_protocols = true;
                    this.manage_protocols();
                }
                this.state_host_info = false;
                this.state_mamange_cpu_mem = false;
                this.state_manage_network_interface = false;
                //Additional for graph
                state_network_graph = false;
                break;
            case 3:
                console("Host is activated.");
                if (this.state_host_info != true) {
                    this.state_host_info = true;
                    this.manage_host_info();
                }
                this.state_manage_protocols = false;
                this.state_mamange_cpu_mem = false;
                this.state_manage_network_interface = false;
                //Additional 
                state_network_graph = false;
                break;

            case 4:
                disableAllTabs();
                break;
            case 5:
                disableAllTabs();
                break;
            case ALLENABLE:
                enableAllTabs();
                break;
            case ALLDISABLE:
                disableAllTabs();
                break;
            default:
                console("The selected tab number is unknown!");
                break;
        };

    }

    public void console(String str) {
        System.out.println(str);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonGroup5 = new javax.swing.ButtonGroup();
        buttonGroup6 = new javax.swing.ButtonGroup();
        buttonGroup7 = new javax.swing.ButtonGroup();
        buttonGroup8 = new javax.swing.ButtonGroup();
        buttonGroup9 = new javax.swing.ButtonGroup();
        buttonGroup10 = new javax.swing.ButtonGroup();
        buttonGroup11 = new javax.swing.ButtonGroup();
        jTabbedPane3 = new javax.swing.JTabbedPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel_cpumem = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jTabbedPane_mem2 = new javax.swing.JTabbedPane();
        jPanel11_mem_stat = new javax.swing.JPanel();
        jbl_mem_avai_swap_space_val = new javax.swing.JLabel();
        jbl_mem_total_swap_size_val = new javax.swing.JLabel();
        jbl_mem_total_ram_buffered = new javax.swing.JLabel();
        jbl_mem_total_ram_shared = new javax.swing.JLabel();
        jbl_mem_total_ram_val = new javax.swing.JLabel();
        jbl_mem_total_swap_size = new javax.swing.JLabel();
        jbl_mem_total_ram_free_val = new javax.swing.JLabel();
        jbl_mem_total_ram_used_val = new javax.swing.JLabel();
        jbl_mem_total_ram_buffered_val = new javax.swing.JLabel();
        jbl_mem_total_ram_free = new javax.swing.JLabel();
        jbl_mem_total_ram_shared_val = new javax.swing.JLabel();
        jbl_mem_total_ram_used = new javax.swing.JLabel();
        jbl_mem_total_ram = new javax.swing.JLabel();
        jbl_mem_total_cached_mem = new javax.swing.JLabel();
        jbl_mem_total_cached_mem_val = new javax.swing.JLabel();
        jbl_mem_avai_swap_space = new javax.swing.JLabel();
        jPanel9_mem_graph = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jTabbedPane_cpu = new javax.swing.JTabbedPane();
        jPanel11_cpu_stat = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jbl_cpu_usercpu_time = new javax.swing.JLabel();
        jbl_cpu_per_user_cpu_val = new javax.swing.JLabel();
        jbl_cpu_raw_user_cpu_val = new javax.swing.JLabel();
        jbl_cpu_per_sys_cpu_val = new javax.swing.JLabel();
        jbl_cpu_raw_sys_cpu_val = new javax.swing.JLabel();
        jbl_cpu_per_idle_cpu_val = new javax.swing.JLabel();
        jbl_cpu_raw_idle_cpu_val = new javax.swing.JLabel();
        jbl_cpu_raw_nice_cpu_val = new javax.swing.JLabel();
        jbl_cpu_raw_nice_cpu = new javax.swing.JLabel();
        jbl_cpu_raw_idle_cpu = new javax.swing.JLabel();
        jbl_cpu_per_idle_cpu = new javax.swing.JLabel();
        jbl_cpu_raw_sys_cpu = new javax.swing.JLabel();
        jbl_cpu_perc_sys_cpu = new javax.swing.JLabel();
        jbl_cpu_raw_user_cpu = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jbl_cpu_1min_load = new javax.swing.JLabel();
        jbl_cpu_1min_load_val = new javax.swing.JLabel();
        jbl_cpu_5min_load = new javax.swing.JLabel();
        jbl_cpu_5min_load_val = new javax.swing.JLabel();
        jbl_cpu_15min_load = new javax.swing.JLabel();
        jbl_cpu_15min_load_val = new javax.swing.JLabel();
        jPanel9_cpu_graph = new javax.swing.JPanel();
        jPanel_net_interfaces = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel14_ni = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jList1_network_interfaces = new javax.swing.JList();
        jPanel15_selcted_nic_graph = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel_prostat = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel11 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jLabel_ip_received = new javax.swing.JLabel();
        jLabel_ip_delivered = new javax.swing.JLabel();
        jLabel_ip_requested = new javax.swing.JLabel();
        jLabel_ip_received_val = new javax.swing.JLabel();
        jLabel_ip_delivered_val = new javax.swing.JLabel();
        jLabel_ip_requested_val = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable_connected_users = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jLabel_in_udp_data = new javax.swing.JLabel();
        jLabel_out_udp_data = new javax.swing.JLabel();
        jLabel_in_udp_data_val = new javax.swing.JLabel();
        jLabel_out_udp_data_val = new javax.swing.JLabel();
        jLabel_udp_no_port = new javax.swing.JLabel();
        jLabel_udp_no_port_val = new javax.swing.JLabel();
        jLabel_udp_local_ports = new javax.swing.JLabel();
        jLabel_udp_local_pots_cal = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel_in_tcp_seg = new javax.swing.JLabel();
        jLabel_out_tcp_seg = new javax.swing.JLabel();
        jLabel_in_tcp_seg_val = new javax.swing.JLabel();
        jLabel_out_tcp_seg_val = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel_in_tcp_mes = new javax.swing.JLabel();
        jLabel_in_icmp_mes = new javax.swing.JLabel();
        jLabel_in_icmp_mes_val = new javax.swing.JLabel();
        jLabel_out_icmp_mes_val = new javax.swing.JLabel();
        jPanel_hostinfo = new javax.swing.JPanel();
        jLabel_sys_uptime = new javax.swing.JLabel();
        jLabel_sys_date = new javax.swing.JLabel();
        jLabel_sys_uptime_val = new javax.swing.JLabel();
        jLabel_sys_date_val = new javax.swing.JLabel();
        jPanel_snmpreq = new javax.swing.JPanel();
        jButton_send_oid_request = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea_snmpreq_info = new javax.swing.JTextArea();
        jTextField_oid_number = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextArea_snmp_req_output = new javax.swing.JTextArea();
        jLabel_snmp_req_info = new javax.swing.JLabel();
        jLabel_snmp_req_info_val = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jButton_ping_start = new javax.swing.JButton();
        jButton_ping_stop = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jTextField_ping_ipaddress = new javax.swing.JTextField();
        jScrollPane8 = new javax.swing.JScrollPane();
        jTextArea_ping_output_privateOs = new javax.swing.JTextArea();
        jButton_ping_clean_io = new javax.swing.JButton();
        jPanel14 = new javax.swing.JPanel();
        jComboBox_iperf_protocols = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jComboBox_iperf_durations = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jComboBox_iperf_bw = new javax.swing.JComboBox();
        jButton_iperf_start = new javax.swing.JButton();
        jButton_iperf_stop = new javax.swing.JButton();
        jButton_iperf_clean_io = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane10 = new javax.swing.JScrollPane();
        jTextArea_iperf_output_privateOs = new javax.swing.JTextArea();
        jTabbedPane4 = new javax.swing.JTabbedPane();
        jPanel_cpumem1 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jTabbedPane_mem3 = new javax.swing.JTabbedPane();
        jPanel11_mem_stat1 = new javax.swing.JPanel();
        jbl_mem_avai_swap_space_val1 = new javax.swing.JLabel();
        jbl_mem_total_swap_size_val1 = new javax.swing.JLabel();
        jbl_mem_total_ram_buffered1 = new javax.swing.JLabel();
        jbl_mem_total_ram_shared1 = new javax.swing.JLabel();
        jbl_mem_total_ram_val1 = new javax.swing.JLabel();
        jbl_mem_total_swap_size1 = new javax.swing.JLabel();
        jbl_mem_total_ram_free_val1 = new javax.swing.JLabel();
        jbl_mem_total_ram_used_val1 = new javax.swing.JLabel();
        jbl_mem_total_ram_buffered_val1 = new javax.swing.JLabel();
        jbl_mem_total_ram_free1 = new javax.swing.JLabel();
        jbl_mem_total_ram_shared_val1 = new javax.swing.JLabel();
        jbl_mem_total_ram_used1 = new javax.swing.JLabel();
        jbl_mem_total_ram1 = new javax.swing.JLabel();
        jbl_mem_total_cached_mem1 = new javax.swing.JLabel();
        jbl_mem_total_cached_mem_val1 = new javax.swing.JLabel();
        jbl_mem_avai_swap_space1 = new javax.swing.JLabel();
        jPanel9_mem_graph1 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jTabbedPane_cpu1 = new javax.swing.JTabbedPane();
        jPanel11_cpu_stat1 = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        jbl_cpu_usercpu_time1 = new javax.swing.JLabel();
        jbl_cpu_per_user_cpu_val1 = new javax.swing.JLabel();
        jbl_cpu_raw_user_cpu_val1 = new javax.swing.JLabel();
        jbl_cpu_per_sys_cpu_val1 = new javax.swing.JLabel();
        jbl_cpu_raw_sys_cpu_val1 = new javax.swing.JLabel();
        jbl_cpu_per_idle_cpu_val1 = new javax.swing.JLabel();
        jbl_cpu_raw_idle_cpu_val1 = new javax.swing.JLabel();
        jbl_cpu_raw_nice_cpu_val1 = new javax.swing.JLabel();
        jbl_cpu_raw_nice_cpu1 = new javax.swing.JLabel();
        jbl_cpu_raw_idle_cpu1 = new javax.swing.JLabel();
        jbl_cpu_per_idle_cpu1 = new javax.swing.JLabel();
        jbl_cpu_raw_sys_cpu1 = new javax.swing.JLabel();
        jbl_cpu_perc_sys_cpu1 = new javax.swing.JLabel();
        jbl_cpu_raw_user_cpu1 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        jbl_cpu_1min_load1 = new javax.swing.JLabel();
        jbl_cpu_1min_load_val1 = new javax.swing.JLabel();
        jbl_cpu_5min_load1 = new javax.swing.JLabel();
        jbl_cpu_5min_load_val1 = new javax.swing.JLabel();
        jbl_cpu_15min_load1 = new javax.swing.JLabel();
        jbl_cpu_15min_load_val1 = new javax.swing.JLabel();
        jPanel9_cpu_graph1 = new javax.swing.JPanel();
        jPanel_net_interfaces1 = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        jPanel14_ni1 = new javax.swing.JPanel();
        jScrollPane9 = new javax.swing.JScrollPane();
        jList1_network_interfaces1 = new javax.swing.JList();
        jPanel15_selcted_nic_graph1 = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        jScrollPane11 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel_prostat1 = new javax.swing.JPanel();
        jTabbedPane5 = new javax.swing.JTabbedPane();
        jPanel22 = new javax.swing.JPanel();
        jPanel23 = new javax.swing.JPanel();
        jLabel_ip_received1 = new javax.swing.JLabel();
        jLabel_ip_delivered1 = new javax.swing.JLabel();
        jLabel_ip_requested1 = new javax.swing.JLabel();
        jLabel_ip_received_val1 = new javax.swing.JLabel();
        jLabel_ip_delivered_val1 = new javax.swing.JLabel();
        jLabel_ip_requested_val1 = new javax.swing.JLabel();
        jPanel24 = new javax.swing.JPanel();
        jScrollPane12 = new javax.swing.JScrollPane();
        jTable_connected_users1 = new javax.swing.JTable();
        jPanel25 = new javax.swing.JPanel();
        jLabel_in_udp_data1 = new javax.swing.JLabel();
        jLabel_out_udp_data1 = new javax.swing.JLabel();
        jLabel_in_udp_data_val1 = new javax.swing.JLabel();
        jLabel_out_udp_data_val1 = new javax.swing.JLabel();
        jLabel_udp_no_port1 = new javax.swing.JLabel();
        jLabel_udp_no_port_val1 = new javax.swing.JLabel();
        jLabel_udp_local_ports1 = new javax.swing.JLabel();
        jLabel_udp_local_pots_cal1 = new javax.swing.JLabel();
        jPanel26 = new javax.swing.JPanel();
        jLabel_in_tcp_seg1 = new javax.swing.JLabel();
        jLabel_out_tcp_seg1 = new javax.swing.JLabel();
        jLabel_in_tcp_seg_val1 = new javax.swing.JLabel();
        jLabel_out_tcp_seg_val1 = new javax.swing.JLabel();
        jPanel27 = new javax.swing.JPanel();
        jLabel_in_tcp_mes1 = new javax.swing.JLabel();
        jLabel_in_icmp_mes1 = new javax.swing.JLabel();
        jLabel_in_icmp_mes_val1 = new javax.swing.JLabel();
        jLabel_out_icmp_mes_val1 = new javax.swing.JLabel();
        jPanel_hostinfo1 = new javax.swing.JPanel();
        jLabel_sys_uptime1 = new javax.swing.JLabel();
        jLabel_sys_date1 = new javax.swing.JLabel();
        jLabel_sys_uptime_val1 = new javax.swing.JLabel();
        jLabel_sys_date_val1 = new javax.swing.JLabel();
        jPanel_snmpreq1 = new javax.swing.JPanel();
        jButton_send_oid_request1 = new javax.swing.JButton();
        jScrollPane14 = new javax.swing.JScrollPane();
        jTextArea_snmpreq_info1 = new javax.swing.JTextArea();
        jTextField_oid_number1 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane15 = new javax.swing.JScrollPane();
        jTextArea_snmp_req_output1 = new javax.swing.JTextArea();
        jLabel_snmp_req_info1 = new javax.swing.JLabel();
        jLabel_snmp_req_info_val1 = new javax.swing.JLabel();
        jPanel28 = new javax.swing.JPanel();
        jPanel29 = new javax.swing.JPanel();
        jButton_ping_start1 = new javax.swing.JButton();
        jButton_ping_stop1 = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jTextField_ping_ipaddress1 = new javax.swing.JTextField();
        jScrollPane17 = new javax.swing.JScrollPane();
        jTextArea_ping_output_publicOs = new javax.swing.JTextArea();
        jButton_ping_clean_io1 = new javax.swing.JButton();
        jPanel30 = new javax.swing.JPanel();
        jComboBox_iperf_protocols1 = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jComboBox_iperf_durations1 = new javax.swing.JComboBox();
        jLabel11 = new javax.swing.JLabel();
        jComboBox_iperf_bw1 = new javax.swing.JComboBox();
        jButton_iperf_start1 = new javax.swing.JButton();
        jButton_iperf_stop1 = new javax.swing.JButton();
        jButton_iperf_clean_io1 = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane18 = new javax.swing.JScrollPane();
        jTextArea_iperf_output_publicOs = new javax.swing.JTextArea();
        jPanel_control_interface = new javax.swing.JPanel();
        jPanel_control_interface_panel = new javax.swing.JPanel();
        jTextField_userID_control_interface = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList_ci_usevios_enabled_users = new javax.swing.JList();
        jButton_ci_add_service = new javax.swing.JButton();
        jButton_ci_remove_service = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel_ci_errors = new javax.swing.JLabel();
        jPanel_control_interface_panel1 = new javax.swing.JPanel();
        jTextField_control_interface_MAC = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jScrollPane13 = new javax.swing.JScrollPane();
        jList_ci_usevios_mac_vpnip = new javax.swing.JList();
        jButton_ci_add_mac_vpnip = new javax.swing.JButton();
        jButton_ci_remove_mac_vpnip = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        jScrollPane21 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel_ci_errors1 = new javax.swing.JLabel();
        jTextField_control_interface_vpnip = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jPanel31 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jPanel_console_outputs = new javax.swing.JPanel();
        jPanel32 = new javax.swing.JPanel();
        jScrollPane20 = new javax.swing.JScrollPane();
        jTextArea_console_outputs = new javax.swing.JTextArea();
        jButton_console_outputs = new javax.swing.JButton();
        jPanel_conf_panel = new javax.swing.JPanel();
        jPanel_inner_conf1 = new javax.swing.JPanel();
        jButton_stop_snmp2 = new javax.swing.JButton();
        jButton_start_snmp2 = new javax.swing.JButton();
        jTextField_ipaddress_val = new javax.swing.JTextField();
        jPanel_snmp_versions2 = new javax.swing.JPanel();
        jRadioButton_snmp_v1_privateOS = new javax.swing.JRadioButton();
        jRadioButton_snmpv2_privateOS = new javax.swing.JRadioButton();
        jLabel_ipaddress2 = new javax.swing.JLabel();
        jScrollPane16 = new javax.swing.JScrollPane();
        jTextArea_privateOS = new javax.swing.JTextArea();
        jButton_clean_privateOS = new javax.swing.JButton();
        jPanel_inner_conf2 = new javax.swing.JPanel();
        jButton_stop_snmp3 = new javax.swing.JButton();
        jButton_start_snmp3 = new javax.swing.JButton();
        jTextField_ipaddress_val1 = new javax.swing.JTextField();
        jPanel_snmp_versions3 = new javax.swing.JPanel();
        jRadioButton_snmpv1_publicOS = new javax.swing.JRadioButton();
        jRadioButton_snmpv2_publicOS = new javax.swing.JRadioButton();
        jLabel_ipaddress3 = new javax.swing.JLabel();
        jScrollPane19 = new javax.swing.JScrollPane();
        jTextArea_publicOs = new javax.swing.JTextArea();
        jButton_clean_publicOS = new javax.swing.JButton();
        jPanel_status_bar = new javax.swing.JPanel();
        jLabel_status = new javax.swing.JLabel();
        jLabel_status_report = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Argela - uSeViOS System Monitor");

        jTabbedPane3.setPreferredSize(new java.awt.Dimension(686, 700));
        jTabbedPane3.setRequestFocusEnabled(false);

        jTabbedPane1.setBackground(new java.awt.Color(204, 204, 204));
        jTabbedPane1.setAlignmentX(0.3F);
        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                enableSelectedTab(evt);
            }
        });

        jPanel15.setBorder(javax.swing.BorderFactory.createTitledBorder("Memory Parameters(Extra OS Modifications Required)"));

        jbl_mem_avai_swap_space_val.setText("0");

        jbl_mem_total_swap_size_val.setText("0");

        jbl_mem_total_ram_buffered.setText("Total Ram Buffered                :");

        jbl_mem_total_ram_shared.setText("Total Ram Shared                  :");

        jbl_mem_total_ram_val.setText("0");

        jbl_mem_total_swap_size.setText("Total Swap Size                     :");

        jbl_mem_total_ram_free_val.setText("0");

        jbl_mem_total_ram_used_val.setText("0");

        jbl_mem_total_ram_buffered_val.setText("0");

        jbl_mem_total_ram_free.setText("Total Ram Free                      :");

        jbl_mem_total_ram_shared_val.setText("0");

        jbl_mem_total_ram_used.setText("Total Ram Used                     :");

        jbl_mem_total_ram.setText("Total Ram in Machine            :");

        jbl_mem_total_cached_mem.setText("Total Cached Memory            :");

        jbl_mem_total_cached_mem_val.setText("0");

        jbl_mem_avai_swap_space.setText("Available Swap Space            :");

        org.jdesktop.layout.GroupLayout jPanel11_mem_statLayout = new org.jdesktop.layout.GroupLayout(jPanel11_mem_stat);
        jPanel11_mem_stat.setLayout(jPanel11_mem_statLayout);
        jPanel11_mem_statLayout.setHorizontalGroup(
            jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11_mem_statLayout.createSequentialGroup()
                .add(23, 23, 23)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel11_mem_statLayout.createSequentialGroup()
                        .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jbl_mem_avai_swap_space)
                            .add(jbl_mem_total_swap_size))
                        .add(35, 35, 35)
                        .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jbl_mem_avai_swap_space_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                            .add(jbl_mem_total_swap_size_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel11_mem_statLayout.createSequentialGroup()
                        .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                .add(jbl_mem_total_ram_used, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jbl_mem_total_ram, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(jbl_mem_total_ram_free)
                            .add(jbl_mem_total_ram_shared)
                            .add(jbl_mem_total_ram_buffered)
                            .add(jbl_mem_total_cached_mem))
                        .add(32, 32, 32)
                        .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jbl_mem_total_cached_mem_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_buffered_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_shared_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_free_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_used_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(0, 278, Short.MAX_VALUE))))
        );
        jPanel11_mem_statLayout.setVerticalGroup(
            jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11_mem_statLayout.createSequentialGroup()
                .add(30, 30, 30)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_swap_size_val)
                    .add(jbl_mem_total_swap_size))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_avai_swap_space_val)
                    .add(jbl_mem_avai_swap_space))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram)
                    .add(jbl_mem_total_ram_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram_used)
                    .add(jbl_mem_total_ram_used_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram_free)
                    .add(jbl_mem_total_ram_free_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram_shared)
                    .add(jbl_mem_total_ram_shared_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram_buffered)
                    .add(jbl_mem_total_ram_buffered_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_cached_mem)
                    .add(jbl_mem_total_cached_mem_val))
                .addContainerGap(112, Short.MAX_VALUE))
        );

        jTabbedPane_mem2.addTab("Statistics", jPanel11_mem_stat);

        jPanel9_mem_graph.setLayout(new java.awt.BorderLayout());
        jTabbedPane_mem2.addTab("Graphs", jPanel9_mem_graph);

        org.jdesktop.layout.GroupLayout jPanel15Layout = new org.jdesktop.layout.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane_mem2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel15Layout.createSequentialGroup()
                .add(jTabbedPane_mem2)
                .addContainerGap())
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("CPU Parameters(Extra OS Modifications Required)"));
        jPanel1.setToolTipText("CPU");

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder("Known Parameters")));

        jbl_cpu_usercpu_time.setText("Percentage of User CPU Time       :");

        jbl_cpu_per_user_cpu_val.setText("0");

        jbl_cpu_raw_user_cpu_val.setText("0");

        jbl_cpu_per_sys_cpu_val.setText("0");

        jbl_cpu_raw_sys_cpu_val.setText("0");

        jbl_cpu_per_idle_cpu_val.setText("0");

        jbl_cpu_raw_idle_cpu_val.setText("0");

        jbl_cpu_raw_nice_cpu_val.setText("0");

        jbl_cpu_raw_nice_cpu.setText("Raw Nice CPU TIme                      :");

        jbl_cpu_raw_idle_cpu.setText("Raw Idle CPU Time                       :");

        jbl_cpu_per_idle_cpu.setText("Percentage of Idle CPU Time        :");

        jbl_cpu_raw_sys_cpu.setText("Raw System CPU Time                  :");

        jbl_cpu_perc_sys_cpu.setText("Percentages of System CPU Time :");

        jbl_cpu_raw_user_cpu.setText("Raw User CPU Time                      :");

        org.jdesktop.layout.GroupLayout jPanel8Layout = new org.jdesktop.layout.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel8Layout.createSequentialGroup()
                        .add(jbl_cpu_usercpu_time)
                        .add(39, 39, 39)
                        .add(jbl_cpu_per_user_cpu_val))
                    .add(jPanel8Layout.createSequentialGroup()
                        .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jbl_cpu_raw_user_cpu)
                            .add(jbl_cpu_perc_sys_cpu)
                            .add(jbl_cpu_raw_sys_cpu)
                            .add(jbl_cpu_per_idle_cpu)
                            .add(jbl_cpu_raw_idle_cpu)
                            .add(jbl_cpu_raw_nice_cpu))
                        .add(39, 39, 39)
                        .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jbl_cpu_raw_nice_cpu_val)
                            .add(jbl_cpu_raw_idle_cpu_val)
                            .add(jbl_cpu_per_idle_cpu_val)
                            .add(jbl_cpu_raw_sys_cpu_val)
                            .add(jbl_cpu_per_sys_cpu_val)
                            .add(jbl_cpu_raw_user_cpu_val))))
                .addContainerGap(110, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_usercpu_time)
                    .add(jbl_cpu_per_user_cpu_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_raw_user_cpu)
                    .add(jbl_cpu_raw_user_cpu_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_perc_sys_cpu)
                    .add(jbl_cpu_per_sys_cpu_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_raw_sys_cpu)
                    .add(jbl_cpu_raw_sys_cpu_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_per_idle_cpu)
                    .add(jbl_cpu_per_idle_cpu_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_raw_idle_cpu)
                    .add(jbl_cpu_raw_idle_cpu_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_raw_nice_cpu)
                    .add(jbl_cpu_raw_nice_cpu_val))
                .addContainerGap(93, Short.MAX_VALUE))
        );

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder("CPU 1, 5, 15 min Load"));

        jbl_cpu_1min_load.setText("1min:");

        jbl_cpu_1min_load_val.setText("0");

        jbl_cpu_5min_load.setText("5min:");

        jbl_cpu_5min_load_val.setText("0");

        jbl_cpu_15min_load.setText("15min:");

        jbl_cpu_15min_load_val.setText("0");

        org.jdesktop.layout.GroupLayout jPanel9Layout = new org.jdesktop.layout.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel9Layout.createSequentialGroup()
                        .add(40, 40, 40)
                        .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel9Layout.createSequentialGroup()
                                .add(jbl_cpu_5min_load)
                                .add(18, 18, 18)
                                .add(jbl_cpu_5min_load_val))
                            .add(jPanel9Layout.createSequentialGroup()
                                .add(jbl_cpu_1min_load)
                                .add(18, 18, 18)
                                .add(jbl_cpu_1min_load_val))))
                    .add(jPanel9Layout.createSequentialGroup()
                        .add(34, 34, 34)
                        .add(jbl_cpu_15min_load)
                        .add(18, 18, 18)
                        .add(jbl_cpu_15min_load_val)))
                .addContainerGap(165, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_1min_load)
                    .add(jbl_cpu_1min_load_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_5min_load)
                    .add(jbl_cpu_5min_load_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_15min_load)
                    .add(jbl_cpu_15min_load_val))
                .addContainerGap(183, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel11_cpu_statLayout = new org.jdesktop.layout.GroupLayout(jPanel11_cpu_stat);
        jPanel11_cpu_stat.setLayout(jPanel11_cpu_statLayout);
        jPanel11_cpu_statLayout.setHorizontalGroup(
            jPanel11_cpu_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11_cpu_statLayout.createSequentialGroup()
                .add(jPanel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel11_cpu_statLayout.setVerticalGroup(
            jPanel11_cpu_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11_cpu_statLayout.createSequentialGroup()
                .add(jPanel11_cpu_statLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(0, 0, 0))
        );

        jTabbedPane_cpu.addTab("Statistics", jPanel11_cpu_stat);

        jPanel9_cpu_graph.setLayout(new java.awt.BorderLayout());
        jTabbedPane_cpu.addTab("Graph", jPanel9_cpu_graph);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane_cpu)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jTabbedPane_cpu)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel_cpumemLayout = new org.jdesktop.layout.GroupLayout(jPanel_cpumem);
        jPanel_cpumem.setLayout(jPanel_cpumemLayout);
        jPanel_cpumemLayout.setHorizontalGroup(
            jPanel_cpumemLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_cpumemLayout.createSequentialGroup()
                .add(jPanel_cpumemLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel15, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel_cpumemLayout.setVerticalGroup(
            jPanel_cpumemLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_cpumemLayout.createSequentialGroup()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel15, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("CPU&Memory", jPanel_cpumem);

        jPanel_net_interfaces.setPreferredSize(new java.awt.Dimension(537, 600));

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Dynamically Traffic Change in Network Interfaces"));
        jPanel7.setMaximumSize(new java.awt.Dimension(625, 314));
        jPanel7.setMinimumSize(new java.awt.Dimension(630, 310));

        jPanel14_ni.setBorder(javax.swing.BorderFactory.createTitledBorder("Network Int"));
        jPanel14_ni.setMaximumSize(new java.awt.Dimension(200, 2147483647));
        jPanel14_ni.setMinimumSize(new java.awt.Dimension(120, 300));
        jPanel14_ni.setLayout(new java.awt.BorderLayout());

        jList1_network_interfaces.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Interface 1", "Interface 2" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList1_network_interfaces.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1_network_interfaces.setMinimumSize(new java.awt.Dimension(40, 50));
        jList1_network_interfaces.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                drawGraph(evt);
            }
        });
        jScrollPane4.setViewportView(jList1_network_interfaces);

        jPanel14_ni.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        jPanel15_selcted_nic_graph.setBorder(javax.swing.BorderFactory.createTitledBorder("Realtime Traffic Variation in Selected NI"));
        jPanel15_selcted_nic_graph.setMinimumSize(new java.awt.Dimension(480, 300));
        jPanel15_selcted_nic_graph.setPreferredSize(new java.awt.Dimension(480, 300));
        jPanel15_selcted_nic_graph.setLayout(new java.awt.BorderLayout());

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .add(jPanel14_ni, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel15_selcted_nic_graph, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 579, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel14_ni, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel15_selcted_nic_graph, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
        );

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("All NI Current Values"));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Network Interfaces", "Incoming Packets", "Outgoing Packets"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setShowGrid(true);
        jScrollPane1.setViewportView(jTable1);

        org.jdesktop.layout.GroupLayout jPanel10Layout = new org.jdesktop.layout.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane1)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel_net_interfacesLayout = new org.jdesktop.layout.GroupLayout(jPanel_net_interfaces);
        jPanel_net_interfaces.setLayout(jPanel_net_interfacesLayout);
        jPanel_net_interfacesLayout.setHorizontalGroup(
            jPanel_net_interfacesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel_net_interfacesLayout.setVerticalGroup(
            jPanel_net_interfacesLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_net_interfacesLayout.createSequentialGroup()
                .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(21, 21, 21))
        );

        jTabbedPane1.addTab("Network Int", jPanel_net_interfaces);

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 13), new java.awt.Color(255, 255, 255))); // NOI18N

        jLabel_ip_received.setText("Received IP Packets            :");

        jLabel_ip_delivered.setText("Delivered IP Packets            :");

        jLabel_ip_requested.setText("Requested IP Packets          :");

        jLabel_ip_received_val.setText("0");

        jLabel_ip_delivered_val.setText("0");

        jLabel_ip_requested_val.setText("0");

        org.jdesktop.layout.GroupLayout jPanel12Layout = new org.jdesktop.layout.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel12Layout.createSequentialGroup()
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel12Layout.createSequentialGroup()
                        .add(28, 28, 28)
                        .add(jLabel_ip_received))
                    .add(jPanel12Layout.createSequentialGroup()
                        .add(25, 25, 25)
                        .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel_ip_delivered)
                            .add(jLabel_ip_requested))))
                .add(41, 41, 41)
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_ip_delivered_val)
                    .add(jLabel_ip_received_val)
                    .add(jLabel_ip_requested_val))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel12Layout.createSequentialGroup()
                .add(32, 32, 32)
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel12Layout.createSequentialGroup()
                        .add(jLabel_ip_received_val)
                        .add(12, 12, 12)
                        .add(jLabel_ip_delivered_val)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jLabel_ip_requested_val))
                    .add(jPanel12Layout.createSequentialGroup()
                        .add(jLabel_ip_received)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jLabel_ip_delivered)
                        .add(12, 12, 12)
                        .add(jLabel_ip_requested)))
                .add(0, 260, Short.MAX_VALUE))
        );

        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder("Connected Users"));

        jTable_connected_users.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Number", "IP Address", "MAC Address"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTable_connected_users);

        org.jdesktop.layout.GroupLayout jPanel13Layout = new org.jdesktop.layout.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 672, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 293, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );

        org.jdesktop.layout.GroupLayout jPanel11Layout = new org.jdesktop.layout.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel11Layout.createSequentialGroup()
                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .add(jPanel12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(18, 18, 18))
        );

        jTabbedPane2.addTab("IP", jPanel11);

        jLabel_in_udp_data.setText("Incoming Datagrams   :");

        jLabel_out_udp_data.setText("Outgoing Datagrams   :");

        jLabel_in_udp_data_val.setText("0");

        jLabel_out_udp_data_val.setText("0");

        jLabel_udp_no_port.setText("UDP No Ports               :");

        jLabel_udp_no_port_val.setText("NULL");

        jLabel_udp_local_ports.setText("UDP Local Ports           :");

        jLabel_udp_local_pots_cal.setText("NULL");

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(40, 40, 40)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_in_udp_data)
                    .add(jLabel_out_udp_data)
                    .add(jLabel_udp_no_port)
                    .add(jLabel_udp_local_ports))
                .add(18, 18, 18)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_udp_local_pots_cal)
                    .add(jLabel_udp_no_port_val)
                    .add(jLabel_out_udp_data_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel_in_udp_data_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE))
                .addContainerGap(195, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(35, 35, 35)
                        .add(jLabel_in_udp_data_val))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel_in_udp_data)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_out_udp_data)
                    .add(jLabel_out_udp_data_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_udp_no_port)
                    .add(jLabel_udp_no_port_val))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_udp_local_ports)
                    .add(jLabel_udp_local_pots_cal))
                .addContainerGap(586, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("UDP", jPanel3);

        jLabel_in_tcp_seg.setText("TCP Incoming Segments       :");

        jLabel_out_tcp_seg.setText("TCP Outcoming Segments    :");

        jLabel_in_tcp_seg_val.setText("0");

        jLabel_out_tcp_seg_val.setText("0");

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(37, 37, 37)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jLabel_out_tcp_seg)
                        .add(18, 18, 18)
                        .add(jLabel_out_tcp_seg_val))
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jLabel_in_tcp_seg)
                        .add(18, 18, 18)
                        .add(jLabel_in_tcp_seg_val)))
                .addContainerGap(455, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(36, 36, 36)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_in_tcp_seg)
                    .add(jLabel_in_tcp_seg_val))
                .add(18, 18, 18)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_out_tcp_seg)
                    .add(jLabel_out_tcp_seg_val))
                .addContainerGap(635, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("TCP", jPanel5);

        jLabel_in_tcp_mes.setText("ICMP Outgoing Messages       :");

        jLabel_in_icmp_mes.setText("ICMP Incoming Messages       :");

        jLabel_in_icmp_mes_val.setText("0");

        jLabel_out_icmp_mes_val.setText("0");

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(37, 37, 37)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_in_icmp_mes)
                    .add(jLabel_in_tcp_mes))
                .add(26, 26, 26)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel4Layout.createSequentialGroup()
                        .add(jLabel_in_icmp_mes_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                        .add(8, 8, 8))
                    .add(jLabel_out_icmp_mes_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE))
                .add(190, 190, 190))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(38, 38, 38)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_in_icmp_mes)
                    .add(jLabel_in_icmp_mes_val))
                .add(18, 18, 18)
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_in_tcp_mes)
                    .add(jLabel_out_icmp_mes_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(633, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("ICMP", jPanel4);

        org.jdesktop.layout.GroupLayout jPanel_prostatLayout = new org.jdesktop.layout.GroupLayout(jPanel_prostat);
        jPanel_prostat.setLayout(jPanel_prostatLayout);
        jPanel_prostatLayout.setHorizontalGroup(
            jPanel_prostatLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane2)
        );
        jPanel_prostatLayout.setVerticalGroup(
            jPanel_prostatLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jTabbedPane2)
        );

        jTabbedPane1.addTab("Protocol Stat", jPanel_prostat);

        jLabel_sys_uptime.setText("System Uptime         :");

        jLabel_sys_date.setText("System Date            :");

        jLabel_sys_uptime_val.setText("0");

        jLabel_sys_date_val.setText("0");

        org.jdesktop.layout.GroupLayout jPanel_hostinfoLayout = new org.jdesktop.layout.GroupLayout(jPanel_hostinfo);
        jPanel_hostinfo.setLayout(jPanel_hostinfoLayout);
        jPanel_hostinfoLayout.setHorizontalGroup(
            jPanel_hostinfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_hostinfoLayout.createSequentialGroup()
                .add(83, 83, 83)
                .add(jPanel_hostinfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel_sys_date)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel_sys_uptime))
                .add(18, 18, 18)
                .add(jPanel_hostinfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_sys_uptime_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                    .add(jLabel_sys_date_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(168, 168, 168))
        );
        jPanel_hostinfoLayout.setVerticalGroup(
            jPanel_hostinfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_hostinfoLayout.createSequentialGroup()
                .add(40, 40, 40)
                .add(jPanel_hostinfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_sys_uptime)
                    .add(jLabel_sys_uptime_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_hostinfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_sys_date)
                    .add(jLabel_sys_date_val))
                .addContainerGap(689, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("HostInfo", jPanel_hostinfo);

        jButton_send_oid_request.setText("Send Request");
        jButton_send_oid_request.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_send_oid_requestActionPerformed(evt);
            }
        });

        jTextArea_snmpreq_info.setEditable(false);
        jTextArea_snmpreq_info.setColumns(20);
        jTextArea_snmpreq_info.setRows(5);
        jTextArea_snmpreq_info.setText("    \n    In this section you can easily request much more information by inserting \n    Object Identifiers (OID)s. You can send more than one request at each\n    time.The response of these requests will be viewed below.");
        jScrollPane5.setViewportView(jTextArea_snmpreq_info);

        jLabel1.setText("Output of the requested OID");

        jTextArea_snmp_req_output.setEditable(false);
        jTextArea_snmp_req_output.setColumns(20);
        jTextArea_snmp_req_output.setRows(5);
        jScrollPane6.setViewportView(jTextArea_snmp_req_output);

        jLabel_snmp_req_info.setText("INFO:");

        org.jdesktop.layout.GroupLayout jPanel_snmpreqLayout = new org.jdesktop.layout.GroupLayout(jPanel_snmpreq);
        jPanel_snmpreq.setLayout(jPanel_snmpreqLayout);
        jPanel_snmpreqLayout.setHorizontalGroup(
            jPanel_snmpreqLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_snmpreqLayout.createSequentialGroup()
                .add(48, 48, 48)
                .add(jPanel_snmpreqLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel_snmpreqLayout.createSequentialGroup()
                        .add(jLabel1)
                        .add(0, 496, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_snmpreqLayout.createSequentialGroup()
                        .add(jPanel_snmpreqLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jPanel_snmpreqLayout.createSequentialGroup()
                                .add(6, 6, 6)
                                .add(jLabel_snmp_req_info)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jLabel_snmp_req_info_val, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane6)
                            .add(jPanel_snmpreqLayout.createSequentialGroup()
                                .add(0, 0, Short.MAX_VALUE)
                                .add(jButton_send_oid_request, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 128, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane5)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jTextField_oid_number))
                        .add(50, 50, 50))))
        );
        jPanel_snmpreqLayout.setVerticalGroup(
            jPanel_snmpreqLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_snmpreqLayout.createSequentialGroup()
                .add(18, 18, 18)
                .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 105, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_snmpreqLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_snmp_req_info)
                    .add(jLabel_snmp_req_info_val))
                .add(34, 34, 34)
                .add(jTextField_oid_number, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jButton_send_oid_request, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 452, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(50, 50, 50))
        );

        jTabbedPane1.addTab("Request", jPanel_snmpreq);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Ping Test"));

        jButton_ping_start.setText("Start Ping Test");
        jButton_ping_start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ping_startActionPerformed(evt);
            }
        });

        jButton_ping_stop.setText("Stop Ping Test");
        jButton_ping_stop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ping_stopActionPerformed(evt);
            }
        });

        jLabel5.setText("IP Address:");

        jTextField_ping_ipaddress.setText("192.168.126.1");
        jTextField_ping_ipaddress.setPreferredSize(new java.awt.Dimension(90, 30));

        jTextArea_ping_output_privateOs.setColumns(20);
        jTextArea_ping_output_privateOs.setRows(5);
        jScrollPane8.setViewportView(jTextArea_ping_output_privateOs);

        jButton_ping_clean_io.setText("Clean");
        jButton_ping_clean_io.setMaximumSize(new java.awt.Dimension(79, 35));
        jButton_ping_clean_io.setPreferredSize(new java.awt.Dimension(79, 30));
        jButton_ping_clean_io.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ping_clean_ioActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jButton_ping_clean_io, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 94, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel6Layout.createSequentialGroup()
                        .add(21, 21, 21)
                        .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane8)
                            .add(jPanel6Layout.createSequentialGroup()
                                .add(jLabel5)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jTextField_ping_ipaddress, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(48, 48, 48)
                                .add(jButton_ping_start, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 124, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton_ping_stop)))))
                .add(21, 21, 21))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel5)
                        .add(jButton_ping_start, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jButton_ping_stop, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jTextField_ping_ipaddress, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jScrollPane8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 241, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_ping_clean_io, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder("Iperf Test"));

        jComboBox_iperf_protocols.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TCP", "UDP" }));
        jComboBox_iperf_protocols.setPreferredSize(new java.awt.Dimension(82, 35));
        jComboBox_iperf_protocols.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox_iperf_protocolsActionPerformed(evt);
            }
        });

        jLabel2.setText("Pro.:");

        jLabel3.setText("Dur.:");

        jComboBox_iperf_durations.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "5", "10", "20", "40", "60", "120", "240", "360" }));
        jComboBox_iperf_durations.setMinimumSize(new java.awt.Dimension(100, 27));
        jComboBox_iperf_durations.setPreferredSize(new java.awt.Dimension(80, 35));

        jLabel4.setText("Bw:");

        jComboBox_iperf_bw.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1m", "3m", "5m", "7m", "9m", "15m", "20m" }));
        jComboBox_iperf_bw.setPreferredSize(new java.awt.Dimension(72, 35));

        jButton_iperf_start.setText("Start");
        jButton_iperf_start.setMinimumSize(new java.awt.Dimension(75, 35));
        jButton_iperf_start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_iperf_startActionPerformed(evt);
            }
        });

        jButton_iperf_stop.setText("Stop");
        jButton_iperf_stop.setMinimumSize(new java.awt.Dimension(75, 35));
        jButton_iperf_stop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_iperf_stopActionPerformed(evt);
            }
        });

        jButton_iperf_clean_io.setText("Clean");
        jButton_iperf_clean_io.setPreferredSize(new java.awt.Dimension(79, 30));
        jButton_iperf_clean_io.setSize(new java.awt.Dimension(97, 30));
        jButton_iperf_clean_io.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_iperf_clean_ioActionPerformed(evt);
            }
        });

        jLabel6.setText("s");

        jTextArea_iperf_output_privateOs.setColumns(20);
        jTextArea_iperf_output_privateOs.setRows(5);
        jScrollPane10.setViewportView(jTextArea_iperf_output_privateOs);

        org.jdesktop.layout.GroupLayout jPanel14Layout = new org.jdesktop.layout.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel14Layout.createSequentialGroup()
                .add(21, 21, 21)
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel14Layout.createSequentialGroup()
                        .add(jButton_iperf_clean_io, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 87, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(12, 12, 12))
                    .add(jPanel14Layout.createSequentialGroup()
                        .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel14Layout.createSequentialGroup()
                                .add(6, 6, 6)
                                .add(jLabel2)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jComboBox_iperf_protocols, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jLabel3)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jComboBox_iperf_durations, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(40, 40, 40)
                                .add(jLabel4)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jComboBox_iperf_bw, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(30, 30, 30)
                                .add(jButton_iperf_start, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton_iperf_stop, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(3, 3, 3))
                            .add(jScrollPane10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE))
                        .add(22, 22, 22))))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel14Layout.createSequentialGroup()
                .add(4, 4, 4)
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jComboBox_iperf_bw, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel4)
                    .add(jComboBox_iperf_durations, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3)
                    .add(jButton_iperf_start, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton_iperf_stop, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel6)
                    .add(jComboBox_iperf_protocols, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 270, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_iperf_clean_io, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(12, 12, 12))
        );

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel14, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("NetTest", jPanel2);

        jTabbedPane1.setSelectedIndex(5);

        jTabbedPane3.addTab("Private OS", jTabbedPane1);

        jPanel16.setBorder(javax.swing.BorderFactory.createTitledBorder("Memory Parameters(Extra OS Modifications Required)"));

        jbl_mem_avai_swap_space_val1.setText("0");

        jbl_mem_total_swap_size_val1.setText("0");

        jbl_mem_total_ram_buffered1.setText("Total Ram Buffered                :");

        jbl_mem_total_ram_shared1.setText("Total Ram Shared                  :");

        jbl_mem_total_ram_val1.setText("0");

        jbl_mem_total_swap_size1.setText("Total Swap Size                     :");

        jbl_mem_total_ram_free_val1.setText("0");

        jbl_mem_total_ram_used_val1.setText("0");

        jbl_mem_total_ram_buffered_val1.setText("0");

        jbl_mem_total_ram_free1.setText("Total Ram Free                      :");

        jbl_mem_total_ram_shared_val1.setText("0");

        jbl_mem_total_ram_used1.setText("Total Ram Used                     :");

        jbl_mem_total_ram1.setText("Total Ram in Machine            :");

        jbl_mem_total_cached_mem1.setText("Total Cached Memory            :");

        jbl_mem_total_cached_mem_val1.setText("0");

        jbl_mem_avai_swap_space1.setText("Available Swap Space            :");

        org.jdesktop.layout.GroupLayout jPanel11_mem_stat1Layout = new org.jdesktop.layout.GroupLayout(jPanel11_mem_stat1);
        jPanel11_mem_stat1.setLayout(jPanel11_mem_stat1Layout);
        jPanel11_mem_stat1Layout.setHorizontalGroup(
            jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11_mem_stat1Layout.createSequentialGroup()
                .add(23, 23, 23)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel11_mem_stat1Layout.createSequentialGroup()
                        .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jbl_mem_avai_swap_space1)
                            .add(jbl_mem_total_swap_size1))
                        .add(35, 35, 35)
                        .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jbl_mem_avai_swap_space_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                            .add(jbl_mem_total_swap_size_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel11_mem_stat1Layout.createSequentialGroup()
                        .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                .add(jbl_mem_total_ram_used1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jbl_mem_total_ram1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(jbl_mem_total_ram_free1)
                            .add(jbl_mem_total_ram_shared1)
                            .add(jbl_mem_total_ram_buffered1)
                            .add(jbl_mem_total_cached_mem1))
                        .add(32, 32, 32)
                        .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jbl_mem_total_cached_mem_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_buffered_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_shared_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_free_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jbl_mem_total_ram_used_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 163, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(0, 278, Short.MAX_VALUE))))
        );
        jPanel11_mem_stat1Layout.setVerticalGroup(
            jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11_mem_stat1Layout.createSequentialGroup()
                .add(30, 30, 30)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_swap_size_val1)
                    .add(jbl_mem_total_swap_size1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_avai_swap_space_val1)
                    .add(jbl_mem_avai_swap_space1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram1)
                    .add(jbl_mem_total_ram_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram_used1)
                    .add(jbl_mem_total_ram_used_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram_free1)
                    .add(jbl_mem_total_ram_free_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram_shared1)
                    .add(jbl_mem_total_ram_shared_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_ram_buffered1)
                    .add(jbl_mem_total_ram_buffered_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11_mem_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_mem_total_cached_mem1)
                    .add(jbl_mem_total_cached_mem_val1))
                .addContainerGap(112, Short.MAX_VALUE))
        );

        jTabbedPane_mem3.addTab("Statistics", jPanel11_mem_stat1);

        jPanel9_mem_graph1.setLayout(new java.awt.BorderLayout());
        jTabbedPane_mem3.addTab("Graphs", jPanel9_mem_graph1);

        org.jdesktop.layout.GroupLayout jPanel16Layout = new org.jdesktop.layout.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane_mem3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel16Layout.createSequentialGroup()
                .add(jTabbedPane_mem3)
                .addContainerGap())
        );

        jPanel17.setBorder(javax.swing.BorderFactory.createTitledBorder("CPU Parameters(Extra OS Modifications Required)"));
        jPanel17.setToolTipText("CPU");

        jPanel18.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder("Known Parameters")));

        jbl_cpu_usercpu_time1.setText("Percentage of User CPU Time       :");

        jbl_cpu_per_user_cpu_val1.setText("0");

        jbl_cpu_raw_user_cpu_val1.setText("0");

        jbl_cpu_per_sys_cpu_val1.setText("0");

        jbl_cpu_raw_sys_cpu_val1.setText("0");

        jbl_cpu_per_idle_cpu_val1.setText("0");

        jbl_cpu_raw_idle_cpu_val1.setText("0");

        jbl_cpu_raw_nice_cpu_val1.setText("0");

        jbl_cpu_raw_nice_cpu1.setText("Raw Nice CPU TIme                      :");

        jbl_cpu_raw_idle_cpu1.setText("Raw Idle CPU Time                       :");

        jbl_cpu_per_idle_cpu1.setText("Percentage of Idle CPU Time        :");

        jbl_cpu_raw_sys_cpu1.setText("Raw System CPU Time                  :");

        jbl_cpu_perc_sys_cpu1.setText("Percentages of System CPU Time :");

        jbl_cpu_raw_user_cpu1.setText("Raw User CPU Time                      :");

        org.jdesktop.layout.GroupLayout jPanel18Layout = new org.jdesktop.layout.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel18Layout.createSequentialGroup()
                        .add(jbl_cpu_usercpu_time1)
                        .add(39, 39, 39)
                        .add(jbl_cpu_per_user_cpu_val1))
                    .add(jPanel18Layout.createSequentialGroup()
                        .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jbl_cpu_raw_user_cpu1)
                            .add(jbl_cpu_perc_sys_cpu1)
                            .add(jbl_cpu_raw_sys_cpu1)
                            .add(jbl_cpu_per_idle_cpu1)
                            .add(jbl_cpu_raw_idle_cpu1)
                            .add(jbl_cpu_raw_nice_cpu1))
                        .add(39, 39, 39)
                        .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jbl_cpu_raw_nice_cpu_val1)
                            .add(jbl_cpu_raw_idle_cpu_val1)
                            .add(jbl_cpu_per_idle_cpu_val1)
                            .add(jbl_cpu_raw_sys_cpu_val1)
                            .add(jbl_cpu_per_sys_cpu_val1)
                            .add(jbl_cpu_raw_user_cpu_val1))))
                .addContainerGap(110, Short.MAX_VALUE))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_usercpu_time1)
                    .add(jbl_cpu_per_user_cpu_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_raw_user_cpu1)
                    .add(jbl_cpu_raw_user_cpu_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_perc_sys_cpu1)
                    .add(jbl_cpu_per_sys_cpu_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_raw_sys_cpu1)
                    .add(jbl_cpu_raw_sys_cpu_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_per_idle_cpu1)
                    .add(jbl_cpu_per_idle_cpu_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_raw_idle_cpu1)
                    .add(jbl_cpu_raw_idle_cpu_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel18Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_raw_nice_cpu1)
                    .add(jbl_cpu_raw_nice_cpu_val1))
                .addContainerGap(93, Short.MAX_VALUE))
        );

        jPanel19.setBorder(javax.swing.BorderFactory.createTitledBorder("CPU 1, 5, 15 min Load"));

        jbl_cpu_1min_load1.setText("1min:");

        jbl_cpu_1min_load_val1.setText("0");

        jbl_cpu_5min_load1.setText("5min:");

        jbl_cpu_5min_load_val1.setText("0");

        jbl_cpu_15min_load1.setText("15min:");

        jbl_cpu_15min_load_val1.setText("0");

        org.jdesktop.layout.GroupLayout jPanel19Layout = new org.jdesktop.layout.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel19Layout.createSequentialGroup()
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel19Layout.createSequentialGroup()
                        .add(40, 40, 40)
                        .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel19Layout.createSequentialGroup()
                                .add(jbl_cpu_5min_load1)
                                .add(18, 18, 18)
                                .add(jbl_cpu_5min_load_val1))
                            .add(jPanel19Layout.createSequentialGroup()
                                .add(jbl_cpu_1min_load1)
                                .add(18, 18, 18)
                                .add(jbl_cpu_1min_load_val1))))
                    .add(jPanel19Layout.createSequentialGroup()
                        .add(34, 34, 34)
                        .add(jbl_cpu_15min_load1)
                        .add(18, 18, 18)
                        .add(jbl_cpu_15min_load_val1)))
                .addContainerGap(165, Short.MAX_VALUE))
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_1min_load1)
                    .add(jbl_cpu_1min_load_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_5min_load1)
                    .add(jbl_cpu_5min_load_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel19Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jbl_cpu_15min_load1)
                    .add(jbl_cpu_15min_load_val1))
                .addContainerGap(183, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel11_cpu_stat1Layout = new org.jdesktop.layout.GroupLayout(jPanel11_cpu_stat1);
        jPanel11_cpu_stat1.setLayout(jPanel11_cpu_stat1Layout);
        jPanel11_cpu_stat1Layout.setHorizontalGroup(
            jPanel11_cpu_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11_cpu_stat1Layout.createSequentialGroup()
                .add(jPanel18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel19, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel11_cpu_stat1Layout.setVerticalGroup(
            jPanel11_cpu_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11_cpu_stat1Layout.createSequentialGroup()
                .add(jPanel11_cpu_stat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel19, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(0, 0, 0))
        );

        jTabbedPane_cpu1.addTab("Statistics", jPanel11_cpu_stat1);

        jPanel9_cpu_graph1.setLayout(new java.awt.BorderLayout());
        jTabbedPane_cpu1.addTab("Graph", jPanel9_cpu_graph1);

        org.jdesktop.layout.GroupLayout jPanel17Layout = new org.jdesktop.layout.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane_cpu1)
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel17Layout.createSequentialGroup()
                .add(jTabbedPane_cpu1)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel_cpumem1Layout = new org.jdesktop.layout.GroupLayout(jPanel_cpumem1);
        jPanel_cpumem1.setLayout(jPanel_cpumem1Layout);
        jPanel_cpumem1Layout.setHorizontalGroup(
            jPanel_cpumem1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_cpumem1Layout.createSequentialGroup()
                .add(jPanel_cpumem1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel17, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel_cpumem1Layout.setVerticalGroup(
            jPanel_cpumem1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_cpumem1Layout.createSequentialGroup()
                .add(jPanel17, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane4.addTab("CPU&Memory", jPanel_cpumem1);

        jPanel_net_interfaces1.setPreferredSize(new java.awt.Dimension(537, 600));

        jPanel20.setBorder(javax.swing.BorderFactory.createTitledBorder("Dynamically Traffic Change in Network Interfaces"));
        jPanel20.setMaximumSize(new java.awt.Dimension(625, 314));

        jPanel14_ni1.setBorder(javax.swing.BorderFactory.createTitledBorder("Network Int"));
        jPanel14_ni1.setMinimumSize(new java.awt.Dimension(120, 300));
        jPanel14_ni1.setLayout(new java.awt.BorderLayout());

        jList1_network_interfaces1.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Interface 1", "Interface 2" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList1_network_interfaces1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jList1_network_interfaces1.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jList1_network_interfaces1drawGraph(evt);
            }
        });
        jScrollPane9.setViewportView(jList1_network_interfaces1);

        jPanel14_ni1.add(jScrollPane9, java.awt.BorderLayout.CENTER);

        jPanel15_selcted_nic_graph1.setBorder(javax.swing.BorderFactory.createTitledBorder("Realtime Traffic Variation in Selected NI"));
        jPanel15_selcted_nic_graph1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel15_selcted_nic_graph1.setMinimumSize(new java.awt.Dimension(480, 300));
        jPanel15_selcted_nic_graph1.setPreferredSize(new java.awt.Dimension(680, 500));
        jPanel15_selcted_nic_graph1.setLayout(new java.awt.BorderLayout());

        org.jdesktop.layout.GroupLayout jPanel20Layout = new org.jdesktop.layout.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel20Layout.createSequentialGroup()
                .add(jPanel14_ni1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel15_selcted_nic_graph1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel14_ni1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel15_selcted_nic_graph1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );

        jPanel21.setBorder(javax.swing.BorderFactory.createTitledBorder("All NI Current Values"));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Network Interfaces", "Inoming Packets", "Outgoing Packets"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable2.setShowGrid(true);
        jScrollPane11.setViewportView(jTable2);

        org.jdesktop.layout.GroupLayout jPanel21Layout = new org.jdesktop.layout.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
            jPanel21Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 711, Short.MAX_VALUE)
        );
        jPanel21Layout.setVerticalGroup(
            jPanel21Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel21Layout.createSequentialGroup()
                .add(jScrollPane11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel_net_interfaces1Layout = new org.jdesktop.layout.GroupLayout(jPanel_net_interfaces1);
        jPanel_net_interfaces1.setLayout(jPanel_net_interfaces1Layout);
        jPanel_net_interfaces1Layout.setHorizontalGroup(
            jPanel_net_interfaces1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel20, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel21, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel_net_interfaces1Layout.setVerticalGroup(
            jPanel_net_interfaces1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_net_interfaces1Layout.createSequentialGroup()
                .add(jPanel20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel21, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(17, 17, 17))
        );

        jTabbedPane4.addTab("Network Int", jPanel_net_interfaces1);

        jPanel23.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 13), new java.awt.Color(255, 255, 255))); // NOI18N

        jLabel_ip_received1.setText("Received IP Packets            :");

        jLabel_ip_delivered1.setText(" Delivered IP Packets           :");

        jLabel_ip_requested1.setText("Requested IP Packets           :");

        jLabel_ip_received_val1.setText("0");

        jLabel_ip_delivered_val1.setText("0");

        jLabel_ip_requested_val1.setText("0");

        org.jdesktop.layout.GroupLayout jPanel23Layout = new org.jdesktop.layout.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
            jPanel23Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel23Layout.createSequentialGroup()
                .add(jPanel23Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel23Layout.createSequentialGroup()
                        .add(28, 28, 28)
                        .add(jLabel_ip_received1))
                    .add(jPanel23Layout.createSequentialGroup()
                        .add(25, 25, 25)
                        .add(jPanel23Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel_ip_delivered1)
                            .add(jLabel_ip_requested1))))
                .add(41, 41, 41)
                .add(jPanel23Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_ip_delivered_val1)
                    .add(jLabel_ip_received_val1)
                    .add(jLabel_ip_requested_val1))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel23Layout.setVerticalGroup(
            jPanel23Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel23Layout.createSequentialGroup()
                .add(32, 32, 32)
                .add(jPanel23Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel23Layout.createSequentialGroup()
                        .add(jLabel_ip_received_val1)
                        .add(12, 12, 12)
                        .add(jLabel_ip_delivered_val1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jLabel_ip_requested_val1))
                    .add(jPanel23Layout.createSequentialGroup()
                        .add(jLabel_ip_received1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jLabel_ip_delivered1)
                        .add(12, 12, 12)
                        .add(jLabel_ip_requested1)))
                .add(0, 260, Short.MAX_VALUE))
        );

        jPanel24.setBorder(javax.swing.BorderFactory.createTitledBorder("Connected Users"));

        jTable_connected_users1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Number", "IP Address", "MAC Address"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane12.setViewportView(jTable_connected_users1);

        org.jdesktop.layout.GroupLayout jPanel24Layout = new org.jdesktop.layout.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(
            jPanel24Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel24Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 672, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel24Layout.setVerticalGroup(
            jPanel24Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 293, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );

        org.jdesktop.layout.GroupLayout jPanel22Layout = new org.jdesktop.layout.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(
            jPanel22Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel22Layout.createSequentialGroup()
                .add(jPanel22Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel24, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel23, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel22Layout.setVerticalGroup(
            jPanel22Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel22Layout.createSequentialGroup()
                .add(jPanel23, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel24, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(18, 18, 18))
        );

        jTabbedPane5.addTab("IP", jPanel22);

        jLabel_in_udp_data1.setText("Incoming Datagrams   :");

        jLabel_out_udp_data1.setText("Outgoing Datagrams   :");

        jLabel_in_udp_data_val1.setText("0");

        jLabel_out_udp_data_val1.setText("0");

        jLabel_udp_no_port1.setText("UDP No Ports               :");

        jLabel_udp_no_port_val1.setText("NULL");

        jLabel_udp_local_ports1.setText("UDP Local Ports           :");

        jLabel_udp_local_pots_cal1.setText("NULL");

        org.jdesktop.layout.GroupLayout jPanel25Layout = new org.jdesktop.layout.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
            jPanel25Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel25Layout.createSequentialGroup()
                .add(40, 40, 40)
                .add(jPanel25Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_in_udp_data1)
                    .add(jLabel_out_udp_data1)
                    .add(jLabel_udp_no_port1)
                    .add(jLabel_udp_local_ports1))
                .add(18, 18, 18)
                .add(jPanel25Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_udp_local_pots_cal1)
                    .add(jLabel_udp_no_port_val1)
                    .add(jLabel_out_udp_data_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel_in_udp_data_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE))
                .addContainerGap(195, Short.MAX_VALUE))
        );
        jPanel25Layout.setVerticalGroup(
            jPanel25Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel25Layout.createSequentialGroup()
                .add(jPanel25Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel25Layout.createSequentialGroup()
                        .add(35, 35, 35)
                        .add(jLabel_in_udp_data_val1))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel25Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel_in_udp_data1)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel25Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_out_udp_data1)
                    .add(jLabel_out_udp_data_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel25Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_udp_no_port1)
                    .add(jLabel_udp_no_port_val1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel25Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_udp_local_ports1)
                    .add(jLabel_udp_local_pots_cal1))
                .addContainerGap(586, Short.MAX_VALUE))
        );

        jTabbedPane5.addTab("UDP", jPanel25);

        jLabel_in_tcp_seg1.setText("TCP Incoming Segments       :");

        jLabel_out_tcp_seg1.setText("TCP Outcoming Segments    :");

        jLabel_in_tcp_seg_val1.setText("0");

        jLabel_out_tcp_seg_val1.setText("0");

        org.jdesktop.layout.GroupLayout jPanel26Layout = new org.jdesktop.layout.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(
            jPanel26Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel26Layout.createSequentialGroup()
                .add(37, 37, 37)
                .add(jPanel26Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel26Layout.createSequentialGroup()
                        .add(jLabel_out_tcp_seg1)
                        .add(18, 18, 18)
                        .add(jLabel_out_tcp_seg_val1))
                    .add(jPanel26Layout.createSequentialGroup()
                        .add(jLabel_in_tcp_seg1)
                        .add(18, 18, 18)
                        .add(jLabel_in_tcp_seg_val1)))
                .addContainerGap(455, Short.MAX_VALUE))
        );
        jPanel26Layout.setVerticalGroup(
            jPanel26Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel26Layout.createSequentialGroup()
                .add(36, 36, 36)
                .add(jPanel26Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_in_tcp_seg1)
                    .add(jLabel_in_tcp_seg_val1))
                .add(18, 18, 18)
                .add(jPanel26Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_out_tcp_seg1)
                    .add(jLabel_out_tcp_seg_val1))
                .addContainerGap(635, Short.MAX_VALUE))
        );

        jTabbedPane5.addTab("TCP", jPanel26);

        jLabel_in_tcp_mes1.setText("ICMP Outgoing Messages       :");

        jLabel_in_icmp_mes1.setText("ICMP Incoming Messages       :");

        jLabel_in_icmp_mes_val1.setText("0");

        jLabel_out_icmp_mes_val1.setText("0");

        org.jdesktop.layout.GroupLayout jPanel27Layout = new org.jdesktop.layout.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel27Layout.createSequentialGroup()
                .add(37, 37, 37)
                .add(jPanel27Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_in_icmp_mes1)
                    .add(jLabel_in_tcp_mes1))
                .add(26, 26, 26)
                .add(jPanel27Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel27Layout.createSequentialGroup()
                        .add(jLabel_in_icmp_mes_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                        .add(8, 8, 8))
                    .add(jLabel_out_icmp_mes_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE))
                .add(190, 190, 190))
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel27Layout.createSequentialGroup()
                .add(38, 38, 38)
                .add(jPanel27Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_in_icmp_mes1)
                    .add(jLabel_in_icmp_mes_val1))
                .add(18, 18, 18)
                .add(jPanel27Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_in_tcp_mes1)
                    .add(jLabel_out_icmp_mes_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(633, Short.MAX_VALUE))
        );

        jTabbedPane5.addTab("ICMP", jPanel27);

        org.jdesktop.layout.GroupLayout jPanel_prostat1Layout = new org.jdesktop.layout.GroupLayout(jPanel_prostat1);
        jPanel_prostat1.setLayout(jPanel_prostat1Layout);
        jPanel_prostat1Layout.setHorizontalGroup(
            jPanel_prostat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane5)
        );
        jPanel_prostat1Layout.setVerticalGroup(
            jPanel_prostat1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jTabbedPane5)
        );

        jTabbedPane4.addTab("Protocol Stat", jPanel_prostat1);

        jLabel_sys_uptime1.setText("System Uptime         :");

        jLabel_sys_date1.setText("System Date            :");

        jLabel_sys_uptime_val1.setText("0");

        jLabel_sys_date_val1.setText("0");

        org.jdesktop.layout.GroupLayout jPanel_hostinfo1Layout = new org.jdesktop.layout.GroupLayout(jPanel_hostinfo1);
        jPanel_hostinfo1.setLayout(jPanel_hostinfo1Layout);
        jPanel_hostinfo1Layout.setHorizontalGroup(
            jPanel_hostinfo1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_hostinfo1Layout.createSequentialGroup()
                .add(83, 83, 83)
                .add(jPanel_hostinfo1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel_sys_date1)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel_sys_uptime1))
                .add(18, 18, 18)
                .add(jPanel_hostinfo1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_sys_uptime_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                    .add(jLabel_sys_date_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .add(168, 168, 168))
        );
        jPanel_hostinfo1Layout.setVerticalGroup(
            jPanel_hostinfo1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_hostinfo1Layout.createSequentialGroup()
                .add(40, 40, 40)
                .add(jPanel_hostinfo1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_sys_uptime1)
                    .add(jLabel_sys_uptime_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_hostinfo1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_sys_date1)
                    .add(jLabel_sys_date_val1))
                .addContainerGap(689, Short.MAX_VALUE))
        );

        jTabbedPane4.addTab("HostInfo", jPanel_hostinfo1);

        jButton_send_oid_request1.setText("Send Request");
        jButton_send_oid_request1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_send_oid_request1ActionPerformed(evt);
            }
        });

        jTextArea_snmpreq_info1.setEditable(false);
        jTextArea_snmpreq_info1.setColumns(20);
        jTextArea_snmpreq_info1.setRows(5);
        jTextArea_snmpreq_info1.setText("    \n    In this section you can easily request much more information by inserting \n    Object Identifiers (OID)s. You can send more than one request at each\n    time.The response of these requests will be viewed below.");
        jScrollPane14.setViewportView(jTextArea_snmpreq_info1);

        jLabel7.setText("Output of the requested OID");

        jTextArea_snmp_req_output1.setEditable(false);
        jTextArea_snmp_req_output1.setColumns(20);
        jTextArea_snmp_req_output1.setRows(5);
        jScrollPane15.setViewportView(jTextArea_snmp_req_output1);

        jLabel_snmp_req_info1.setText("INFO:");

        org.jdesktop.layout.GroupLayout jPanel_snmpreq1Layout = new org.jdesktop.layout.GroupLayout(jPanel_snmpreq1);
        jPanel_snmpreq1.setLayout(jPanel_snmpreq1Layout);
        jPanel_snmpreq1Layout.setHorizontalGroup(
            jPanel_snmpreq1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_snmpreq1Layout.createSequentialGroup()
                .add(48, 48, 48)
                .add(jPanel_snmpreq1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel_snmpreq1Layout.createSequentialGroup()
                        .add(jLabel7)
                        .add(0, 496, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_snmpreq1Layout.createSequentialGroup()
                        .add(jPanel_snmpreq1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jPanel_snmpreq1Layout.createSequentialGroup()
                                .add(6, 6, 6)
                                .add(jLabel_snmp_req_info1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jLabel_snmp_req_info_val1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane15)
                            .add(jPanel_snmpreq1Layout.createSequentialGroup()
                                .add(0, 0, Short.MAX_VALUE)
                                .add(jButton_send_oid_request1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 128, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane14)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jTextField_oid_number1))
                        .add(50, 50, 50))))
        );
        jPanel_snmpreq1Layout.setVerticalGroup(
            jPanel_snmpreq1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_snmpreq1Layout.createSequentialGroup()
                .add(18, 18, 18)
                .add(jScrollPane14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 106, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_snmpreq1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_snmp_req_info1)
                    .add(jLabel_snmp_req_info_val1))
                .add(30, 30, 30)
                .add(jTextField_oid_number1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_send_oid_request1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel7)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jScrollPane15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 468, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(33, 33, 33))
        );

        jTabbedPane4.addTab("Request", jPanel_snmpreq1);

        jPanel29.setBorder(javax.swing.BorderFactory.createTitledBorder("Ping Test"));

        jButton_ping_start1.setText("Start Ping Test");
        jButton_ping_start1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ping_start1ActionPerformed(evt);
            }
        });

        jButton_ping_stop1.setText("Stop Ping Test");
        jButton_ping_stop1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ping_stop1ActionPerformed(evt);
            }
        });

        jLabel8.setText("IP Address:");

        jTextField_ping_ipaddress1.setText("192.168.127.1");
        jTextField_ping_ipaddress1.setPreferredSize(new java.awt.Dimension(90, 30));

        jTextArea_ping_output_publicOs.setColumns(20);
        jTextArea_ping_output_publicOs.setRows(5);
        jScrollPane17.setViewportView(jTextArea_ping_output_publicOs);

        jButton_ping_clean_io1.setText("Clean");
        jButton_ping_clean_io1.setMaximumSize(new java.awt.Dimension(79, 35));
        jButton_ping_clean_io1.setPreferredSize(new java.awt.Dimension(79, 30));
        jButton_ping_clean_io1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ping_clean_io1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel29Layout = new org.jdesktop.layout.GroupLayout(jPanel29);
        jPanel29.setLayout(jPanel29Layout);
        jPanel29Layout.setHorizontalGroup(
            jPanel29Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel29Layout.createSequentialGroup()
                .add(jPanel29Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel29Layout.createSequentialGroup()
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jButton_ping_clean_io1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 94, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jPanel29Layout.createSequentialGroup()
                        .add(21, 21, 21)
                        .add(jPanel29Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane17)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel29Layout.createSequentialGroup()
                                .add(jLabel8)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jTextField_ping_ipaddress1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(48, 48, 48)
                                .add(jButton_ping_start1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 124, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton_ping_stop1)))))
                .add(21, 21, 21))
        );
        jPanel29Layout.setVerticalGroup(
            jPanel29Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel29Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel29Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE, false)
                    .add(jLabel8)
                    .add(jButton_ping_start1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jButton_ping_stop1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jTextField_ping_ipaddress1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jScrollPane17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 242, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jButton_ping_clean_io1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(21, 21, 21))
        );

        jPanel30.setBorder(javax.swing.BorderFactory.createTitledBorder("Iperf Test"));

        jComboBox_iperf_protocols1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TCP", "UDP" }));
        jComboBox_iperf_protocols1.setPreferredSize(new java.awt.Dimension(82, 35));
        jComboBox_iperf_protocols1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox_iperf_protocols1ActionPerformed(evt);
            }
        });

        jLabel9.setText("Pro.:");

        jLabel10.setText("Dur.:");

        jComboBox_iperf_durations1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "5", "10", "20", "40", "60", "120", "240", "360" }));
        jComboBox_iperf_durations1.setPreferredSize(new java.awt.Dimension(100, 35));

        jLabel11.setText("Bw:");

        jComboBox_iperf_bw1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1m", "3m", "5m", "7m", "9m", "15m", "20m" }));
        jComboBox_iperf_bw1.setPreferredSize(new java.awt.Dimension(72, 35));

        jButton_iperf_start1.setText("Start");
        jButton_iperf_start1.setMinimumSize(new java.awt.Dimension(75, 35));
        jButton_iperf_start1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_iperf_start1ActionPerformed(evt);
            }
        });

        jButton_iperf_stop1.setText("Stop");
        jButton_iperf_stop1.setMinimumSize(new java.awt.Dimension(75, 35));
        jButton_iperf_stop1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_iperf_stop1ActionPerformed(evt);
            }
        });

        jButton_iperf_clean_io1.setText("Clean");
        jButton_iperf_clean_io1.setPreferredSize(new java.awt.Dimension(79, 30));
        jButton_iperf_clean_io1.setSize(new java.awt.Dimension(97, 30));
        jButton_iperf_clean_io1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_iperf_clean_io1ActionPerformed(evt);
            }
        });

        jLabel12.setText("s");

        jTextArea_iperf_output_publicOs.setColumns(20);
        jTextArea_iperf_output_publicOs.setRows(5);
        jScrollPane18.setViewportView(jTextArea_iperf_output_publicOs);

        org.jdesktop.layout.GroupLayout jPanel30Layout = new org.jdesktop.layout.GroupLayout(jPanel30);
        jPanel30.setLayout(jPanel30Layout);
        jPanel30Layout.setHorizontalGroup(
            jPanel30Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel30Layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(jPanel30Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jScrollPane18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 665, Short.MAX_VALUE)
                    .add(jPanel30Layout.createSequentialGroup()
                        .add(jLabel9)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jComboBox_iperf_protocols1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(jLabel10)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jComboBox_iperf_durations1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(32, 32, 32)
                        .add(jLabel11)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jComboBox_iperf_bw1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(39, 39, 39)
                        .add(jButton_iperf_start1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton_iperf_stop1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jButton_iperf_clean_io1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(26, 26, 26))
        );
        jPanel30Layout.setVerticalGroup(
            jPanel30Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel30Layout.createSequentialGroup()
                .add(4, 4, 4)
                .add(jPanel30Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jComboBox_iperf_bw1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel11)
                    .add(jComboBox_iperf_durations1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel10)
                    .add(jButton_iperf_start1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel12)
                    .add(jComboBox_iperf_protocols1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel9)
                    .add(jButton_iperf_stop1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_iperf_clean_io1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel28Layout = new org.jdesktop.layout.GroupLayout(jPanel28);
        jPanel28.setLayout(jPanel28Layout);
        jPanel28Layout.setHorizontalGroup(
            jPanel28Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel29, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel30, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel28Layout.setVerticalGroup(
            jPanel28Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel28Layout.createSequentialGroup()
                .add(jPanel29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 369, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        jTabbedPane4.addTab("NetTest", jPanel28);

        jTabbedPane3.addTab("Public OS", jTabbedPane4);

        jPanel_control_interface_panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Control Interface"));

        jLabel13.setText("User ID :");

        jScrollPane3.setViewportView(jList_ci_usevios_enabled_users);

        jButton_ci_add_service.setText("Add Service");
        jButton_ci_add_service.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ci_add_serviceActionPerformed(evt);
            }
        });

        jButton_ci_remove_service.setText("Remove Service");
        jButton_ci_remove_service.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ci_remove_serviceActionPerformed(evt);
            }
        });

        jLabel14.setText("uSeViOS enabled Users ");

        jTextArea1.setEditable(false);
        jTextArea1.setBackground(new java.awt.Color(237, 237, 237));
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setText("*Please fill the userID text field by providing\nuser identifier and user's MAC address \ne.g. userID,MAC");
        jTextArea1.setBorder(null);
        jScrollPane7.setViewportView(jTextArea1);

        jLabel_ci_errors.setForeground(new java.awt.Color(204, 0, 0));

        org.jdesktop.layout.GroupLayout jPanel_control_interface_panelLayout = new org.jdesktop.layout.GroupLayout(jPanel_control_interface_panel);
        jPanel_control_interface_panel.setLayout(jPanel_control_interface_panelLayout);
        jPanel_control_interface_panelLayout.setHorizontalGroup(
            jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panelLayout.createSequentialGroup()
                .add(17, 17, 17)
                .add(jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panelLayout.createSequentialGroup()
                        .add(jTextField_userID_control_interface, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 250, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jButton_ci_add_service, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jButton_ci_remove_service, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE))
                        .add(12, 12, 12))
                    .add(jPanel_control_interface_panelLayout.createSequentialGroup()
                        .add(jLabel13)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(jPanel_control_interface_panelLayout.createSequentialGroup()
                        .add(jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 289, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel_ci_errors))
                        .add(0, 0, Short.MAX_VALUE)))
                .add(jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panelLayout.createSequentialGroup()
                        .add(6, 6, 6)
                        .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel14))
                .add(40, 40, 40))
        );
        jPanel_control_interface_panelLayout.setVerticalGroup(
            jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panelLayout.createSequentialGroup()
                .add(jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel_control_interface_panelLayout.createSequentialGroup()
                        .add(31, 31, 31)
                        .add(jLabel13)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jTextField_userID_control_interface, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jButton_ci_add_service))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel_control_interface_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jButton_ci_remove_service)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panelLayout.createSequentialGroup()
                                .add(0, 0, Short.MAX_VALUE)
                                .add(jLabel_ci_errors)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                    .add(jPanel_control_interface_panelLayout.createSequentialGroup()
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jLabel14)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 204, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .add(43, 43, 43))
        );

        jPanel_control_interface_panel1.setBorder(javax.swing.BorderFactory.createTitledBorder("MAC-VPN IP Match Management"));

        jLabel15.setText("MAC:");

        jList_ci_usevios_mac_vpnip.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane13.setViewportView(jList_ci_usevios_mac_vpnip);

        jButton_ci_add_mac_vpnip.setText("Add Tuple");
        jButton_ci_add_mac_vpnip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ci_add_mac_vpnipActionPerformed(evt);
            }
        });

        jButton_ci_remove_mac_vpnip.setText("Remove Tuple");
        jButton_ci_remove_mac_vpnip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_ci_remove_mac_vpnipActionPerformed(evt);
            }
        });

        jLabel16.setText("uSeViOS  MAC-VPN Match IP List");

        jTextArea2.setEditable(false);
        jTextArea2.setBackground(new java.awt.Color(237, 237, 237));
        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jTextArea2.setText("*Please fill the MAC-IP text field by providing \nuser's MAC address and user's VPN IP.");
        jTextArea2.setAutoscrolls(false);
        jTextArea2.setBorder(null);
        jScrollPane21.setViewportView(jTextArea2);

        jLabel_ci_errors1.setForeground(new java.awt.Color(204, 0, 0));

        jLabel17.setText("VPN-IP:");

        org.jdesktop.layout.GroupLayout jPanel_control_interface_panel1Layout = new org.jdesktop.layout.GroupLayout(jPanel_control_interface_panel1);
        jPanel_control_interface_panel1.setLayout(jPanel_control_interface_panel1Layout);
        jPanel_control_interface_panel1Layout.setHorizontalGroup(
            jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panel1Layout.createSequentialGroup()
                .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel_control_interface_panel1Layout.createSequentialGroup()
                        .add(17, 17, 17)
                        .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel17)
                            .add(jLabel15))
                        .add(18, 18, 18)
                        .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jTextField_control_interface_MAC, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 235, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panel1Layout.createSequentialGroup()
                                .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(jButton_ci_add_mac_vpnip)
                                    .add(jTextField_control_interface_vpnip))
                                .add(1, 1, 1)))
                        .add(64, 64, 64))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel_control_interface_panel1Layout.createSequentialGroup()
                        .add(26, 26, 26)
                        .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel_ci_errors1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 47, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jScrollPane21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 307, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(56, 56, 56)))
                .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel_control_interface_panel1Layout.createSequentialGroup()
                        .add(63, 63, 63)
                        .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jButton_ci_remove_mac_vpnip, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jScrollPane13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE))
                        .add(31, 31, 31))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panel1Layout.createSequentialGroup()
                        .add(jLabel16)
                        .add(44, 44, 44))))
        );
        jPanel_control_interface_panel1Layout.setVerticalGroup(
            jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel16)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane13)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_ci_remove_mac_vpnip)
                .add(41, 41, 41))
            .add(jPanel_control_interface_panel1Layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jTextField_control_interface_MAC, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel15))
                .add(8, 8, 8)
                .add(jPanel_control_interface_panel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel17)
                    .add(jTextField_control_interface_vpnip, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_ci_add_mac_vpnip)
                .add(18, 18, 18)
                .add(jLabel_ci_errors1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(jScrollPane21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(49, 49, 49))
        );

        jPanel31.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel18.setText("Refresh Latest uSeViOs Enabled Users and MAC-VPN IP Match List");

        jButton1.setText("Refresh Lists");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel31Layout = new org.jdesktop.layout.GroupLayout(jPanel31);
        jPanel31.setLayout(jPanel31Layout);
        jPanel31Layout.setHorizontalGroup(
            jPanel31Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel31Layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(jLabel18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 550, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jButton1)
                .addContainerGap())
        );
        jPanel31Layout.setVerticalGroup(
            jPanel31Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel31Layout.createSequentialGroup()
                .add(9, 9, 9)
                .add(jPanel31Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel18)
                    .add(jButton1))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel_control_interfaceLayout = new org.jdesktop.layout.GroupLayout(jPanel_control_interface);
        jPanel_control_interface.setLayout(jPanel_control_interfaceLayout);
        jPanel_control_interfaceLayout.setHorizontalGroup(
            jPanel_control_interfaceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_control_interface_panel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel_control_interfaceLayout.createSequentialGroup()
                .add(jPanel31, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(2, 2, 2))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_control_interface_panel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel_control_interfaceLayout.setVerticalGroup(
            jPanel_control_interfaceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_control_interfaceLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel31, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(7, 7, 7)
                .add(jPanel_control_interface_panel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 281, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_control_interface_panel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(133, Short.MAX_VALUE))
        );

        jTabbedPane3.addTab("Control Interface", jPanel_control_interface);

        jPanel32.setBorder(javax.swing.BorderFactory.createTitledBorder("Console Outputs"));

        jTextArea_console_outputs.setColumns(20);
        jTextArea_console_outputs.setRows(5);
        jScrollPane20.setViewportView(jTextArea_console_outputs);

        jButton_console_outputs.setText("Clean");
        jButton_console_outputs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_console_outputsActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel32Layout = new org.jdesktop.layout.GroupLayout(jPanel32);
        jPanel32.setLayout(jPanel32Layout);
        jPanel32Layout.setHorizontalGroup(
            jPanel32Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane20, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 732, Short.MAX_VALUE)
            .add(jPanel32Layout.createSequentialGroup()
                .add(0, 0, Short.MAX_VALUE)
                .add(jButton_console_outputs))
        );
        jPanel32Layout.setVerticalGroup(
            jPanel32Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel32Layout.createSequentialGroup()
                .add(jScrollPane20, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 635, Short.MAX_VALUE)
                .add(109, 109, 109)
                .add(jButton_console_outputs))
        );

        org.jdesktop.layout.GroupLayout jPanel_console_outputsLayout = new org.jdesktop.layout.GroupLayout(jPanel_console_outputs);
        jPanel_console_outputs.setLayout(jPanel_console_outputsLayout);
        jPanel_console_outputsLayout.setHorizontalGroup(
            jPanel_console_outputsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel32, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel_console_outputsLayout.setVerticalGroup(
            jPanel_console_outputsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_console_outputsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel32, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane3.addTab("Console Outputs", jPanel_console_outputs);

        jPanel_inner_conf1.setBorder(javax.swing.BorderFactory.createTitledBorder("Private OS"));

        jButton_stop_snmp2.setText("Stop Server");
        jButton_stop_snmp2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stop_snmp_private(evt);
            }
        });

        jButton_start_snmp2.setText("Start Server");
        jButton_start_snmp2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                start_snmp_private(evt);
            }
        });

        jTextField_ipaddress_val.setText("192.168.126.1");
        jTextField_ipaddress_val.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField_ipaddress_valActionPerformed(evt);
            }
        });

        jPanel_snmp_versions2.setBorder(javax.swing.BorderFactory.createTitledBorder("Request Version"));

        buttonGroup1.add(jRadioButton_snmp_v1_privateOS);
        jRadioButton_snmp_v1_privateOS.setSelected(true);
        jRadioButton_snmp_v1_privateOS.setText("v1");
        jRadioButton_snmp_v1_privateOS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                enableSNMPv1_private(evt);
            }
        });

        buttonGroup1.add(jRadioButton_snmpv2_privateOS);
        jRadioButton_snmpv2_privateOS.setText("v2");
        jRadioButton_snmpv2_privateOS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                enableSNMPV2_private(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel_snmp_versions2Layout = new org.jdesktop.layout.GroupLayout(jPanel_snmp_versions2);
        jPanel_snmp_versions2.setLayout(jPanel_snmp_versions2Layout);
        jPanel_snmp_versions2Layout.setHorizontalGroup(
            jPanel_snmp_versions2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_snmp_versions2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jRadioButton_snmp_v1_privateOS, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 52, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jRadioButton_snmpv2_privateOS, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_snmp_versions2Layout.setVerticalGroup(
            jPanel_snmp_versions2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_snmp_versions2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(jRadioButton_snmpv2_privateOS)
                .add(jRadioButton_snmp_v1_privateOS))
        );

        jLabel_ipaddress2.setText("IP Address :");

        jTextArea_privateOS.setColumns(20);
        jTextArea_privateOS.setRows(5);
        jScrollPane16.setViewportView(jTextArea_privateOS);

        jButton_clean_privateOS.setText("Clean");
        jButton_clean_privateOS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_clean_privateOSActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel_inner_conf1Layout = new org.jdesktop.layout.GroupLayout(jPanel_inner_conf1);
        jPanel_inner_conf1.setLayout(jPanel_inner_conf1Layout);
        jPanel_inner_conf1Layout.setHorizontalGroup(
            jPanel_inner_conf1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 732, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_inner_conf1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel_ipaddress2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 86, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jTextField_ipaddress_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 137, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_start_snmp2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 86, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_stop_snmp2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 84, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_clean_privateOS, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 62, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_snmp_versions2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );
        jPanel_inner_conf1Layout.setVerticalGroup(
            jPanel_inner_conf1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_inner_conf1Layout.createSequentialGroup()
                .add(jPanel_inner_conf1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel_snmp_versions2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_inner_conf1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jButton_clean_privateOS, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 44, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jButton_stop_snmp2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 44, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jButton_start_snmp2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 44, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jTextField_ipaddress_val, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jLabel_ipaddress2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jScrollPane16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
                .add(13, 13, 13))
        );

        jPanel_inner_conf2.setBorder(javax.swing.BorderFactory.createTitledBorder("Public OS"));

        jButton_stop_snmp3.setText("Stop Server");
        jButton_stop_snmp3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stop_snmp_public(evt);
            }
        });

        jButton_start_snmp3.setText("Start Server");
        jButton_start_snmp3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                start_snmp_public(evt);
            }
        });

        jTextField_ipaddress_val1.setText("192.168.127.1");

        jPanel_snmp_versions3.setBorder(javax.swing.BorderFactory.createTitledBorder("Request Version"));

        buttonGroup2.add(jRadioButton_snmpv1_publicOS);
        jRadioButton_snmpv1_publicOS.setSelected(true);
        jRadioButton_snmpv1_publicOS.setText("v1");
        jRadioButton_snmpv1_publicOS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                enableSNMPv1_public(evt);
            }
        });

        buttonGroup2.add(jRadioButton_snmpv2_publicOS);
        jRadioButton_snmpv2_publicOS.setText("v2");
        jRadioButton_snmpv2_publicOS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                enableSNMPV2_public(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel_snmp_versions3Layout = new org.jdesktop.layout.GroupLayout(jPanel_snmp_versions3);
        jPanel_snmp_versions3.setLayout(jPanel_snmp_versions3Layout);
        jPanel_snmp_versions3Layout.setHorizontalGroup(
            jPanel_snmp_versions3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_snmp_versions3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jRadioButton_snmpv1_publicOS, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jRadioButton_snmpv2_publicOS, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_snmp_versions3Layout.setVerticalGroup(
            jPanel_snmp_versions3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_snmp_versions3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(jRadioButton_snmpv2_publicOS)
                .add(jRadioButton_snmpv1_publicOS))
        );

        jLabel_ipaddress3.setText("IP Address :");

        jTextArea_publicOs.setColumns(20);
        jTextArea_publicOs.setRows(5);
        jScrollPane19.setViewportView(jTextArea_publicOs);

        jButton_clean_publicOS.setText("Clean");
        jButton_clean_publicOS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_clean_publicOSActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel_inner_conf2Layout = new org.jdesktop.layout.GroupLayout(jPanel_inner_conf2);
        jPanel_inner_conf2.setLayout(jPanel_inner_conf2Layout);
        jPanel_inner_conf2Layout.setHorizontalGroup(
            jPanel_inner_conf2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane19)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_inner_conf2Layout.createSequentialGroup()
                .add(0, 0, Short.MAX_VALUE)
                .add(jLabel_ipaddress3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 81, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(jTextField_ipaddress_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 142, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_start_snmp3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 81, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_stop_snmp3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 82, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton_clean_publicOS, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_snmp_versions3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );
        jPanel_inner_conf2Layout.setVerticalGroup(
            jPanel_inner_conf2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_inner_conf2Layout.createSequentialGroup()
                .add(jPanel_inner_conf2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel_snmp_versions3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_inner_conf2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jButton_clean_publicOS, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 44, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jButton_stop_snmp3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jButton_start_snmp3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jTextField_ipaddress_val1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 38, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jLabel_ipaddress3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane19, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel_conf_panelLayout = new org.jdesktop.layout.GroupLayout(jPanel_conf_panel);
        jPanel_conf_panel.setLayout(jPanel_conf_panelLayout);
        jPanel_conf_panelLayout.setHorizontalGroup(
            jPanel_conf_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_inner_conf1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jPanel_inner_conf2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel_conf_panelLayout.setVerticalGroup(
            jPanel_conf_panelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_conf_panelLayout.createSequentialGroup()
                .add(jPanel_inner_conf1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_inner_conf2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane3.addTab("Settings", jPanel_conf_panel);

        jPanel_status_bar.setBackground(new java.awt.Color(204, 204, 204));
        jPanel_status_bar.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        jPanel_status_bar.setPreferredSize(new java.awt.Dimension(4, 32));

        jLabel_status.setText("Status:");

        jLabel_status_report.setText("Current message");

        org.jdesktop.layout.GroupLayout jPanel_status_barLayout = new org.jdesktop.layout.GroupLayout(jPanel_status_bar);
        jPanel_status_bar.setLayout(jPanel_status_barLayout);
        jPanel_status_barLayout.setHorizontalGroup(
            jPanel_status_barLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_status_barLayout.createSequentialGroup()
                .add(12, 12, 12)
                .add(jLabel_status)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel_status_report)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel_status_barLayout.setVerticalGroup(
            jPanel_status_barLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_status_barLayout.createSequentialGroup()
                .add(jPanel_status_barLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_status_report)
                    .add(jLabel_status))
                .add(0, 1, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_status_bar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 765, Short.MAX_VALUE)
            .add(jTabbedPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 765, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jTabbedPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 859, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel_status_bar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        jTabbedPane3.getAccessibleContext().setAccessibleName("Private OS");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton_send_oid_requestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_send_oid_requestActionPerformed

        this.console(this.jTextField_oid_number.getText());
        String str = this.jTextField_oid_number.getText().replace(".", "");
        if (this.isInteger(str)) {
            this.jTextArea_snmp_req_output.setText(this.snmpInterface.getSNMPRequestOutput(this.jTextField_oid_number.getText()));
            this.jLabel_snmp_req_info_val.setText("Response is successfully received");
        } else {
            this.jLabel_snmp_req_info_val.setText("OID number is invalid, use a numer such as .1.3.6");
        }
    }//GEN-LAST:event_jButton_send_oid_requestActionPerformed

    private void drawGraph(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_drawGraph
        // TODO add your handling code here:
        //drawNetworkChange();
    }//GEN-LAST:event_drawGraph

    private void enableSelectedTab(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_enableSelectedTab
        // TODO add your handling code here:
        //console("Selected tab number: "+this.jTabbedPane1.getSelectedIndex()+"");
        if (!this.threadCancelling) {
            if (this.startButtonClicked) {
                console("Start Button is Clicked");
                this.disableTabDataTraffic(this.jTabbedPane1.getSelectedIndex());
            }
        }
    }//GEN-LAST:event_enableSelectedTab
    PINGConnectivityTester ping = new PINGConnectivityTester();
    PINGConnectivityTester ping_public = new PINGConnectivityTester();
    private void jButton_ping_startActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ping_startActionPerformed
        ping.IP = this.jTextField_ping_ipaddress.getText();
        ping.setTextArea(this.jTextArea_ping_output_privateOs);
        ping.startConnectivityTester();
    }//GEN-LAST:event_jButton_ping_startActionPerformed

    private void jButton_ping_stopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ping_stopActionPerformed
        ping.stopConnectivityTester();
    }//GEN-LAST:event_jButton_ping_stopActionPerformed

    private void jButton_ping_clean_ioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ping_clean_ioActionPerformed
        // TODO add your handling code here:
        this.jTextArea_ping_output_privateOs.setText("");
    }//GEN-LAST:event_jButton_ping_clean_ioActionPerformed

    private void jButton_iperf_clean_ioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_iperf_clean_ioActionPerformed
        // TODO add your handling code here:
        this.jTextArea_iperf_output_privateOs.setText("");
    }//GEN-LAST:event_jButton_iperf_clean_ioActionPerformed

    private void jButton_iperf_stopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_iperf_stopActionPerformed
        //jTextArea_iperf_output.append("Iperf is being stoped");
        //iperf.stopIperfTester();
        iperf.stopIPERF();
    }//GEN-LAST:event_jButton_iperf_stopActionPerformed

    private void jButton_iperf_startActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_iperf_startActionPerformed

        console(this.jComboBox_iperf_protocols.getSelectedItem().toString());
        iperf.IP = this.defaultIPAddress;
        iperf.setTextArea(this.jTextArea_iperf_output_privateOs);
        iperf.selectedProtocol = this.jComboBox_iperf_protocols.getSelectedItem().toString();
        iperf.selectedDuration = Integer.parseInt(this.jComboBox_iperf_durations.getSelectedItem().toString());
        iperf.selectedBandwidth = this.jComboBox_iperf_bw.getSelectedItem().toString();
        jTextArea_iperf_output_privateOs.append("Assigned Paramters: Protocol:" + iperf.selectedProtocol + "\nDuration:" + iperf.selectedDuration + "\n");
        iperf.startIPERF();
    }//GEN-LAST:event_jButton_iperf_startActionPerformed

    private void jComboBox_iperf_protocolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox_iperf_protocolsActionPerformed
        // TODO add your handling code here:
        if (this.jComboBox_iperf_protocols.getSelectedItem().toString().equalsIgnoreCase("TCP")) {
            this.jComboBox_iperf_bw.setEnabled(false);
        } else if (this.jComboBox_iperf_protocols.getSelectedItem().toString().equalsIgnoreCase("UDP")) {
            this.jComboBox_iperf_bw.setEnabled(true);
        }
    }//GEN-LAST:event_jComboBox_iperf_protocolsActionPerformed

    private void stop_snmp_private(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stop_snmp_private
        this.stopComponents();
    }//GEN-LAST:event_stop_snmp_private

    private void start_snmp_private(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_start_snmp_private
        this.startComponents();
    }//GEN-LAST:event_start_snmp_private

    private void enableSNMPv1_private(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_enableSNMPv1_private
        this.snmpInterface.setSNMPRequester("SNMPv1");
    }//GEN-LAST:event_enableSNMPv1_private

    private void enableSNMPV2_private(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_enableSNMPV2_private
        this.snmpInterface.setSNMPRequester("SNMPv2");
    }//GEN-LAST:event_enableSNMPV2_private

    private void stop_snmp_public(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stop_snmp_public
        this.stopComponents_public();
    }//GEN-LAST:event_stop_snmp_public

    private void start_snmp_public(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_start_snmp_public
        this.startComponents_public();
    }//GEN-LAST:event_start_snmp_public

    private void enableSNMPv1_public(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_enableSNMPv1_public
        this.snmpInterface_public.setSNMPRequester("SNMPv1");
        //this.jLabel_config_info1.setText("v1 is set");
    }//GEN-LAST:event_enableSNMPv1_public

    private void enableSNMPV2_public(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_enableSNMPV2_public
        this.snmpInterface_public.setSNMPRequester("SNMPv2");
        //this.jLabel_config_info1.setText("v2 is set");
    }//GEN-LAST:event_enableSNMPV2_public

    private void jList1_network_interfaces1drawGraph(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jList1_network_interfaces1drawGraph
        // TODO add your handling code here:
        if (this.jComboBox_iperf_protocols1.getSelectedItem().toString().equalsIgnoreCase("TCP")) {
            this.jComboBox_iperf_bw1.setEnabled(false);
        } else if (this.jComboBox_iperf_protocols1.getSelectedItem().toString().equalsIgnoreCase("UDP")) {
            this.jComboBox_iperf_bw1.setEnabled(true);
        }
    }//GEN-LAST:event_jList1_network_interfaces1drawGraph

    private void jButton_send_oid_request1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_send_oid_request1ActionPerformed
        this.console(this.jTextField_oid_number1.getText());
        String str = this.jTextField_oid_number1.getText().replace(".", "");
        if (this.isInteger(str)) {
            this.jTextArea_snmp_req_output1.setText(this.snmpInterface_public.getSNMPRequestOutput(this.jTextField_oid_number1.getText()));
            this.jLabel_snmp_req_info_val1.setText("Response is successfully received");
        } else {
            this.jLabel_snmp_req_info_val1.setText("OID number is invalid, use a numer such as .1.3.6");
        }        // TODO add your handling code here:
    }//GEN-LAST:event_jButton_send_oid_request1ActionPerformed

    private void jButton_ping_start1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ping_start1ActionPerformed
        // TODO add your handling code here:
        ping_public.IP = this.jTextField_ping_ipaddress1.getText();
        ping_public.setTextArea(this.jTextArea_ping_output_publicOs);
        ping_public.startConnectivityTester();
    }//GEN-LAST:event_jButton_ping_start1ActionPerformed

    private void jButton_ping_stop1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ping_stop1ActionPerformed
        // TODO add your handling code here:
        ping_public.stopConnectivityTester();
    }//GEN-LAST:event_jButton_ping_stop1ActionPerformed

    private void jButton_ping_clean_io1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ping_clean_io1ActionPerformed
        this.jTextArea_ping_output_publicOs.setText("");
    }//GEN-LAST:event_jButton_ping_clean_io1ActionPerformed

    private void jComboBox_iperf_protocols1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox_iperf_protocols1ActionPerformed
// TODO add your handling code here:
        if (this.jComboBox_iperf_protocols1.getSelectedItem().toString().equalsIgnoreCase("TCP")) {
            this.jComboBox_iperf_bw1.setEnabled(false);
        } else if (this.jComboBox_iperf_protocols1.getSelectedItem().toString().equalsIgnoreCase("UDP")) {
            this.jComboBox_iperf_bw1.setEnabled(true);
        }    }//GEN-LAST:event_jComboBox_iperf_protocols1ActionPerformed

    private void jButton_iperf_start1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_iperf_start1ActionPerformed
        console(this.jComboBox_iperf_protocols1.getSelectedItem().toString());
        iperf_public.IP = this.defaultIPAddress_public;
        iperf_public.setTextArea(this.jTextArea_iperf_output_publicOs);
        iperf_public.selectedProtocol = this.jComboBox_iperf_protocols1.getSelectedItem().toString();
        iperf_public.selectedDuration = Integer.parseInt(this.jComboBox_iperf_durations1.getSelectedItem().toString());
        iperf_public.selectedBandwidth = this.jComboBox_iperf_bw1.getSelectedItem().toString();
        jTextArea_iperf_output_publicOs.append("Assigned Paramters: Protocol:" + iperf_public.selectedProtocol + "\nDuration:" + iperf_public.selectedDuration + "\n");
        iperf_public.startIPERF();    }//GEN-LAST:event_jButton_iperf_start1ActionPerformed

    private void jButton_iperf_stop1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_iperf_stop1ActionPerformed
        iperf_public.stopIPERF();
    }//GEN-LAST:event_jButton_iperf_stop1ActionPerformed

    private void jButton_iperf_clean_io1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_iperf_clean_io1ActionPerformed
        // TODO add your handling code here:
        this.jTextArea_iperf_output_publicOs.setText("");
    }//GEN-LAST:event_jButton_iperf_clean_io1ActionPerformed

    private void jTextField_ipaddress_valActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField_ipaddress_valActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField_ipaddress_valActionPerformed

    private void jButton_console_outputsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_console_outputsActionPerformed
        // TODO add your handling code here:
        this.jTextArea_console_outputs.setText("");
    }//GEN-LAST:event_jButton_console_outputsActionPerformed

    private void jButton_clean_privateOSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_clean_privateOSActionPerformed
        // TODO add your handling code here:
        this.jTextArea_privateOS.setText("");
    }//GEN-LAST:event_jButton_clean_privateOSActionPerformed

    private void jButton_clean_publicOSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_clean_publicOSActionPerformed
        // TODO add your handling code here:
        this.jTextArea_publicOs.setText("");
    }//GEN-LAST:event_jButton_clean_publicOSActionPerformed

    private void jButton_ci_add_serviceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ci_add_serviceActionPerformed
        String text=this.jTextField_userID_control_interface.getText();
        this.jLabel_ci_errors.setText("");

        if(checkTextValidity(text)){
            // add to arrayList
            String[] str=text.split(",");
            if(str.length>1){
                 UseViOSUser user= this.snmpInterface_public.new UseViOSUser();
                 user.MAC=str[1];user.userName=str[0];
                 this.snmpInterface_public.usevoisUsers.add(user);
                 // sent to server
                 this.snmpInterface_public.sendUSEViOSUsersToServer();
                 
                 //listModelForNI_public_usevios = (DefaultListModel) jList_ci_usevios_enabled_users.getModel();
                this.jTextField_userID_control_interface.setText("");
             if(str.length>1){
                listModelForNI_public_usevios.addElement(str[0]);// just give the user name
             }
            }
        }else{
        
        }
    }//GEN-LAST:event_jButton_ci_add_serviceActionPerformed

    private void jButton_ci_remove_serviceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ci_remove_serviceActionPerformed
        // TODO add your handling code here:
        //listModelForNI_public_usevios= (DefaultListModel) jList_ci_usevios_enabled_users.getModel();
        this.jLabel_ci_errors.setText("");

        int selectedIndex=this.jList_ci_usevios_enabled_users.getSelectedIndex();
        if(selectedIndex!=-1){
            
            String s=(String)this.jList_ci_usevios_enabled_users.getSelectedValue();
            for(int i=0;i<this.snmpInterface_public.usevoisUsers.size();i++){
                if(this.snmpInterface_public.usevoisUsers.get(i).userName.equalsIgnoreCase(s)){
                  //  console("Selected User:"+this.snmpInterface_public.usevoisUsers.get(i).userName);
                    this.snmpInterface_public.usevoisUsers.remove(i);
                    
                    break;
                }
            }
             //console("Selected User:"+this.snmpInterface_public.usevoisUsers.size());

              // sent to server
            this.snmpInterface_public.sendUSEViOSUsersToServer();
            
            listModelForNI_public_usevios.remove(selectedIndex);
        }
    }//GEN-LAST:event_jButton_ci_remove_serviceActionPerformed

    private void jButton_ci_add_mac_vpnipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ci_add_mac_vpnipActionPerformed
        // TODO add your handling code here:
        String mac = this.jTextField_control_interface_MAC.getText();
        String ip = this.jTextField_control_interface_vpnip.getText();

        this.jLabel_ci_errors1.setText("");

        if (!mac.equalsIgnoreCase("") && !ip.equalsIgnoreCase("")) {
            
            if (ServerInterface.validateMAC(mac) && ServerInterface.validateIP(ip)) {
                
                MACVPNIPTuple tuple = this.snmpInterface_public.new MACVPNIPTuple();
                tuple.MAC = mac;
                tuple.VPNIP = ip;
                this.snmpInterface_public.macVPNIPUserList.add(tuple);
                // sent to server
                this.snmpInterface_public.sendMACVPNIPUserListToServer();
                String tup=mac+"-"+ip;
                listModelForNI_public_mac_vpnip.addElement(tup);// just give the user name
                this.jTextField_control_interface_MAC.setText("");
                this.jTextField_control_interface_vpnip.setText("");
            } else {
                this.jLabel_ci_errors1.setText("MAC or VPN IP Address format is not correct!");
            }
        } else {
            this.jLabel_ci_errors1.setText("Please fill out the mac and ip addresses!");
        }
    }//GEN-LAST:event_jButton_ci_add_mac_vpnipActionPerformed

    private void jButton_ci_remove_mac_vpnipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_ci_remove_mac_vpnipActionPerformed
        // TODO add your handling code here:
        this.jLabel_ci_errors1.setText("");

        int selectedIndex = this.jList_ci_usevios_mac_vpnip.getSelectedIndex();
        if (selectedIndex != -1) {
            String s = (String) this.jList_ci_usevios_mac_vpnip.getSelectedValue();
            String[] tuple=s.split("-");
            for (int i = 0; i < this.snmpInterface_public.macVPNIPUserList.size(); i++) {
                if (this.snmpInterface_public.macVPNIPUserList.get(i).MAC.contains(tuple[0])) {
                    this.snmpInterface_public.macVPNIPUserList.remove(i);
                    break;
                }
            }
            // sent to server
            this.snmpInterface_public.sendMACVPNIPUserListToServer();

            listModelForNI_public_mac_vpnip.remove(selectedIndex);
        }
    }//GEN-LAST:event_jButton_ci_remove_mac_vpnipActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        this.prepareConfigParameters();
        this.prepareConfigParameters_public();

        this.loadUsers();
    }//GEN-LAST:event_jButton1ActionPerformed

    public boolean checkTextValidity(String text){
        this.jLabel_ci_errors.setText("");
        boolean result=false;
        
        if(text.contains(","))
        {
            String[] str=text.split(",");
            if(str.length>1){
                UseViOSUser user= this.snmpInterface_public.new UseViOSUser();
                user.MAC=str[1];user.userName=str[0];
                
                if(!(this.snmpInterface_public.contains(this.snmpInterface_public.usevoisUsers,user))){
                    if(ServerInterface.validateMAC(str[1])){
                        result=true;
                    }else{
                        this.jLabel_ci_errors.setText("MAC address isn't the right format");
                        console("MAC address isn't the right format!");
                        result=false;
                    }
                }else{
                   this.jLabel_ci_errors.setText("This username or MAC Address is already used!");
                   console("This username or MAC Address is already used!");
                }
           }
        }else{
            console("Please use the userID,MAC address format!");
            this.jLabel_ci_errors.setText("Please use the userID,MAC address format!");
            result=false;
        }
        return result;
    }
    
    
    IPERF iperf = new IPERF();
    IPERF iperf_public = new IPERF();

    public boolean isInteger(String integerString) {
        try {
            Integer.parseInt(integerString);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup10;
    private javax.swing.ButtonGroup buttonGroup11;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.ButtonGroup buttonGroup5;
    private javax.swing.ButtonGroup buttonGroup6;
    private javax.swing.ButtonGroup buttonGroup7;
    private javax.swing.ButtonGroup buttonGroup8;
    private javax.swing.ButtonGroup buttonGroup9;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton_ci_add_mac_vpnip;
    private javax.swing.JButton jButton_ci_add_service;
    private javax.swing.JButton jButton_ci_remove_mac_vpnip;
    private javax.swing.JButton jButton_ci_remove_service;
    private javax.swing.JButton jButton_clean_privateOS;
    private javax.swing.JButton jButton_clean_publicOS;
    private javax.swing.JButton jButton_console_outputs;
    private javax.swing.JButton jButton_iperf_clean_io;
    private javax.swing.JButton jButton_iperf_clean_io1;
    private javax.swing.JButton jButton_iperf_start;
    private javax.swing.JButton jButton_iperf_start1;
    private javax.swing.JButton jButton_iperf_stop;
    private javax.swing.JButton jButton_iperf_stop1;
    private javax.swing.JButton jButton_ping_clean_io;
    private javax.swing.JButton jButton_ping_clean_io1;
    private javax.swing.JButton jButton_ping_start;
    private javax.swing.JButton jButton_ping_start1;
    private javax.swing.JButton jButton_ping_stop;
    private javax.swing.JButton jButton_ping_stop1;
    private javax.swing.JButton jButton_send_oid_request;
    private javax.swing.JButton jButton_send_oid_request1;
    private javax.swing.JButton jButton_start_snmp2;
    private javax.swing.JButton jButton_start_snmp3;
    private javax.swing.JButton jButton_stop_snmp2;
    private javax.swing.JButton jButton_stop_snmp3;
    private javax.swing.JComboBox jComboBox_iperf_bw;
    private javax.swing.JComboBox jComboBox_iperf_bw1;
    private javax.swing.JComboBox jComboBox_iperf_durations;
    private javax.swing.JComboBox jComboBox_iperf_durations1;
    private javax.swing.JComboBox jComboBox_iperf_protocols;
    private javax.swing.JComboBox jComboBox_iperf_protocols1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel_ci_errors;
    private javax.swing.JLabel jLabel_ci_errors1;
    private javax.swing.JLabel jLabel_in_icmp_mes;
    private javax.swing.JLabel jLabel_in_icmp_mes1;
    private javax.swing.JLabel jLabel_in_icmp_mes_val;
    private javax.swing.JLabel jLabel_in_icmp_mes_val1;
    private javax.swing.JLabel jLabel_in_tcp_mes;
    private javax.swing.JLabel jLabel_in_tcp_mes1;
    private javax.swing.JLabel jLabel_in_tcp_seg;
    private javax.swing.JLabel jLabel_in_tcp_seg1;
    private javax.swing.JLabel jLabel_in_tcp_seg_val;
    private javax.swing.JLabel jLabel_in_tcp_seg_val1;
    private javax.swing.JLabel jLabel_in_udp_data;
    private javax.swing.JLabel jLabel_in_udp_data1;
    private javax.swing.JLabel jLabel_in_udp_data_val;
    private javax.swing.JLabel jLabel_in_udp_data_val1;
    private javax.swing.JLabel jLabel_ip_delivered;
    private javax.swing.JLabel jLabel_ip_delivered1;
    private javax.swing.JLabel jLabel_ip_delivered_val;
    private javax.swing.JLabel jLabel_ip_delivered_val1;
    private javax.swing.JLabel jLabel_ip_received;
    private javax.swing.JLabel jLabel_ip_received1;
    private javax.swing.JLabel jLabel_ip_received_val;
    private javax.swing.JLabel jLabel_ip_received_val1;
    private javax.swing.JLabel jLabel_ip_requested;
    private javax.swing.JLabel jLabel_ip_requested1;
    private javax.swing.JLabel jLabel_ip_requested_val;
    private javax.swing.JLabel jLabel_ip_requested_val1;
    private javax.swing.JLabel jLabel_ipaddress2;
    private javax.swing.JLabel jLabel_ipaddress3;
    private javax.swing.JLabel jLabel_out_icmp_mes_val;
    private javax.swing.JLabel jLabel_out_icmp_mes_val1;
    private javax.swing.JLabel jLabel_out_tcp_seg;
    private javax.swing.JLabel jLabel_out_tcp_seg1;
    private javax.swing.JLabel jLabel_out_tcp_seg_val;
    private javax.swing.JLabel jLabel_out_tcp_seg_val1;
    private javax.swing.JLabel jLabel_out_udp_data;
    private javax.swing.JLabel jLabel_out_udp_data1;
    private javax.swing.JLabel jLabel_out_udp_data_val;
    private javax.swing.JLabel jLabel_out_udp_data_val1;
    private javax.swing.JLabel jLabel_snmp_req_info;
    private javax.swing.JLabel jLabel_snmp_req_info1;
    private javax.swing.JLabel jLabel_snmp_req_info_val;
    private javax.swing.JLabel jLabel_snmp_req_info_val1;
    private javax.swing.JLabel jLabel_status;
    public static javax.swing.JLabel jLabel_status_report;
    private javax.swing.JLabel jLabel_sys_date;
    private javax.swing.JLabel jLabel_sys_date1;
    private javax.swing.JLabel jLabel_sys_date_val;
    private javax.swing.JLabel jLabel_sys_date_val1;
    private javax.swing.JLabel jLabel_sys_uptime;
    private javax.swing.JLabel jLabel_sys_uptime1;
    private javax.swing.JLabel jLabel_sys_uptime_val;
    private javax.swing.JLabel jLabel_sys_uptime_val1;
    private javax.swing.JLabel jLabel_udp_local_ports;
    private javax.swing.JLabel jLabel_udp_local_ports1;
    private javax.swing.JLabel jLabel_udp_local_pots_cal;
    private javax.swing.JLabel jLabel_udp_local_pots_cal1;
    private javax.swing.JLabel jLabel_udp_no_port;
    private javax.swing.JLabel jLabel_udp_no_port1;
    private javax.swing.JLabel jLabel_udp_no_port_val;
    private javax.swing.JLabel jLabel_udp_no_port_val1;
    private javax.swing.JList jList1_network_interfaces;
    private javax.swing.JList jList1_network_interfaces1;
    private javax.swing.JList jList_ci_usevios_enabled_users;
    private javax.swing.JList jList_ci_usevios_mac_vpnip;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel11_cpu_stat;
    private javax.swing.JPanel jPanel11_cpu_stat1;
    private javax.swing.JPanel jPanel11_mem_stat;
    private javax.swing.JPanel jPanel11_mem_stat1;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel14_ni;
    private javax.swing.JPanel jPanel14_ni1;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel15_selcted_nic_graph;
    private javax.swing.JPanel jPanel15_selcted_nic_graph1;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPanel jPanel9_cpu_graph;
    private javax.swing.JPanel jPanel9_cpu_graph1;
    private javax.swing.JPanel jPanel9_mem_graph;
    private javax.swing.JPanel jPanel9_mem_graph1;
    private javax.swing.JPanel jPanel_conf_panel;
    private javax.swing.JPanel jPanel_console_outputs;
    private javax.swing.JPanel jPanel_control_interface;
    private javax.swing.JPanel jPanel_control_interface_panel;
    private javax.swing.JPanel jPanel_control_interface_panel1;
    private javax.swing.JPanel jPanel_cpumem;
    private javax.swing.JPanel jPanel_cpumem1;
    private javax.swing.JPanel jPanel_hostinfo;
    private javax.swing.JPanel jPanel_hostinfo1;
    private javax.swing.JPanel jPanel_inner_conf1;
    private javax.swing.JPanel jPanel_inner_conf2;
    private javax.swing.JPanel jPanel_net_interfaces;
    private javax.swing.JPanel jPanel_net_interfaces1;
    private javax.swing.JPanel jPanel_prostat;
    private javax.swing.JPanel jPanel_prostat1;
    private javax.swing.JPanel jPanel_snmp_versions2;
    private javax.swing.JPanel jPanel_snmp_versions3;
    private javax.swing.JPanel jPanel_snmpreq;
    private javax.swing.JPanel jPanel_snmpreq1;
    private javax.swing.JPanel jPanel_status_bar;
    private javax.swing.JRadioButton jRadioButton_snmp_v1_privateOS;
    private javax.swing.JRadioButton jRadioButton_snmpv1_publicOS;
    private javax.swing.JRadioButton jRadioButton_snmpv2_privateOS;
    private javax.swing.JRadioButton jRadioButton_snmpv2_publicOS;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane12;
    private javax.swing.JScrollPane jScrollPane13;
    private javax.swing.JScrollPane jScrollPane14;
    private javax.swing.JScrollPane jScrollPane15;
    private javax.swing.JScrollPane jScrollPane16;
    private javax.swing.JScrollPane jScrollPane17;
    private javax.swing.JScrollPane jScrollPane18;
    private javax.swing.JScrollPane jScrollPane19;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane20;
    private javax.swing.JScrollPane jScrollPane21;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTabbedPane jTabbedPane3;
    private javax.swing.JTabbedPane jTabbedPane4;
    private javax.swing.JTabbedPane jTabbedPane5;
    private javax.swing.JTabbedPane jTabbedPane_cpu;
    private javax.swing.JTabbedPane jTabbedPane_cpu1;
    private javax.swing.JTabbedPane jTabbedPane_mem2;
    private javax.swing.JTabbedPane jTabbedPane_mem3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable_connected_users;
    private javax.swing.JTable jTable_connected_users1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea_console_outputs;
    public javax.swing.JTextArea jTextArea_iperf_output_privateOs;
    public javax.swing.JTextArea jTextArea_iperf_output_publicOs;
    public javax.swing.JTextArea jTextArea_ping_output_privateOs;
    public javax.swing.JTextArea jTextArea_ping_output_publicOs;
    public javax.swing.JTextArea jTextArea_privateOS;
    public javax.swing.JTextArea jTextArea_publicOs;
    private javax.swing.JTextArea jTextArea_snmp_req_output;
    private javax.swing.JTextArea jTextArea_snmp_req_output1;
    private javax.swing.JTextArea jTextArea_snmpreq_info;
    private javax.swing.JTextArea jTextArea_snmpreq_info1;
    private javax.swing.JTextField jTextField_control_interface_MAC;
    private javax.swing.JTextField jTextField_control_interface_vpnip;
    private javax.swing.JTextField jTextField_ipaddress_val;
    private javax.swing.JTextField jTextField_ipaddress_val1;
    private javax.swing.JTextField jTextField_oid_number;
    private javax.swing.JTextField jTextField_oid_number1;
    private javax.swing.JTextField jTextField_ping_ipaddress;
    private javax.swing.JTextField jTextField_ping_ipaddress1;
    private javax.swing.JTextField jTextField_userID_control_interface;
    private javax.swing.JLabel jbl_cpu_15min_load;
    private javax.swing.JLabel jbl_cpu_15min_load1;
    private javax.swing.JLabel jbl_cpu_15min_load_val;
    private javax.swing.JLabel jbl_cpu_15min_load_val1;
    private javax.swing.JLabel jbl_cpu_1min_load;
    private javax.swing.JLabel jbl_cpu_1min_load1;
    private javax.swing.JLabel jbl_cpu_1min_load_val;
    private javax.swing.JLabel jbl_cpu_1min_load_val1;
    private javax.swing.JLabel jbl_cpu_5min_load;
    private javax.swing.JLabel jbl_cpu_5min_load1;
    private javax.swing.JLabel jbl_cpu_5min_load_val;
    private javax.swing.JLabel jbl_cpu_5min_load_val1;
    private javax.swing.JLabel jbl_cpu_per_idle_cpu;
    private javax.swing.JLabel jbl_cpu_per_idle_cpu1;
    private javax.swing.JLabel jbl_cpu_per_idle_cpu_val;
    private javax.swing.JLabel jbl_cpu_per_idle_cpu_val1;
    private javax.swing.JLabel jbl_cpu_per_sys_cpu_val;
    private javax.swing.JLabel jbl_cpu_per_sys_cpu_val1;
    private javax.swing.JLabel jbl_cpu_per_user_cpu_val;
    private javax.swing.JLabel jbl_cpu_per_user_cpu_val1;
    private javax.swing.JLabel jbl_cpu_perc_sys_cpu;
    private javax.swing.JLabel jbl_cpu_perc_sys_cpu1;
    private javax.swing.JLabel jbl_cpu_raw_idle_cpu;
    private javax.swing.JLabel jbl_cpu_raw_idle_cpu1;
    private javax.swing.JLabel jbl_cpu_raw_idle_cpu_val;
    private javax.swing.JLabel jbl_cpu_raw_idle_cpu_val1;
    private javax.swing.JLabel jbl_cpu_raw_nice_cpu;
    private javax.swing.JLabel jbl_cpu_raw_nice_cpu1;
    private javax.swing.JLabel jbl_cpu_raw_nice_cpu_val;
    private javax.swing.JLabel jbl_cpu_raw_nice_cpu_val1;
    private javax.swing.JLabel jbl_cpu_raw_sys_cpu;
    private javax.swing.JLabel jbl_cpu_raw_sys_cpu1;
    private javax.swing.JLabel jbl_cpu_raw_sys_cpu_val;
    private javax.swing.JLabel jbl_cpu_raw_sys_cpu_val1;
    private javax.swing.JLabel jbl_cpu_raw_user_cpu;
    private javax.swing.JLabel jbl_cpu_raw_user_cpu1;
    private javax.swing.JLabel jbl_cpu_raw_user_cpu_val;
    private javax.swing.JLabel jbl_cpu_raw_user_cpu_val1;
    private javax.swing.JLabel jbl_cpu_usercpu_time;
    private javax.swing.JLabel jbl_cpu_usercpu_time1;
    private javax.swing.JLabel jbl_mem_avai_swap_space;
    private javax.swing.JLabel jbl_mem_avai_swap_space1;
    private javax.swing.JLabel jbl_mem_avai_swap_space_val;
    private javax.swing.JLabel jbl_mem_avai_swap_space_val1;
    private javax.swing.JLabel jbl_mem_total_cached_mem;
    private javax.swing.JLabel jbl_mem_total_cached_mem1;
    private javax.swing.JLabel jbl_mem_total_cached_mem_val;
    private javax.swing.JLabel jbl_mem_total_cached_mem_val1;
    private javax.swing.JLabel jbl_mem_total_ram;
    private javax.swing.JLabel jbl_mem_total_ram1;
    private javax.swing.JLabel jbl_mem_total_ram_buffered;
    private javax.swing.JLabel jbl_mem_total_ram_buffered1;
    private javax.swing.JLabel jbl_mem_total_ram_buffered_val;
    private javax.swing.JLabel jbl_mem_total_ram_buffered_val1;
    private javax.swing.JLabel jbl_mem_total_ram_free;
    private javax.swing.JLabel jbl_mem_total_ram_free1;
    private javax.swing.JLabel jbl_mem_total_ram_free_val;
    private javax.swing.JLabel jbl_mem_total_ram_free_val1;
    private javax.swing.JLabel jbl_mem_total_ram_shared;
    private javax.swing.JLabel jbl_mem_total_ram_shared1;
    private javax.swing.JLabel jbl_mem_total_ram_shared_val;
    private javax.swing.JLabel jbl_mem_total_ram_shared_val1;
    private javax.swing.JLabel jbl_mem_total_ram_used;
    private javax.swing.JLabel jbl_mem_total_ram_used1;
    private javax.swing.JLabel jbl_mem_total_ram_used_val;
    private javax.swing.JLabel jbl_mem_total_ram_used_val1;
    private javax.swing.JLabel jbl_mem_total_ram_val;
    private javax.swing.JLabel jbl_mem_total_ram_val1;
    private javax.swing.JLabel jbl_mem_total_swap_size;
    private javax.swing.JLabel jbl_mem_total_swap_size1;
    private javax.swing.JLabel jbl_mem_total_swap_size_val;
    private javax.swing.JLabel jbl_mem_total_swap_size_val1;
    // End of variables declaration//GEN-END:variables
}
