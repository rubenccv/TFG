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
package org.onosproject.severalping;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.ICMP;
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
    private final PacketProcessor packetProcessor = new PingPacketProcessor();
    private final FlowRuleListener flowListener = new InternalFlowListener();

   // static byte	TYPE_ECHO_REQUEST = 8;

    /*Pequeña explicacion para la memoria:
     * Todo este codigo se ejecuta en el controlador, por tanto, para poder contabilizar los pings 
     * que esta habiendo es necesario que todos los REQUEST vayan al controlador, para ello creamos la regla
     * llamada intercept que se ejecuta en el activate
     * 
     * Ademas tambien creamos un procesador de paquetes para detectar si el paquete recibido es ICMP 
     * y poder trabajar con el. Una vez comprobado que es ICMP REQUEST con la funcion isICMP 
     * sumamos el ping en el HashMap
     * 
     * Si el numero de pings es menor que el maximo simplemente decimos que se sume 1 y llamos a PingPruner
     * Este metodo lo que hace es quitar 1 del HashMap de numero de pings pasado 1 minuto
     * 
     * Si el numero de pings es mayor que el maximo creamos una regla que se enviara al openVswitch 
     * y se instalara alli. La regla lo que dice es que banee los ICMP Request que se envien 
     * desde ese origen a ese destino exactamente permitiendo asi poder responder a los Reply de otros 
     * pings que se puedan enviar
     * 
     * Finalmente ponemos un listener para escuchar los flujos que se eliminan. 
     * De esta forma cuando la regla se ha borrado lo notificamos
     */    
    private final TrafficSelector intercept = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_ICMP).matchIcmpType(ICMP.TYPE_ECHO_REQUEST)
            .build();

    private final HashMap<PingRecord,Integer> pings = new HashMap<PingRecord,Integer>();
    private final Timer timer = new Timer("severalping-sweeper");

    public AppComponent()
    {
        
    }

    @Activate //Cuando activamos la aplicacion desde ONOS se ejecuta este metodo
    public void activate() {	
        appId = coreService.registerApplication("org.onosproject.severalping",
                                                () -> log.info("Periscope down."));
        packetService.addProcessor(packetProcessor, PRIORITY);
        flowRuleService.addListener(flowListener);
        packetService.requestPackets(intercept, PacketPriority.CONTROL, appId,
                                     Optional.empty());
        
       
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(packetProcessor);
        packetService.cancelPackets(intercept, PacketPriority.CONTROL, appId);
        flowRuleService.removeFlowRulesById(appId);
        flowRuleService.removeListener(flowListener);
        log.info("Stopped");
    }


    //Una vez activada la aplicacion se activa un procesador de paquetes que va a coger aquellos que son ICMP unicamente
    private class PingPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            Ethernet eth = context.inPacket().parsed();
            if (isIcmpPing(eth)) {
            	log.error("Paquete ICMP recibido");
                processPing(context, eth); //Llamamos al metodo processPing
            }
        }
    }
    
    //Este metodo procesa el paquete ICMP recibido.  
    private void processPing(PacketContext context, Ethernet eth) {
    	//En primer lugar cogemos el dispositivo y las MAC origen y destino del paquete
    	//creamos un PingRecord cuyo constructor esta abajo para obtener del HashMap ping 
    	//el numero de pings entre esa MAC origen y destino que ha habido
   
        DeviceId deviceId = context.inPacket().receivedFrom().deviceId();
        MacAddress src = eth.getSourceMAC();
        MacAddress dst = eth.getDestinationMAC();
        PingRecord ping = new PingRecord(deviceId, src, dst);
        Integer num_pings = pings.get(ping);

        if (num_pings==null) {
            num_pings=0;
        }
        if (num_pings<LIM_PINGS) {
            // Less pings than allowed detected; track it for the next minute
            log.info(MSG_PINGED_OK, num_pings+1, LIM_PINGS, src, dst);
            pings.put(ping,num_pings+1);
         
            //Crea una tarea en 60 segundos para que se quite 1 de ellos.
            timer.schedule(new PingPruner(ping), TIMEOUT_SEC * 1000);
        }
        else {
         	//En caso de que sea mayor que el umbral que tenemos llamamos al metodo banPings y bloqueamos el paquete a la salida
            log.warn(MSG_PINGED_LIMIT, LIM_PINGS, src, dst);
            banPings(deviceId, src, dst);
            context.block();
        }
    }

    //Creamos una regla temporal en el openVswitch que descarte los paquetes ICMP entre src y dst.
    private void banPings(DeviceId deviceId, MacAddress src, MacAddress dst) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthSrc(src).matchEthDst(dst).matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_ICMP).matchIcmpType(ICMP.TYPE_ECHO_REQUEST).build();
        TrafficTreatment drop = DefaultTrafficTreatment.builder()
                .drop().build();

        flowObjectiveService.forward(deviceId, DefaultForwardingObjective.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(drop)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(DROP_PRIORITY)
                .makeTemporary(TIMEOUT_SEC)
                .add());
        
        //log.error("Regla creada entre {} y {} que descarta los paquetes",src,dst);
    }


    // Indicates whether the specified packet corresponds to ICMP ping.
    private boolean isIcmpPing(Ethernet eth) {
        return eth.getEtherType() == Ethernet.TYPE_IPV4 &&
                ((IPv4) eth.getPayload()).getProtocol() == IPv4.PROTOCOL_ICMP && ((ICMP)((IPv4) eth.getPayload()).getPayload()).getIcmpType()==ICMP.TYPE_ECHO_REQUEST ;
    }



    // Record of a ping between two end-station MAC addresses
    private class PingRecord {
        private final DeviceId deviceId;
        private final MacAddress src;
        private final MacAddress dst;

        PingRecord(DeviceId deviceId, MacAddress src, MacAddress dst) {
            this.deviceId = deviceId;
            this.src = src;
            this.dst = dst;
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceId,src, dst);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final PingRecord other = (PingRecord) obj;
            return Objects.equals(this.deviceId, other.deviceId) && Objects.equals(this.src, other.src) && Objects.equals(this.dst, other.dst);
        }
    }

    
    // Realmente esto es necesario? Si ya aumentas en 1 el numero de pings arriba no haria falta yo creo
    private class PingPruner extends TimerTask {
        private final PingRecord ping;

        public PingPruner(PingRecord ping) {
            this.ping = ping;
        }

        @Override
        public void run() {
           Integer num_pings = pings.get(ping);

           if (num_pings!=null) {
               if(num_pings==0)
               {
                   pings.remove(ping);
               }
               else {
                   pings.put(ping,num_pings-1);
               }
           }
        }
    }

    // Listens for our removed flows.
    private class InternalFlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            FlowRule flowRule = event.subject();
            if (event.type() == RULE_REMOVED && flowRule.appId() == appId.id()) {
                Criterion criterion = flowRule.selector().getCriterion(ETH_SRC);
                MacAddress src = ((EthCriterion) criterion).mac();
                criterion = flowRule.selector().getCriterion(ETH_DST);
                MacAddress dst = ((EthCriterion) criterion).mac();
                log.warn(MSG_PING_REENABLED, src, dst);
            }
        }
    }
}
