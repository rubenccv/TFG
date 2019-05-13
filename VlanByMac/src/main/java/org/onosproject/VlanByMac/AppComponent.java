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


import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.packet.PacketContext;
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
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TableId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProbingService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.ProbeMode;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

/**
 * Esta app permite enviar trafico asignando VLANs. 
 * Para ello el trafico que llega al openVswitch es etiquetado con una VLAN (rule1) acorde con un HashMap 
 * previamente definido. 
 * Cuando el trafico va a ser enviado a un host destino se comprueba que pertenecen a la misma VLAN, 
 * y en caso de ser correcto, el openVSwitch quita la VLAN (rule2) y se envia por el puerto correspondiente
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
	
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

	int priority = 65000;
    private final HashMap<MacAddress,VlanId> macVlanMap = new HashMap<MacAddress,VlanId>();    
    
    //private final HashMap<VlanId,ArrayList<PortNumber>> vlanPortMap = new HashMap<VlanId,ArrayList<PortNumber>>();    

    private final HashMap<VlanId,FlowRule> vlanRuleMap = new HashMap<VlanId,FlowRule>();    
    
    private final HashMap<MacAddress,FlowRule[]> macRuleMap = new HashMap<MacAddress,FlowRule[]>();    

    private final HostListener hostListener = new InternalHostListener();
    
    @Activate
	protected void activate() {
		appId = coreService.registerApplication("org.onosproject.VlanByMac",
				() -> log.info("Periscope down."));
		log.info("Started");
		hostService.addListener(hostListener);
        //packetService.addProcessor(packetProcessor, 128);
		
		TrafficSelector selector = DefaultTrafficSelector.builder().matchEthType(EthType.EtherType.ARP).build();		
		TrafficTreatment trtr = DefaultTrafficTreatment.builder().setOutput(PortNumber.CONTROLLER).build();


		FlowRule rule = DefaultFlowRule.builder()
				.fromApp(appId)
				.forTable(IndexTableId.of(2))
				.forDevice(event.subject().location().deviceId())
				.makePermanent()
				.withPriority(priority)
				.withSelector(selector)
				.withTreatment(trtr)
				.build();
		
		flowRuleService.applyFlowRules(rule);
        
		//Asignamos las VLAN al hashmap con las diferentes mac que tendremos en nuestra red
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:01"),VlanId.vlanId((short)1));
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:02"),VlanId.vlanId((short)1));
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:03"),VlanId.vlanId((short)2));
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:04"),VlanId.vlanId((short)2));
		
		//MAC del router que intercomunica las VLANs
		macVlanMap.put(MacAddress.valueOf("00:00:00:00:00:05"),VlanId.NONE);
		
	}

	@Deactivate
	protected void deactivate() {
		cfgService.unregisterProperties(getClass(), false);	
		flowRuleService.removeFlowRulesById(appId);
       		hostService.removeListener(hostListener);
		log.info("Stopped");
	}    
	
	private class InternalHostListener implements HostListener {
		@Override
		public void event(HostEvent event) {
			//Cuando se añade un host a la red vemos la VLAN que tiene sacandola del HashMap
			if(event.type()==HostEvent.Type.HOST_ADDED) {
				//TODO Comprobar que el dispositivo en el que esta conectado el host.


				//TODO Configurar inicialmente ciertos puertos del switch como troncales


				//TODO Ver si se inunda si no hay correspondencia con la MAC de destino 
				MacAddress macHost = event.subject().mac();
				VlanId VlanHost = macVlanMap.get(macHost);

				if(VlanHost==null)
				{
					log.warn("Host conectado no reconocido");
					return;
				}

				log.warn("Host conectado reconocido");
				//VlanPortMap.get(VlanHost).add(event.subject().location().port());


				//Creamos una regla para el trafico de broadcast. 
				//Este trafico se tiene que enviar por todos los puertos de la VLAN en la que esta conectado sin etiquetar
				//Y por el puerto del router etiquetado, por ello dividimos en 2 pasos. Primero cogemos la MAC de aquellos con VLAN=NONE
				//Y decimos que envie etiquetado y a continuacion, quitamos la etiqueta y enviamos por los puertos de la VLAN unicamente


				FlowRule ruleBorrar = vlanRuleMap.get(VlanHost);
				
				if(ruleBorrar==null)
					log.error("No se ha encontrado regla para borrar");

				
				//Nota: No es necesario crear la regla de la tabla 0 dado que la tenemos creada en el else de abajo ya.
				TrafficSelector selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.BROADCAST).matchVlanId(VlanHost).build();		
				TrafficTreatment.Builder addVlan1 = DefaultTrafficTreatment.builder();


				Set<MacAddress>macVlan0 = getKeys(macVlanMap,VlanId.NONE);
				for(MacAddress mac: macVlan0) {

					Set<Host> hiterator=hostService.getHostsByMac(mac);

					if(hiterator!=null && hiterator.iterator().hasNext())
					{
						Host h=hiterator.iterator().next();
						addVlan1=addVlan1.setOutput(h.location().port());
					}
				}
				addVlan1=addVlan1.popVlan();

				Set<MacAddress> macVlan = getKeys(macVlanMap,VlanHost);
				for(MacAddress mac: macVlan) {

					Set<Host> hiterator=hostService.getHostsByMac(mac);

					if(hiterator!=null && hiterator.iterator().hasNext())
					{
						Host h=hiterator.iterator().next();
						addVlan1=addVlan1.setOutput(h.location().port());
					}
				}

				TrafficTreatment trtr=addVlan1.transition(2).build();

				FlowRule rule = DefaultFlowRule.builder()
						.fromApp(appId)
						.forTable(IndexTableId.of(1))
						.forDevice(event.subject().location().deviceId())
						.makePermanent()
						.withPriority(priority)
						.withSelector(selector)
						.withTreatment(trtr)
						.build();

				if(ruleBorrar!=null) { //Si hay una regla previa y los hosts cambian la regla hay que borrarla
					flowRuleService.removeFlowRules(ruleBorrar);
				}
				flowRuleService.applyFlowRules(rule);
				vlanRuleMap.put(VlanHost,rule);


				//Distinguimos los casos en los que la VLAN es la 0 (trafico al router) o no (trafico a los hosts)
				
				if(VlanHost.equals(VlanId.NONE)) {
					//Excepcion para el trafico que va dirigido al router (no tiene que quitar la VLAN)

					TrafficSelector selector1 = DefaultTrafficSelector.builder().matchEthSrc(macHost).build();
					TrafficTreatment addVlan = DefaultTrafficTreatment.builder()
							.transition(1).build();

					
					FlowRule rule1 = DefaultFlowRule.builder()
							.fromApp(appId)
							.forTable(IndexTableId.of(0))
							.forDevice(event.subject().location().deviceId())
							.makePermanent()
							.withPriority(priority)
							.withSelector(selector1)
							.withTreatment(addVlan)
							.build();

					//La segunda regla es identica excepto que el trafico que va al router tiene que mantener la VLAN asignada
					TrafficSelector selector2 = DefaultTrafficSelector.builder().matchEthDst(macHost).build();
					TrafficTreatment send = DefaultTrafficTreatment.builder()
							.setOutput(event.subject().location().port()).transition(2).build();

					
					FlowRule rule2 = DefaultFlowRule.builder()
							.fromApp(appId)
							.forTable(IndexTableId.of(1))
							.forDevice(event.subject().location().deviceId())
							.makePermanent()
							.withSelector(selector2)
							.withPriority(priority)
							.withTreatment(send)
							.build();

					
					FlowRule []array = {rule1,rule2};
					flowRuleService.applyFlowRules(array);
					macRuleMap.put(macHost,array);

				}
				
				else {
					TrafficSelector selector1 = DefaultTrafficSelector.builder().matchEthSrc(macHost).build();
					TrafficTreatment addVlan = DefaultTrafficTreatment.builder().pushVlan().setVlanId(VlanHost)
							.transition(1).build();

					//Cuando el host manda trafico se le añade la VLAN
					FlowRule rule1 = DefaultFlowRule.builder()
							.fromApp(appId)
							.forTable(IndexTableId.of(0))
							.forDevice(event.subject().location().deviceId())
							.makePermanent()
							.withPriority(priority)
							.withSelector(selector1)
							.withTreatment(addVlan)
							.build();


					//Regla que quita la VLAN cuando el trafico ha llegado al openVSwitch.
					//Tambien manda el trafico por el puerto en el que esta el host final que cuadre con la MAC destino
					TrafficSelector selector2 = DefaultTrafficSelector.builder()
							.matchEthDst(macHost)
							.matchVlanId(VlanHost)
							.build();
					TrafficTreatment removeVlan = DefaultTrafficTreatment.builder()
							.popVlan()
							.setOutput(event.subject().location().port())
							.transition(2)
							.build();

					FlowRule rule2 = DefaultFlowRule.builder()
							.fromApp(appId)
							.forTable(IndexTableId.of(1))
							.forDevice(event.subject().location().deviceId())
							.makePermanent()
							.withSelector(selector2)
							.withPriority(priority)
							.withTreatment(removeVlan)
							.build();

					FlowRule []array = {rule1,rule2};
					flowRuleService.applyFlowRules(array);
					macRuleMap.put(macHost,array);
				}
			}
			else if(event.type() == HostEvent.Type.HOST_REMOVED) {
				log.warn("Host desconectado");
				FlowRule[] array = macRuleMap.get(event.subject().mac());
				if(array!=null) {
					flowRuleService.removeFlowRules(array);
				}
				
				MacAddress macHost = event.subject().mac();
				VlanId VlanHost = macVlanMap.get(macHost);

				if(VlanHost==null)
				{
					log.warn("Host desconectado no reconocido");
					return;
				}
				FlowRule ruleBorrar = vlanRuleMap.get(VlanHost);

				//Nota: No es necesario crear la regla de la tabla 0 dado que la tenemos creada en el else de abajo ya.
				TrafficSelector selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.BROADCAST).matchVlanId(VlanHost).build();		
				TrafficTreatment.Builder addVlan1 = DefaultTrafficTreatment.builder();


				Set<MacAddress>macVlan0 = getKeys(macVlanMap,VlanId.NONE);
				for(MacAddress mac: macVlan0) {

					Set<Host> hiterator=hostService.getHostsByMac(mac);

					if(hiterator!=null)
					{
						if(hiterator.iterator().hasNext())
						{
							Host h=hiterator.iterator().next();
							addVlan1=addVlan1.setOutput(h.location().port());
						}
					}
				}
				addVlan1=addVlan1.popVlan();

				Set<MacAddress> macVlan = getKeys(macVlanMap,VlanHost);
				for(MacAddress mac: macVlan) {

					Set<Host> hiterator=hostService.getHostsByMac(mac);

					if(hiterator!=null && hiterator.iterator().hasNext())
					{
						Host h=hiterator.iterator().next();
						addVlan1=addVlan1.setOutput(h.location().port());
					}
				}


				TrafficTreatment trtr=addVlan1.transition(2).build();

				FlowRule rule = DefaultFlowRule.builder()
						.fromApp(appId)
						.forTable(IndexTableId.of(1))
						.forDevice(event.subject().location().deviceId())
						.makePermanent()
						.withPriority(priority)
						.withSelector(selector)
						.withTreatment(trtr)
						.build();

				if(ruleBorrar!=null) { //Si hay una regla previa y los hosts cambian la regla hay que borrarla

					flowRuleService.removeFlowRules(ruleBorrar);
				}
				flowRuleService.applyFlowRules(rule);
				vlanRuleMap.put(VlanHost,rule);
			}
		}
	} //Cierre internal host listener
	
	public <K, V> Set<K> getKeys(Map<K, V> map, V value) {
	    Set<K> keys = new HashSet<>();
	    for (Entry<K, V> entry : map.entrySet()) {
	        if (entry.getValue().equals(value)) {
	            keys.add(entry.getKey());
	        }
	    }
	    return keys;
	}
	
}//Cierre del appComponent




