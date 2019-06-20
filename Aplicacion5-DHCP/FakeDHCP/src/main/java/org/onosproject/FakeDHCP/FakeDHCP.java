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
package org.onosproject.FakeDHCP;

import com.google.common.base.Strings;

import org.onlab.packet.DHCP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Optional;

import static org.onosproject.FakeDHCP.OsgiPropertyConstants.PUERTO_ROUTER;
import static org.onosproject.FakeDHCP.OsgiPropertyConstants.PUERTO_ROUTER_DEFAULT;


/**
 * Aplicacion que sabiendo el puerto en el que esta el servidor DHCP impide las ofertas de otros sitios
 * evitando ofertas de otros sitios no confiables.
 */
@Component(immediate = true,
property = {
		PUERTO_ROUTER + ":Integer=" + PUERTO_ROUTER_DEFAULT,
})
public class FakeDHCP{

	private final Logger log = LoggerFactory.getLogger(getClass());

	/** Some configurable property. */
	private int PUERTO_ROUTER = PUERTO_ROUTER_DEFAULT;

	private ApplicationId appId;
	private final PacketProcessor packetProcessor = new DHCPProcessor();


	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected ComponentConfigService cfgService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected CoreService coreService;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	protected PacketService packetService;

	/*Hacemos que todo el trafico DHCP se mande al controlador. 
    El trafico DHCP se envia por el puerto UDP 67*/

	
	/*Recordar que esta regla tiene que recorrer todas las capas TCP.
	No vale con poner directamente UDP sino que hay que pasar por Ethernet e IP primer*/
	TrafficSelector selector = DefaultTrafficSelector.builder()
			.matchEthType(Ethernet.TYPE_IPV4)
			.matchIPProtocol(IPv4.PROTOCOL_UDP)
			.matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
			.matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
			.build();


	@Activate
	protected void activate(ComponentContext context) {
		appId = coreService.registerApplication("org.onosproject.dhcp",
				() -> log.info("Periscope down."));
		cfgService.registerProperties(getClass());

		//Servicio que envia el trafico al controlador segun el selector creado
		packetService.requestPackets(selector, PacketPriority.CONTROL, appId,
				Optional.empty());

		//Una vez tenemos el trafico en el controlador creamos un procesador que procese todos los paquetes
		packetService.addProcessor(packetProcessor, 128);

		modified(context);
		log.info("Activada aplicacion DHCP");
	}

	//Una vez activada la aplicacion se activa un procesador de paquetes que va a coger aquellos que son DHCP unicamente
	private class DHCPProcessor implements PacketProcessor {
		@Override
		public void process(PacketContext context) {
			Ethernet eth = context.inPacket().parsed();
			if (isDHCP(eth)) {
				log.info("Paquete DHCP OFFER recibido");
				
				//Procesamos el paquete
				if(context.inPacket().receivedFrom().port().toLong()!=PUERTO_ROUTER) {
					log.error("Paquete bloqueado por venir de servidor DHCP desconocido");
					
				int IPorigenint = ((IPv4)context.inPacket().parsed().getPayload()).getSourceAddress();
				IpAddress IPorigen = IpAddress.valueOf(IPorigenint);					
					log.error("   El paquete procede de la direccion IP: {} y del puerto: {}",IPorigen,context.inPacket().receivedFrom().port());
					context.block();
				}
			}
		}
		private boolean isDHCP(Ethernet eth) {
			return (eth.getEtherType() == Ethernet.TYPE_IPV4) && (((IPv4) eth.getPayload()).getProtocol() == IPv4.PROTOCOL_UDP)  &&(((DHCP)(((UDP)((IPv4) eth.getPayload()).getPayload())).getPayload()).getPacketType() == DHCP.MsgType.DHCPOFFER);
		}
	}
	@Deactivate
	protected void deactivate() {
		cfgService.unregisterProperties(getClass(), false);
        packetService.removeProcessor(packetProcessor);
        packetService.cancelPackets(selector, PacketPriority.CONTROL, appId);
		log.info("Desactivada aplicacion DHCP");
	}
	
    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String s = Tools.get(properties, "PUERTO_ROUTER");
        PUERTO_ROUTER = Strings.isNullOrEmpty(s) ? PUERTO_ROUTER_DEFAULT : Integer.parseInt(s.trim());

        log.info("Puerto al que se conecta el router se ha cambiado al {}",PUERTO_ROUTER);
    }
}
