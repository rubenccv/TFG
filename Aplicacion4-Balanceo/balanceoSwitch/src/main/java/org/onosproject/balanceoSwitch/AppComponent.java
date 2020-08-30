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
package org.onosproject.balanceoSwitch;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onlab.util.Bandwidth;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BandwidthCapacity;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.net.flow.criteria.Criterion.Type.IN_PHY_PORT;
import org.onosproject.net.flow.criteria.ExtensionCriterion;

import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Aplicacion que realiza el balanceo de carga entre 2 switches para evitar cuellos de botella
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(AppComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService networkConfigService;
    
    private ApplicationId appId;
    
    long puerto1 = 11;
    long puerto2 = 12;
    long puerto3 = 13;
    long puerto4 = 14;
    long bitsPorSegundo = 0;
    
    protected Timer timer; 
    Long temporizadorTarea = 20L;

    int cont = 0;
    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.balanceoSwitch",
                () -> log.info("Periscope down."));
        
        Iterable<Device> devices = deviceService.getDevices(); 
        for(Device d:devices) {
        	List<Port> ports = deviceService.getPorts(d.id());
        	for(Port p:ports) {
        		if(p.number().toLong()== (long)11 || p.number().toLong()== (long)12 || p.number().toLong()==(long)13 || p.number().toLong()== (long)14) {
        			ConnectPoint cp = new ConnectPoint(d.id(),p.number());
        			updatePortBandwidth(cp, Bandwidth.mbps(13));
        		}
        	}
        }
        TimerTask tarea = new TimerTask() {
        	public void run() {
        		//Obtenemos las estadisticas para los puertos del OVS1
                Iterable<Device> devices = deviceService.getDevices(); 
                for (Device d: devices) {
                	bitsPorSegundo = 0;
                    log.warn("Device id " + d.id().toString());
                	List<Port> ports = deviceService.getPorts(d.id());
                	
                	for (Port p: ports) {		
                		
                		log.info("Velocidad puerto " + p.number().toLong() + " es: " +p.portSpeed());
                	/*	
                		if(p.number().toLong()== (long)11 ) {
                			log.error("Puerto 10 alcanzado");
                			break;
                		}*/
                		

                		//Para cada puerto vemos el trafico de salida que tiene
                        PortStatistics traffic = deviceService.getDeltaStatisticsForPort(d.id(), p.number());
                		if(traffic!=null)
                			bitsPorSegundo = bitsPorSegundo + (traffic.bytesSent()*8)/temporizadorTarea;        		
                		else
                			log.error("Unable to read portStats");
                		

                	}//Cierre del for ports
            			log.error("Bits: "+bitsPorSegundo);
                		//Creamos el grupo en el que metemos el puerto 10 ya que siempre se va a mandar minimo por ahi
                	
                		List<GroupBucket> listGroup = new ArrayList<GroupBucket>();
                		
                        TrafficTreatment sendPort = DefaultTrafficTreatment.builder()
                                .setOutput(PortNumber.portNumber(puerto1)).build();
                        
                		GroupBucket gb = DefaultGroupBucket.createSelectGroupBucket(sendPort);
                		
                		listGroup.add(gb);
                		
						
                		//Si los bits por segundo superan ciertos umbrales vamos aumentando los puertos por los cuales mandar el trafico
                		if(bitsPorSegundo>=100000) { //100kbps
                			//Añadimos al grupo el puerto 11
                			log.warn("Añadido puerto 2");
                            TrafficTreatment sendPort2 = DefaultTrafficTreatment.builder()
                                    .setOutput(PortNumber.portNumber(puerto2)).build();
                    		GroupBucket gb2 = DefaultGroupBucket.createSelectGroupBucket(sendPort2);

                    		listGroup.add(gb2);
                			
                		}
                		if(bitsPorSegundo>=200000) { //20Mbps
                			//Añadimos al grupo el puerto 12
                			log.warn("Añadido puerto 3");
                			TrafficTreatment sendPort3 = DefaultTrafficTreatment.builder()
                                    .setOutput(PortNumber.portNumber(puerto3)).build();
                    		GroupBucket gb3 = DefaultGroupBucket.createSelectGroupBucket(sendPort3);

                    		listGroup.add(gb3);
                			
                		}
                		if(bitsPorSegundo>=300000) {
                			//Añadimos al grupo el puerto 13
                			log.warn("Añadido puerto 4");
                            TrafficTreatment sendPort4 = DefaultTrafficTreatment.builder()
                                    .setOutput(PortNumber.portNumber(puerto4)).build();
                    		GroupBucket gb4 = DefaultGroupBucket.createSelectGroupBucket(sendPort4);

                    		listGroup.add(gb4);
                			
                		}
                		
                		GroupBuckets gbs = new GroupBuckets(listGroup);
                		
                		//Creamos el grupo 137 para que cuadre con la aplicacion fwdBalanceo
						DefaultGroup dgd = new DefaultGroup(new GroupId(137),d.id(),GroupDescription.Type.SELECT,gbs);
                		
                		groupService.addGroup(dgd);						
               	
                }
        		
        	}
        };
        
        //Creamos el temporizador para la tarea
        timer = new Timer("Timer");
        long delay = 1000L; // We start polling statistics after 1 second
        long period = 1000L * temporizadorTarea; // Every 30 seconds we get the statistics
        timer.scheduleAtFixedRate(tarea, delay, period);
    	
    }//Cierre del activate	
    @Deactivate
    protected void deactivate() {
    	timer.cancel();
    	log.info("Stopped");
    	
    }//Cierre del deactivate
    
    
    private void updatePortBandwidth(ConnectPoint cp, Bandwidth bandwidth) {
        log.warn("update Port {} Bandwidth {}", cp, bandwidth);
        BandwidthCapacity bwCapacity = networkConfigService.addConfig(cp, BandwidthCapacity.class);
        bwCapacity.capacity(bandwidth).apply();
    }

}//Cierre del Appcomponent
