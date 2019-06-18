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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.core.CoreService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostEvent.Type;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProbingService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.ProbeMode;




import java.util.Timer;
import java.util.TimerTask;

/**
 * Aplicacion que muestra los hosts conectados a cada openVswitch cada cierto tiempo.
 */

@Component(immediate = true)
public class AppComponent{

	private final Logger log = LoggerFactory.getLogger(AppComponent.class);


	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected CoreService coreService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected HostService hostService;
	
	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected DeviceService deviceService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected HostProbingService hostProbingService;
	

	protected Timer timer1;
	protected Timer timer2;
	int cont;
	int numeroDispositivos;
    private final HostListener hostListener = new InternalHostListener();
    
//    private final HostProbingListener hostProbing = new InternalHostProbing();

	@Activate
	protected void activate() {

        hostService.addListener(hostListener);

		log.info("Started");

		//Obtenemos los hosts que estan conectados a nuestro dispositivos
		TimerTask repeatedTask1 = new TimerTask() {
			public void run() {			
				//Vemos si  los dispositivos conectados a nuestro ONOS estan activos
				numeroDispositivos=0;
				Iterable<Host> setH = hostService.getHosts();
				Iterable<Device> setD = deviceService.getDevices();
				for(Device d: setD) {
					if(d.id().toString().startsWith("of:"))
						numeroDispositivos++;
				}
				
				
				log.info("Number of connected devices is: {} and hosts {} ",numeroDispositivos,hostService.getHostCount());
        		
				cont=0;
				for (Host h:setH) {
					cont++;
					if(numeroDispositivos==1)
						log.info("	Host "+cont+" whose MAC is: "+h.mac()+" is on port: "+h.location().port());
					else
						log.info("	Host "+cont+" whose MAC is: "+h.mac()+" is on port: "+h.location().port()+ "of device: "+h.location().deviceId());
					
				}			
			}      		
		};
		//Esta tarea prueba los hosts que estan conectados
		TimerTask repeatedTask2 = new TimerTask() {
			public void run() {
				Iterable<Host> hosts =  hostService.getHosts();
				for(Host h:hosts) {
					ConnectPoint connectPoint = new ConnectPoint(h.location().elementId(),h.location().port());
					//Esta parte en un principio no funcionaba, hubo que modificar la aplicacion hostProbing
					hostProbingService.probeHost(h, connectPoint,ProbeMode.VERIFY);
				}
			}
		};

		timer1 = new Timer("Timer1");
		timer2 = new Timer("Timer2");
		long delay = 1000L; 
		long period = 1000L * 10L; // Every 10 seconds we get the statistics
		timer1.scheduleAtFixedRate(repeatedTask1, delay, period);
		timer2.scheduleAtFixedRate(repeatedTask2, 3*delay, period);
	}

	@Deactivate
	protected void deactivate() {
		timer1.cancel();
		timer2.cancel();
        hostService.removeListener(hostListener);
		log.info("Stopped");
	}    
	
    // Listens for our removed flows.
    private class InternalHostListener implements HostListener {
        public void event(HostEvent event) {
            Host host = event.subject();
            
            if (event.type() == Type.HOST_REMOVED) {
            	
				numeroDispositivos=0;
				Iterable<Device> setD = deviceService.getDevices();
				for(Device d: setD) {
					if(d.id().toString().startsWith("of:"))
						numeroDispositivos++;
				}
				if(numeroDispositivos==1)
					log.info("Host with MAC {} has been removed from port {}"
							,host.mac(),host.location().port(),host.location());
            		
				else
					log.info("Host with MAC {} has been removed from port {} and device {}"
							,host.mac(),host.location().port(),host.location().deviceId());
            }
        }
    }
    
    //TODO QUE DIFERENCIA HAY ENTRE HOST LISTENER Y HOST PROBING LISTENER?? SI HACEN LO MISMO!
  
    
    /* 
    public class InternalHostProbing implements HostProbingListener{

		@Override
		public void event(HostProbingEvent event) {
			event.type() == Type.HOST_REMOVED;
			
		}
    */	
    	

}

