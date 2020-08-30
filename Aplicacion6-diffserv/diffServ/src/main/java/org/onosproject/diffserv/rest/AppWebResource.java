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
 */package org.onosproject.diffserv.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;

//import org.onosproject.diffserv.QoSRestService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
//import javax.ws.rs.POST;
import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Sample web resource.
 */

@Path("store")
public class AppWebResource extends AbstractWebResource {

    /**
     * Get hello world greeting.
     *
     * @return 200 OK
     */
    @GET
    @Path("test")
    public Response getGreeting() {
        ObjectNode node = mapper().createObjectNode().put("hello", "world");
        return ok(node).build();
    }

  /*  @POST
    @Path("qos/{idQueue}/{maxRate}/{minRate}/{portQoS}/{portQueue}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addQos(@PathParam("idQueue") String idQueue,
    @PathParam("maxRate") int maxRate, @PathParam("minRate") int minRate,
    @PathParam("portQoS") int portQoS, @PathParam("portQueue") int portQueue) {


    QoSRestService qosService = get(QoSRestService.class);
    //Creamos la cola con los parametros enviados en la URI
    qosService.addQueue(idQueue, maxRate, minRate, portQoS, portQueue);
    //Si todo es correcto devolvería el estado 200 OK
    return Response.status(200).build();
    }*/

}
