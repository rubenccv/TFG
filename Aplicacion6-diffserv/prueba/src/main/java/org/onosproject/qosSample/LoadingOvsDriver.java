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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.apache.karaf.shell.api.action.Argument;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;

/**
 * Query Qos Command.
 */

@Service
@Command(scope = "onos", name = "qos-loading-driver",
        description = "Qos Test.")
public class LoadingOvsDriver extends AbstractShellCommand {

    @Argument(index = 0, name = "ovsdbDeviceId", description = "the ovsdb device id.", required = true,
            multiValued = false)
    String ovsdbDeviceId = null;

    @Override
    protected void doExecute() {
        dynamicLoadingDriver(DeviceId.deviceId(ovsdbDeviceId));
    }

    private void dynamicLoadingDriver(DeviceId deviceId) {
        NetworkConfigService configService = DefaultServiceDirectory.getService(NetworkConfigService.class);

        BasicDeviceConfig config = configService.addConfig(deviceId,
                BasicDeviceConfig.class);
        config.driver("ovs");
        configService.applyConfig(deviceId, BasicDeviceConfig.class, config.node());
    }
}
