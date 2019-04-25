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
package org.onosproject.banstatsshow;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
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
import java.util.Calendar;

/**
 * Sample application that shows traffic statistics every few seconds.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(AppComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;
    
    private ApplicationId appId;
    protected Timer timer; 
    public long LIMIT_MB = 1000000; //1MB de maximo de datos
    private static final int DROP_PRIORITY = 129;
    

    private final HashMap<TrafficRecord,Boolean> baneos = new HashMap<TrafficRecord,Boolean>();
    
    private final FlowRuleListener flowListener = new InternalFlowListener();
    
    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.banstatsshow",
                () -> log.info("Periscope down."));
        flowRuleService.addListener(flowListener); //Indicamos que empiece a escuchar por reglas (en este caso que borre lineas del HashMap)
        log.info("Started");

        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                Iterable<Device> devices = deviceService.getDevices(); //Obtenemos los dispositivos controlados por el ONOS (en el primer ejemplo solo el openVSwitch)

                for(Device d : devices)
                {
                    log.info("#### [statsshow] Device id " + d.id().toString());

                    List<Port> ports = deviceService.getPorts(d.id());
                    for(Port port : ports) {
                        log.info("#### [statsshow] Port number " + port.number());
                        PortStatistics portstat = deviceService.getStatisticsForPort(d.id(), port.number());
                        if(portstat != null)
                        {
                            log.info("portstat bytes received: " + portstat.bytesReceived());
                            log.info("portstat bytes sent: " + portstat.bytesSent());
                            //almacenamos en la estructura el puerto y los datos usados y restantes
                            long bytesAvailable = LIMIT_MB - portstat.bytesSent(); 
                            TrafficRecord ban = new TrafficRecord(d.id(),port.number());
                          //Si los bytes disponibles son 0 se banea ese puerto
                            
                            if((bytesAvailable<=0) && (port.number().toLong()!=0) && (!baneos.containsKey(ban))) { //Hacemos que el puerto que comunica el ONOS con el switch no sea bloqueado 
                            	banTraffic(d.id(),port.number());
                            	baneos.put(ban, true);
                            	log.info("You have reached the limit of data");
                            }
                        }
                        else
                        {
                            log.info("Unable to read portStats.");
                        }
                        PortStatistics portdeltastat = deviceService.getDeltaStatisticsForPort(d.id(), port.number());

                        if(portdeltastat != null)
                        {
                            log.info("portdeltastat bytes received: " + portdeltastat.bytesReceived());
                            log.info("portdeltastat bytes sent: " + portdeltastat.bytesSent());
                        }
                        else
                        {
                            log.info("Unable to read portDeltaStats.");
                        }
                    }
                }
            }
        };
        timer = new Timer("Timer");

        long delay = 1000L; // We start polling statistics after 1 second
        long period = 1000L * 30L; // Every 30 seconds we get the statistics
        timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }
    
    
    //Creamos el metodo que banea el trafico segun el puerto que supere el limite
    public void banTraffic(DeviceId deviceId, PortNumber numport) {
    	 TrafficSelector selector = DefaultTrafficSelector.builder().matchInPort(numport).build();
    	 TrafficTreatment drop = DefaultTrafficTreatment.builder().drop().build();
    	 
    	//Haremos que el baneo de tiempo sea hasta las 00:00 del dia siguiente
    	 Calendar calendario = Calendar.getInstance();
    	 
    	 calendario.add(Calendar.DAY_OF_MONTH, 1);
    	 calendario.set(Calendar.HOUR_OF_DAY, 0);
    	 calendario.set(Calendar.MINUTE, 0);
    	 calendario.set(Calendar.SECOND, 0);
    	 calendario.set(Calendar.MILLISECOND, 0);
    	 
    	 long horaActual = System.currentTimeMillis();
    	 // Obtenemos los milisegundos de la proxima medianoche
    	 long howMany = calendario.getTimeInMillis();

    	 // Cuandos milisegundos har de siferencia
    	 long milisegundos = howMany - horaActual;
    	 
    	 int segundos = (int)milisegundos/1000;
    	 
         flowObjectiveService.forward(deviceId, DefaultForwardingObjective.builder()
                 .fromApp(appId)
                 .withSelector(selector)
                 .withTreatment(drop)
                 .withFlag(ForwardingObjective.Flag.VERSATILE)
                 .withPriority(DROP_PRIORITY)
                 .makeTemporary(60*1)
                 .add());
    }
    
    //Creamos una estructura que almacene el dispositivo, numero de puerto y un booleano donde almacenemos si el puerto esta baneado o no
    private class TrafficRecord{
    	private final DeviceId deviceId;
        private final PortNumber numport;
        

        TrafficRecord(DeviceId deviceId, PortNumber numport) {
            this.deviceId = deviceId;
        	this.numport = numport;

        	
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(deviceId,numport);
        }

        //Sobreescribimos el metodo equals para que no solo compare los identificadores de  objeto
        //sino tambien si los atributos que contiene el objeto son iguales (en este caso el id y el numero de puerto
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final TrafficRecord other = (TrafficRecord) obj;
            return Objects.equals(this.deviceId, other.deviceId) && Objects.equals(this.numport, other.numport);
        }
        
    }

    @Deactivate
    protected void deactivate() {
        timer.cancel();
        flowRuleService.removeFlowRulesById(appId);
        flowRuleService.removeListener(flowListener);
        log.info("Stopped");
    }
    
    
    // Elimina del Hash Map aquellas lineas de las que ya ha pasado el tiempo 
    private class InternalFlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            FlowRule flowRule = event.subject();
            if (event.type() == RULE_REMOVED && flowRule.appId() == appId.id()) {
                Criterion criterion = flowRule.selector().getCriterion(Type.IN_PORT);
               PortNumber numport = ((PortCriterion) criterion).port();

               DeviceId deviceId = event.subject().deviceId();
               TrafficRecord ban = new TrafficRecord(deviceId,numport);
               baneos.remove(ban);
                log.warn("Reenabled link");
            }
        }
    }
}


