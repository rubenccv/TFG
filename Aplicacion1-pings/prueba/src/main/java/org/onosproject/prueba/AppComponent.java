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
package org.onosproject.prueba;

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
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
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
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.severalping.AppComponent.PingPacketProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;

import static org.onosproject.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_DST;

/**
 * Aplicacion que permite un numero limitado de pings por minuto entre las MACs src/dst
 */
@Component(immediate = true)
public class AppComponent {

    private static Logger log = LoggerFactory.getLogger(AppComponent.class);

    private static final String MSG_PINGED_OK =
            "Ping {}/{} received from {} to {}";
    private static final String MSG_PINGED_LIMIT =
            "Limit of {} pings reached!!! " +
                    "Ping from {} to {} has already been received. 60 seconds ban";
                    
    private static final String MSG_PING_REENABLED =
            "Re-enabled ping from {} to {}";

    private static final int PRIORITY = 128;
    private static final int DROP_PRIORITY = 129;
    private static final int TIMEOUT_SEC = 60; // seconds

    private static final int LIM_PINGS = 4; // maximum number of pings allowed

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    private ApplicationId appId;

    static byte	TYPE_ECHO_REQUEST;

    private final PacketProcessor packetProcessor = new PingPacketProcessor();

    // Means to track detected pings from each device on a temporary basis
    private final HashMap<PingRecord,Integer> pings = new HashMap<PingRecord,Integer>();

    public AppComponent()
    {
        
    }

    @Activate //Cuando activamos la aplicacion desde ONOS se ejecuta este metodo
    public void activate() {	
        appId = coreService.registerApplication("org.onosproject.prueba",
                                                () -> log.info("Periscope down."));
        log.info("Started");
        
        
    }

    @Deactivate
    public void deactivate() {
        flowRuleService.removeFlowRulesById(appId);
        log.info("Stopped");
    }

    //Clase que nos permite obtener el paquete y comprobar si es un ICMP
    private class PingPacketProcessor implements PacketProcessor{
    	
    	public void process(PacketContext context) {
    		Ethernet eth = context.inPacket().parsed();
    		if(eth.getEtherType()==Ethernet.TYPE_IPV4) {
    			
    		}
    			
    		
    	}
    	
    	
    }

}
