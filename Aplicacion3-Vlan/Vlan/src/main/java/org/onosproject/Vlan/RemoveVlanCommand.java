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
package org.onosproject.Vlan;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.apache.karaf.shell.api.action.Argument;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import org.onosproject.Vlan.VlanbyMac;
/**
 * Sample reactive forwarding application.
 */
@Service
@Command(scope = "onos", name = "remove-Vlan-Mac",
		description = "Remove a Vlan to an existing Mac")
public class RemoveVlanCommand extends AbstractShellCommand {
	
	@Argument(index = 0, name = "mac", description = "One Mac Address",
	required = true, multiValued = false)
	//@Completion(MacAddressCompleter.class)
    String mac = null;

	@Argument(index = 1, name="vlan", description = "Vlan to be asigned to a MAC",
					required = true, multiValued = false)
	String vlan=null;

    @Override
    protected void doExecute() {
        VlanbyMac VlanbyMacService = AbstractShellCommand.get(VlanbyMac.class);
        MacAddress macAddress = null;
        VlanId vlanId = null;
    
        if (mac != null && vlan!=null) {
            macAddress = MacAddress.valueOf(mac);
            vlanId = VlanId.vlanId(vlan);
            get(HostAdminService.class).removeHost(HostId.hostId(mac));
            VlanbyMacService.removeVlanMac(vlanId, macAddress);
        }  
        VlanbyMacService.printAddVlanMac(macAddress,vlanId);
    }
}




