# TFG - TO DO list: DONE(✓), TO-BE-DONE(✗)

✓ Adaptar la app OnePing a la versión ONOS 2.1.0

✓ Crear la app SeveralPing para permitir más de un solo ping

✓ Modificar la app OnePing/SeveralPing para que sólo bloquee el tráfico ICMP y sólo cuente los ICMP Echo Request (para que el tráfico se bloquee en un único sentido y no bidireccionalmente)

✓ Crear la app StatsShow que muestra las estadísticas de tráfico de cada puerto de cada dispositivo conectado al controlador

✓ Crear la app DetectHost que muestra los hosts conectados a cada dispositivo. 

✓ Crear la app DetectHostBan que bloquea los puertos de un dispositivo conectado al controlador cuando el tráfico excede un umbral prefijado

✓ Modificar el código de la app DetectHostBan para que el sistema mantenga las estadísticas para cada MAC, con independencia del puerto al que se conecte

✓ Crear una app que permita asignar la VLAN basado en la dirección MAC del host. En una red con 4 VLANs diferentes para el router  y con un switch (OpenVSwitch) que soporta 802.1q, comprobar el correcto funcionamiento (el host se asigna a la VLAN que le corresponde según la MAC que tenga).

✓ Crear una app que permita adaptar el número de enlaces troncales a ser agregados basado en el tráfico que está recibiendo el switch. En una red con 2 switches (Open vSwitch) diferentes interconectados entre sí mediante varios cables (x4), un número variable de enlaces troncales entre switches serán empleados acorde al tráfico que soporta la red en dicho instante.

✓ En la App 3 crear un comando que permita añadir y eliminar correspondencias MAC-VLAN, de manera que no existan únicamente las correspondencias estáticas programadas en el código fuente.

-------------------------------------------------------------------------------------------------
✗ Crear una app que permita bloquear el tráfico DHCP (DHCPOFFER) procedente de puertos que no sea al que se conecta el router principal. Para ello se crea una regla en el dispositivo OpenFlow (Open vSwitch) que mande el tráfico DHCP al controlador. En el controlador se mira si el tráfico es DHCP y si es un DHCPOFFER. Si lo es, y el puerto por el que ha llegado es el que conecta con el router, se permite enviar ese paquete. En caso contrario, se bloquea el envío de dicho paquete para evitar que un host malintencionado desconfigure los hosts de la red.

✗ Balancear tráfico de manera que no se cree una regla (FlowRule) para un solo posible camino, sino diferentes rutas alternativas

✗ Limitar el tráfico (en Mbps) para cada Flow Rule (usar meters)

https://groups.google.com/a/onosproject.org/forum/#!topic/onos-discuss/USB2ryD_RA4
https://blog.sflow.com/2018/04/onos-traffic-analytics.html

✗ Emplear el lenguaje P4 (https://p4.org/) para definir un protocolo experimental y poderlo meter en los campos del matching (selector Pi) para crear una regla al estilo de OpenFlow



--------------------------------Memoria---------------------------------------------------------
Anexo 1: Instalación de Eclipse
Anexo 2: Introducción a la programación por objetos 
