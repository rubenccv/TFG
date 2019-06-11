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

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;

import static org.onosproject.statsshow.OsgiPropertyConstants.TASK_PERIOD;
import static org.onosproject.statsshow.OsgiPropertyConstants.TASK_PERIOD_DEFAULT;


import java.util.Timer;
import java.util.TimerTask;
import java.util.Dictionary;
import java.util.List;

/**
 * Sample application that shows traffic statistics every few seconds.
 */

@Component(
	    immediate = true,
	    service = showStatistics.class,
	    property = {
	    		TASK_PERIOD + ":Integer=" + TASK_PERIOD_DEFAULT,
	    }
	)

public class showStatistics {

    private final Logger log = LoggerFactory.getLogger(showStatistics.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;
    
    /** Configure the periodicity of the task */
    private int TASK_PERIOD = TASK_PERIOD_DEFAULT;

    protected Timer timer; 

    private ApplicationId appId;

    @Activate
    public void activate(ComponentContext context) {
    	
    	 appId = coreService.registerApplication("org.onosproject.severalping",
                 () -> log.info("Periscope down."));
    	
    	cfgService.registerProperties(getClass());
    	modified(context);
    	log.info("Activada aplicacion statsshow");

    	
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
        long period = 1000L * (long)TASK_PERIOD; // Every 30 seconds (by default) we get the statistics
        timer.scheduleAtFixedRate(repeatedTask, delay, period);
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        timer.cancel();
        log.info("Aplicacion statsshow desactivada");
    }
    
  
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        
        String s = Tools.get(properties, "TASK_PERIOD");
        log.error("Cadena: " +s);
        TASK_PERIOD = Strings.isNullOrEmpty(s) ? TASK_PERIOD_DEFAULT : Integer.parseInt(s.trim());
 
        log.info("Propiedad cambiada a: {} segundos de periodicidad",TASK_PERIOD);
    }
    
}
