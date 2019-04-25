/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.detectHost;


import org.onlab.packet.MacAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.host.HostProbingService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.ProbeMode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Skeletal ONOS application component.
 */

@Component(immediate = true)
public class AppComponent{

    private final Logger log = LoggerFactory.getLogger(AppComponent.class);

    private ApplicationId appId;

   
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostProbingService hostProbingService;
    
    private final HashMap<MacAddress,Long> hosts = new HashMap<MacAddress,Long>();    

    protected Timer timer1;
    protected Timer timer2;
    public long LIMIT_MB = 1000000; //1MB de maximo de datos


    @Activate
    protected void activate() {
        
        appId = coreService.registerApplication("org.onosproject.detectHost",
                () -> log.info("Periscope down."));
        log.info("Started");
        

              
        //Obtenemos los hosts que estan conectados a nuestro dispositivos
        TimerTask repeatedTask1 = new TimerTask() {
        	public void run() {
        		//Vemos si  los dispositivos conectados a nuestro ONOS estan activos
                Iterable<Device> devices = deviceService.getDevices(); //Obtenemos los dispositivos controlados por el ONOS (en el primer ejemplo solo el openVSwitch)
                
                for (Device d: devices) {
                	List<Port> ports = deviceService.getPorts(d.id());
                	for(Port port : ports) {
                	ConnectPoint connectPoint = new ConnectPoint(d.id(),port.number());
                    Set<Host> setH = hostService.getConnectedHosts(connectPoint);
                    Iterator<Host> aux = setH.iterator();

                	log.error("Number of host connected is: " +hostService.getHostCount());
                    while (aux.hasNext()) {
                    	//En cada iteracion del bucle obtenemos un nuevo host de los conectados
                    	Host h = aux.next();
                    	MacAddress macHost = h.mac();
                   
                    	
                    	PortStatistics stats = deviceService.getDeltaStatisticsForPort(d.id(), port.number());
                    	
                    	long bytesSent = stats.bytesSent();
                    	long bytesReceived = stats.bytesReceived();
                    	
                    	long deltaStats = bytesSent + bytesReceived;
                    	Long oldData = hosts.get(macHost);
                    	if (oldData==null)
                    		oldData=0L;
                    	
                    	Long totalData = deltaStats + oldData;
                    	log.warn("Total statistics for MAC " +macHost+" are: " +totalData);
                    	
                    	hosts.put(macHost, totalData);
                  }
                }
              }        		
        	}
        };
              
        TimerTask repeatedTask2 = new TimerTask() {
        	public void run() {
              Iterable<Host> hosts =  hostService.getHosts();
              for(Host h:hosts) {
            	  ConnectPoint connectPoint = new ConnectPoint(h.location().deviceId(),h.location().port());
            	  ProbeMode probeMode = ProbeMode.VERIFY;
            	  hostProbingService.probeHost(h, connectPoint,probeMode);          		  
              }
        	}
        };
        
        timer1 = new Timer("Timer1");
        timer2 = new Timer("Timer2");
        long delay = 1000L; // We start polling statistics after 1 second
        long period = 1000L * 10L; // Every 10 seconds we get the statistics
        timer1.scheduleAtFixedRate(repeatedTask1, delay, period);
        timer2.scheduleAtFixedRate(repeatedTask2, delay, period);

    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

  
}
