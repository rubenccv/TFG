/*
 * Copyright 2018-present Open Networking Foundation
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

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }
    static final String MAX_PINGS = "MAX_PINGS";
    static final int MAX_PINGS_DEFAULT = 7;
    
    static final String TIME_BAN = "TIME_BAN";
    static final int TIME_BAN_DEFAULT = 60;
    
    
    /*static final String PACKET_OUT_ONLY = "packetOutOnly";
    static final boolean PACKET_OUT_ONLY_DEFAULT  = false;*/
}