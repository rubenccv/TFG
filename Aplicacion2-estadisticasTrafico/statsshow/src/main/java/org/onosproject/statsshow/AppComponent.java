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
package org.onosproject.statsshow;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Sample application that shows traffic statistics every few seconds.
 */
@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(AppComponent.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    protected Timer timer; 

    @Activate
    protected void activate() {
        log.info("Started");

        TimerTask repeatedTask = new TimerTask() {
            public void run() {
                Iterable<Device> devices = deviceService.getDevices();

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

    @Deactivate
    protected void deactivate() {
        timer.cancel();
        log.info("Stopped");
    }
}
