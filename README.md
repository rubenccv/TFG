# TFG - TO DO list: DONE(✓), TO-BE-DONE(✗)

✓ Adaptar la app OnePing a ONOS 2.0.0

✓ Crear la app SeveralPing para permitir más de un solo ping

✓ Crear la app StatsShow que muestra las estadísticas de tráfico de cada puerto de cada dispositivo conectado al controlador

✓ Crear la app DetectHost que muestra los hosts conectados a cada dispositivo. 

✓ Crear la app DetectHostBan que bloquea los puertos de un dispositivo conectado al controlador cuando el tráfico excede un umbral prefijado

✓ Modificar la app OnePing/SeveralPing para que sólo bloquee el tráfico ICMP y sólo cuente los ICMP Echo Request (para que el tráfico se bloquee en un único sentido y no bidireccionalmente)

✓ Modificar el código de la app DetectHostBan para que el sistema mantenga las estadísticas para cada MAC, con independencia del puerto al que se conecte

✗ Crear una app que permita asignar la VLAN basado en la dirección MAC del host. En una red con 4 VLANs diferentes para el router  y con un switch (OpenVSwitch) que soporta 802.1q, comprobar el correcto funcionamiento (el host se asigna a la VLAN que le corresponde según la MAC que tenga).

✗ Crear una app que permita adaptar el número de enlaces troncales a ser agregados basado en el tráfico que está recibiendo el switch. En una red con 2 switches (OpenVSwitch) diferentes interconectados entre sí mediante varios cables (x4), un número variable de enlaces troncales entre switches serán empleados acorde al tráfico que soporta la red en dicho instante.
