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

	@Activate
	protected void activate() {
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
						
						
						Long maxRate = 100L;
						Long minRate = 50L;
						String name = "cola1";

						QueueDescription queueDesc = DefaultQueueDescription.builder()
								.queueId(QueueId.queueId(name))
								.maxRate(Bandwidth.bps(maxRate))
								.minRate(Bandwidth.bps(minRate))
								.burst(20L)
								.build();


						queueConfig.addQueue(queueDesc);

						log.info("Cola creada");

					    
				        PortDescription portDesc = DefaultPortDescription.builder()
				        		.isEnabled(true)
				        		.withPortNumber(PortNumber.portNumber(2))
				        				.build();

				        
				        Map<Long, QueueDescription> queues = new HashMap<>();
				        queues.put(0L, queueDesc);
				        
				        
				        QosDescription qosDesc = DefaultQosDescription.builder()
				                .qosId(QosId.qosId("qos1"))
				                .type(QosDescription.Type.HTB)
				                .maxRate(Bandwidth.bps(Long.valueOf("100000")))
				                .cbs(5000L)
				                .cir(400L) //paquetes IP/s
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
					else
						log.warn("El id dispositivo no empieza por ovsdb:");
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
	}
}


