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
package org.onosproject.detectHostBan;


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
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRule.Builder;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostProbingService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.ProbeMode;

import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_SRC;

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
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;
    
   // private final FlowRuleListener flowListener = new InternalFlowListener();

    
    private final HashMap<MacAddress,Long> hosts = new HashMap<MacAddress,Long>();    

    protected Timer timer;
    public long LIMIT_MB = 1000000; //1MB de maximo de datos
    private static final int DROP_PRIORITY = 129;
    private int TIMEOUT_SEC = 30;



    @Activate
    protected void activate() {
        
        appId = coreService.registerApplication("org.onosproject.detectHostBan",
                () -> log.info("Periscope down."));
       // flowRuleService.addListener(flowListener);
        log.info("Started");
        
        TimerTask repeatedTask= new TimerTask(){
        	public void run() {
        		
        		Iterable<Host> setH = hostService.getHosts();
        		
        		for (Host h: setH) {
        			MacAddress macHost = h.mac();
        			
        			Long oldData = hosts.get(macHost);
        			if(oldData==null)
        				oldData=0L;
        			
        			else if(oldData>=LIMIT_MB)
        				return;
        			
        			PortStatistics stats = deviceService.getDeltaStatisticsForPort(h.location().deviceId(), h.location().port());

        			long bytesSent = stats.bytesSent();
        			long bytesReceived = stats.bytesReceived();

        			long deltaStats = bytesSent + bytesReceived;
        			
        			Long totalData = deltaStats + oldData;
        		
        			log.warn("Total statistics for MAC " +macHost+" are: " +totalData);

        			//En el HashMap introducimos los datos acumulados que lleva la MAC asociada al host
        			hosts.put(macHost, totalData); 
        			
        			//En caso de que los datos superen en total baneamos la mac del dispositivo
        			if(totalData>=LIMIT_MB) {
        				banPings(h.location().deviceId(),macHost);
        			}
        		}//Cierre del for
        	}//Cierre del run
        };//Cierre del TimerTask
        
        timer = new Timer("Timer");
        long delay = 1000L; // We start polling statistics after 1 second
        long period = 1000L * 10L; // Every 10 seconds we get the statistics
        timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }//Cierre del activate

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        timer.cancel();
        log.info("Stopped");
    }
    
    
    private void banPings(DeviceId deviceId, MacAddress src) {
    	TrafficSelector selector = DefaultTrafficSelector.builder().matchEthSrc(src).build();
        TrafficTreatment drop = DefaultTrafficTreatment.builder()
                .drop().build();
        
        //Creamos la regla que limita el trafico para la MAC de origen
        FlowRule rule1 = DefaultFlowRule.builder()
        		.fromApp(appId)
        		.forDevice(deviceId)
        		.makePermanent()
        		.withSelector(selector)
        		.withPriority(DROP_PRIORITY)
        		.withTreatment(drop)
        		.build();

        
        selector = DefaultTrafficSelector.builder().matchEthDst(src).build();

        FlowRule rule2 = DefaultFlowRule.builder()
        		.fromApp(appId)
        		.forDevice(deviceId)
        		.makePermanent()
        		.withSelector(selector)
        		.withPriority(DROP_PRIORITY)
        		.withTreatment(drop)
        		.build();

        //Y las aplicamos 
        flowRuleService.applyFlowRules(rule1,rule2);
        
       /* flowObjectiveService.forward(deviceId, DefaultForwardingObjective.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(drop)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(DROP_PRIORITY)
                .makeTemporary(30)
                .add());*/
    	

		timer.schedule(new PingPruner(rule1,rule2), TIMEOUT_SEC * 1000);
        
		log.error("Baneo aplicado a la MAC: "+src);
    }
    
   
   /* private class InternalFlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            FlowRule flowRule = event.subject();
            if (event.type() == RULE_REMOVED && flowRule.appId() == appId.id()) {
                Criterion criterion = flowRule.selector().getCriterion(ETH_SRC);
                MacAddress src = ((EthCriterion) criterion).mac();
                hosts.put(src,0L);
                log.warn("Baneo eliminado a la MAC: "+src);
            }
        }
    }*/
    
 // Prunes the given ping record from the specified device.
    private class PingPruner extends TimerTask {
        private final FlowRule rule1;
        private final FlowRule rule2;
        
        public PingPruner(FlowRule rule1, FlowRule rule2) {
            this.rule1 = rule1;
            this.rule2 = rule2;
        }

        @Override
        public void run() {
        	flowRuleService.removeFlowRules(rule1,rule2);
            Criterion criterion = rule1.selector().getCriterion(ETH_SRC);
            MacAddress src = ((EthCriterion) criterion).mac();
        	hosts.put(src,0L);
            log.warn("Baneo eliminado a la MAC: "+src);
        }
    }
}
