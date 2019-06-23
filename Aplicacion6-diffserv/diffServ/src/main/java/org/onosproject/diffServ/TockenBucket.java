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
package org.onosproject.diffServ;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.onlab.util.Bandwidth;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.DefaultQosDescription;
import org.onosproject.net.behaviour.DefaultQueueDescription;
import org.onosproject.net.behaviour.PortConfigBehaviour;
import org.onosproject.net.behaviour.QosConfigBehaviour;
import org.onosproject.net.behaviour.QosDescription;
import org.onosproject.net.behaviour.QosId;
import org.onosproject.net.behaviour.QosDescription.Type;
import org.onosproject.net.behaviour.QueueConfigBehaviour;
import org.onosproject.net.behaviour.QueueDescription;
import org.onosproject.net.behaviour.QueueId;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Aplicacion que no hace nada :) 
 */
@Component(immediate = true)

public class TockenBucket{

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected DeviceService deviceService;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private ApplicationId appId;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected FlowRuleService flowRuleService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected CoreService coreService;

	@Activate
	protected void activate() {

		appId = coreService.registerApplication("org.onosproject.TockenBucket",
				() -> log.info("Periscope down."));
		log.info("Activada aplicacion diffserv");

		//Vemos las colas que tiene el router   

		for(Device d: deviceService.getAvailableDevices()) {
			log.error("Dispositivo {}",d.id());
			if(d.is(null)){
				log.info("No hay dispositivos conectados a la red");
			}
			else {
				if(d.is(QueueConfigBehaviour.class)){ 
					if(d.id().toString().startsWith("ovsdb:")) {
						QueueConfigBehaviour queueConfig = d.as(QueueConfigBehaviour.class); 
						QosConfigBehaviour qosConfig = d.as(QosConfigBehaviour.class);
						PortConfigBehaviour portConfig = d.as(PortConfigBehaviour.class);


					//	Long maxRate = 10000L;
						Long minRate = 5000L;
						String name = "123";

						QueueDescription queueDesc = DefaultQueueDescription.builder()
								.queueId(QueueId.queueId(name))
					//			.maxRate(Bandwidth.bps(maxRate))
								.minRate(Bandwidth.bps(minRate))
								.build();


						queueConfig.addQueue(queueDesc);

						log.info("Cola creada");

						PortDescription portDesc = DefaultPortDescription.builder()
								.isEnabled(true)
								.withPortNumber(PortNumber.portNumber(2))
								.build();


						Map<Long, QueueDescription> queues = new HashMap<>();
						queues.put(123L, queueDesc);


						QosDescription qosDesc = DefaultQosDescription.builder()
								.qosId(QosId.qosId("qos1"))
								.type(QosDescription.Type.HTB)
								.maxRate(Bandwidth.bps(Long.valueOf("10000")))
							//	.cbs(80L)
							//	.cir(5000L) //paquetes IP/s
								
								.queues(queues)
								.build();

						queueConfig.addQueue(queueDesc);
						qosConfig.addQoS(qosDesc);
			     		portConfig.applyQoS(portDesc, qosDesc);



						//Mostramos las colas (ver si funciona el codigo)
						Iterator<QueueDescription> it = queueConfig.getQueues().iterator();
						while(it.hasNext()) {
							log.error("Colas: "+it.next().toString());
						}
					}
					else {
						
						log.warn("El id dispositivo no empieza por ovsdb:");
	
						//Creamos regla que haga que el trafico vaya por la cola		        
						TrafficSelector selector = DefaultTrafficSelector.builder()
								.matchInPort(PortNumber.portNumber(2L))
								.build();
						TrafficTreatment drop = DefaultTrafficTreatment.builder()
								.setQueue(123L)
							//	.setOutput(PortNumber.NORMAL)
								.build();

						//Creamos la regla que limita el trafico para la MAC de origen
						FlowRule rule1 = DefaultFlowRule.builder()
								.fromApp(appId)
								.forDevice(d.id())
								.makePermanent()
								.withSelector(selector)
								.withPriority(129)
								.withTreatment(drop)
								.build();


						flowRuleService.applyFlowRules(rule1);
					}
					
				} 
				else { 
					log.warn("Device {} does not support QueueConfigBehavior", d.id()); 
					

				} 
			} 
		}

	}

	@Deactivate
	protected void deactivate() {
		log.info("Desactivada aplicacion diffserv");
		flowRuleService.removeFlowRulesById(appId);		
	}
}


