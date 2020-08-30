/*
 * Copyright 2017-present Open Networking Foundation
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


package org.onosproject.diffserv;

import java.util.HashMap;
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
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QosRestManager {

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

    }
    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }




    public void addQueue(String idQueue, int maxRate, int minRate, int portQoS, int portQueue) {

        //Vemos las dispositivos que tiene el router, si el dispositivo empieza
        //por ovsdb es sobre el que podemos realizar QoS (no sobre el otro),
        //para ello previamente es necesario en el ONOS obtener este controlador y
        //modificarle los drivers a ovs
        //(que se hace cargando la aplicacion drivers-ovsdb)
        for (Device d: deviceService.getAvailableDevices()) {
            log.info("Dispositivo:  {}", d.id());
            if (d.is(null)) {
                log.info("No hay dispositivos conectados a la red");
            } else {
                if (d.is(QueueConfigBehaviour.class) && d.is(PortConfigBehaviour.class)) {
                    if (d.id().toString().startsWith("ovsdb:")) {
                        QueueConfigBehaviour queueConfig = d.as(QueueConfigBehaviour.class);
                        QosConfigBehaviour qosConfig = d.as(QosConfigBehaviour.class);
                        PortConfigBehaviour portConfig = d.as(PortConfigBehaviour.class);


                        //Creamos la cola con los parámetros enviados en el POST por el usuario
                        QueueDescription queueDesc = DefaultQueueDescription.builder()
                                .queueId(QueueId.queueId(idQueue))
                                .maxRate(Bandwidth.bps(maxRate))
                                .minRate(Bandwidth.bps(minRate))
                                .build();


                        queueConfig.addQueue(queueDesc);




                        Map<Long, QueueDescription> queues = new HashMap<>();
                        queues.put(Long.parseLong("idQueue", 10), queueDesc);



                        //Creamos el puerto sobre el que se aplicará la cola
                        String sport = Integer.toString(portQueue);
                        String interfaz = "eth" + sport;


                        PortDescription portDesc = DefaultPortDescription.builder()
                                .isEnabled(true)
                                .withPortNumber(PortNumber.portNumber(portQueue, interfaz))
                                .build();


                        //Creamos la calidad de servicio
                        QosDescription qosDesc = DefaultQosDescription.builder()
                                .qosId(QosId.qosId("qos1"))
                                .type(QosDescription.Type.HTB)
                                .maxRate(Bandwidth.bps(Long.valueOf("100000")))
                                .queues(queues)
                                .build();

                        qosConfig.addQoS(qosDesc);
                        portConfig.applyQoS(portDesc, qosDesc);


                    } else {
                        log.error("El id dispositivo no empieza por ovsdb");
                        TrafficSelector selector = DefaultTrafficSelector.builder()
                                .matchInPort(PortNumber.portNumber(portQueue))
                                //.matchEthType(EthType.EtherType.
                                //IPV4.ethType().toShort())
                                //.matchIPProtocol(IPv4.PROTOCOL_TCP)
                                //.matchTcpDst(TpPort.tpPort(1230))
                                .build();


                        TrafficTreatment drop = DefaultTrafficTreatment.builder()
                                .setQueue(Long.parseLong(idQueue, 10))
                                .setOutput(PortNumber.NORMAL)
                                .build();


                        //Creamos la regla que indique que el tráfico del puerto elegido
                        //por el usuario vaya por la cola creada.
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
            }
        }
    } //Cierre addQos

} //Cierre de la clase
