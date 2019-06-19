/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.qosSample;


import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.util.Bandwidth;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Argument;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.*;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;

import java.util.HashMap;
import java.util.Map;

/**
 * Query Qos Command.
 */
@Service
@Command(scope = "onos", name = "qos-query",
        description = "Qos Test.")
public class QoSQueryCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "controllerid", description = "the CONTROLLER TYPE device id.", required = true,
            multiValued = false)
    String controllerid = null;

    @Override
    protected void doExecute() {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        Device device = deviceService.getDevice(DeviceId.deviceId(controllerid));
        if (device == null) {
            log.error("{} isn't support config.", controllerid);
            return;
        }

        QueueConfigBehaviour queueConfig = device.as(QueueConfigBehaviour.class);
        QosConfigBehaviour qosConfig = device.as(QosConfigBehaviour.class);
        PortConfigBehaviour portConfig = device.as(PortConfigBehaviour.class);

        queueConfig.getQueues().stream().forEach(q -> {
            print("name=%s, type=%s, dscp=%s, maxRate=%s, " +
                            "minRate=%s, pri=%s, burst=%s", q.queueId(), q.type(),
                    q.dscp(), q.maxRate(), q.minRate(),
                    q.priority(), q.burst());
        });
        qosConfig.getQoses().forEach(q -> {
            print("name=%s, maxRate=%s, cbs=%s, cir=%s, " +
                            "queues=%s, type=%s", q.qosId(), q.maxRate(),
                    q.cbs(), q.cir(), q.queues(), q.type());
        });

    }
}
