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

✓ Crear una app que permita bloquear el tráfico DHCP (DHCPOFFER) procedente de puertos que no sea al que se conecta el router principal. Para ello se crea una regla en el dispositivo OpenFlow (Open vSwitch) que mande el tráfico DHCP al controlador. En el controlador se mira si el tráfico es DHCP y si es un DHCPOFFER. Si lo es, y el puerto por el que ha llegado es el que conecta con el router, se permite enviar ese paquete. En caso contrario, se bloquea el envío de dicho paquete para evitar que un host malintencionado desconfigure los hosts de la red.

-------------------------------------------------------------------------------------------------

✗ Balancear tráfico de manera que no se cree una regla (FlowRule) para un solo posible camino, sino diferentes rutas alternativas

✗ Limitar el tráfico (en Mbps) para cada Flow Rule (usar meters)

https://groups.google.com/a/onosproject.org/forum/#!topic/onos-discuss/USB2ryD_RA4
https://blog.sflow.com/2018/04/onos-traffic-analytics.html

✗ DiffServ:

	Aplicacion hecha: 1 cola con 1 QoS explicar comandos ovs y toda la pesca

	Probar parametros de las colas (cir, cbs) y diferentes tipos de colas

	Colas en funcion del tráfico entrante. Si es ARP menor velocidad que si es VoIP, este con maxima velocidad.Verlo con netcat



✗ Emplear el lenguaje P4 (https://p4.org/) para definir un protocolo experimental y poderlo meter en los campos del matching (selector Pi) para crear una regla al estilo de OpenFlow


✗ App para la creación dinámica de un árbol para tráfico multicast. Cuando un host quiera añadirse a un grupo multicast se envía un "join" IGMP (Internet Group Management Protocol), por lo que el switch puede detectar dicha notificación y crear una regla de OpenFlow que añada el puerto al que se conecta el host que realiza el "join" a la lista de puertos por los que realizar el multicast.


✗ App para la creación de un cortafuegos (firewall) dinámico basado en patrones de tráfico. Por ejemplo, cuando se observe un host (dirección IP) que está enviando tráfico "sospechoso" de ser un ataque de denegación de servicio (DoS), se descartará todo el tráfico asociado.


✗ App para la aplicación de una lista de control de accesso (ACL), es decir, permitir el switching normal entre hosts autorizados, pero descartar todo el tráfico de host no autorizados. El nivel de autorización se hará a nivel de dirección MAC (L2).

Realizar una matriz de tráfico a partir de los bytes que pasan por cada enlace


# ------------Memoria------------

Anexo 1: Instalación de Eclipse

Anexo 2: Introducción a la programación por objetos 
