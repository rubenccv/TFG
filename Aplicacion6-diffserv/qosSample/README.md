# qos-sample
QoS sample application

This is a simple app about QoS on ONOS, you can run it by following these steps

1. put this in onos/apps directory    
   cd onos/apps      
   git clone https://github.com/JackSunshine/qos-sample.git
   
2. modify onos/modules.defs     
   ONOS_APPS = [    
    ...       
    '//apps/fwd:onos-apps-fwd-oar',   
    '//apps/qos-sample:onos-apps-qos-sample-oar',    
    ...     
    
3. build onos with tools/build/onos-buck run onos-local -- clean debug      
   To attach to the ONOS CLI console tools/test/bin/onos localhost    
   then start qos-sample app    
   app activate org.onosproject.qos-sample
   
4. setup your OpenvSwitch with     
   ovs-vsctl set-manager tcp:127.0.0.1     
   ovs-vsctl set-controller br0 tcp:127.0.0.1
   
5. change ovsdb device default driver to ovs by qos-loading-driver command      
   
   qos-loading-driver ovsdb:127.0.0.1    
   
6. create/delete/query QoS by qos-add/del/query command    
    
   qos-add ovsdb:127.0.0.1 port-name qos/queue-id 1000 1000    
   qos-del ovsdb:127.0.0.1 port-name    
   qos-query ovsdb:127.0.0.1

