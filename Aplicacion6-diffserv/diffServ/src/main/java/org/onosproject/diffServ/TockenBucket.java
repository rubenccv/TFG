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
import java.util.Iterator;

import org.onlab.util.Bandwidth;
import org.onosproject.net.Device;
import org.onosproject.net.behaviour.DefaultQueueDescription;
import org.onosproject.net.behaviour.QueueConfigBehaviour;
import org.onosproject.net.behaviour.QueueDescription;
import org.onosproject.net.behaviour.QueueId;
import org.onosproject.net.device.DeviceService;
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
        	log.warn("Entro aqui");
        	if(d.is(null)){
        		log.info("No hay dispositivos conectados a la red");
        	}
        	else {
        		if(d.is(QueueConfigBehaviour.class)){ 
        			QueueConfigBehaviour queueConfig = d.as(QueueConfigBehaviour.class); 
        			log.error(""+queueConfig.data().deviceId().toString());
        			log.error(""+queueConfig.data().driver().name());
        			log.warn("Dispositivo: "+d.id());
        			
        			//Creamos una cola
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

        	/*		
        			Iterator<QueueDescription> it = queueConfig.getQueues().iterator();
        			log.warn("Y aqui tambien");
        			while(it.hasNext()) {
        				log.error("Colas: "+it.toString());
        			}
        		*/	
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


