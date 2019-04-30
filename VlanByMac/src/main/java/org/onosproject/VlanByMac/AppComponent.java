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
package org.onosproject.VlanByMac;


import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
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
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProbingService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.ProbeMode;
import org.onosproject.net.flowobjective.ForwardingObjective;


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

	int priority = 129;
    private final HashMap<MacAddress,Short> macVlanMap = new HashMap<MacAddress,Short>();    
    
    private final HashMap<MacAddress,FlowRule[]> macRuleMap = new HashMap<MacAddress,FlowRule[]>();    

    private final HostListener hostListener = new InternalHostListener();

	@Activate
	protected void activate() {
		appId = coreService.registerApplication("org.onosproject.VlanByMac",
				() -> log.info("Periscope down."));
		log.info("Started");
		hostService.addListener(hostListener);

		//Asignamos las VLAN al hashmap con las diferentes mac que tendremos en nuestra red
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:01"),(short)1);
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:02"),(short)1);
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:03"),(short)2);
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:04"),(short)2);

	}

	@Deactivate
	protected void deactivate() {
		cfgService.unregisterProperties(getClass(), false);	
		flowRuleService.removeFlowRulesById(appId);
		log.info("Stopped");
	}    
	
	private class InternalHostListener implements HostListener {
    @Override
    public void event(HostEvent event) {
    	if(event.type()==HostEvent.Type.HOST_ADDED) {
    		//Mirar el HashMap para crear una regla de flujo 
    		//que el trafico de la direccion MAC del host añadido se le asigne la VLAN correspondiente
    		log.warn("Host añadido");
    		MacAddress macHost = event.subject().mac();
    		
    		VlanId VlanHost = VlanId.vlanId(macVlanMap.get(macHost));
    		
        	TrafficSelector selector1 = DefaultTrafficSelector.builder().matchEthSrc(macHost).build();
            TrafficTreatment addVlan = DefaultTrafficTreatment.builder().pushVlan().setVlanId(VlanHost)
                 .build();
            
            //Creamos la regla que asigna la VLAN para la mac del host que se acaba de conectar
            FlowRule rule1 = DefaultFlowRule.builder()
            		.fromApp(appId)
            		.forDevice(event.subject().location().deviceId())
            		.makePermanent()
            		.withPriority(8)
            		.withSelector(selector1)
            		.withTreatment(addVlan)
            		.build();
            
         	TrafficSelector selector2 = DefaultTrafficSelector.builder().matchEthDst(macHost).build();
            TrafficTreatment removeVlan = DefaultTrafficTreatment.builder().popVlan().build();
            
            //Creamos la regla que asigna la VLAN para la mac del host que se acaba de conectar
            FlowRule rule2 = DefaultFlowRule.builder()
            		.fromApp(appId)
            		.forDevice(event.subject().location().deviceId())
            		.makePermanent()
            		.withSelector(selector2)
            		.withPriority(9)
            		.withTreatment(removeVlan)
            		.build();
            
            FlowRule []array = {rule1,rule2};
            flowRuleService.applyFlowRules(rule1);
            macRuleMap.put(macHost,array);
      
    	}
    	else if(event.type() == HostEvent.Type.HOST_REMOVED) {
    		log.warn("Host eliminado");
    		FlowRule[] array = macRuleMap.get(event.subject().mac());
    		flowRuleService.removeFlowRules(array);;
    	}
    }
}
}

