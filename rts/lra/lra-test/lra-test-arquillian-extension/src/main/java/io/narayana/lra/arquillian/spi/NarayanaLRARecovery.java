/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package io.narayana.lra.arquillian.spi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.narayana.lra.LRAConstants;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NarayanaLRARecovery implements LRARecoveryService {
    private static final Logger log = Logger.getLogger(NarayanaLRARecovery.class);
    private static final ThreadLocalRandom randomGenerator = ThreadLocalRandom.current();

    /**
     * A bit of hacking to change the internals of annotations defined in LRA TCK.
     * There is need to adjust timeout defined on the annotation definition.
     */
    static {
        String[] resourceClassNames = new String[]{
                "org.eclipse.microprofile.lra.tck.participant.api.LraResource",
                "org.eclipse.microprofile.lra.tck.participant.api.RecoveryResource"};
        for(String resourceClassName: resourceClassNames) {
            try {
                Class<?> clazz = Class.forName(resourceClassName);
                LRAAnnotationAdjuster.processWithClass(clazz);
            } catch (ClassNotFoundException e) {
                log.debugf("Cannot load class %s to adjust LRA annotation on the class", resourceClassName);
            }
        }
    }

    @Override
    public void waitForCallbacks(URI lraId) {
        log.infof("waitForCallback called with lraId: %s", lraId);

        HttpServer server = null;
        ThreadPoolExecutor threadPoolExecutor = null;
        String httpCallbackSuffix = Integer.toString(randomGenerator.nextInt(Integer.MAX_VALUE));
        String callbackServerHost = "localhost"; // TODO: parametrize me
        int callbackServerPort = 8181; // TODO: parametrize me
        int timeout_s = (int) TimeAdjuster.adjust(10); // TODO: make the constant from me
        MyHttpHandler httpHandler = new MyHttpHandler(httpCallbackSuffix);

        try {
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            server = HttpServer.create(new InetSocketAddress(callbackServerHost, callbackServerPort), 0);
            server.createContext("/", httpHandler);
            server.setExecutor(threadPoolExecutor);
            server.start();
            log.infof(" Server started on port %d", callbackServerPort);

            Client client = ClientBuilder.newClient();
            String coordinatorHost = lraId.getHost();
            int coordinatorPort = lraId.getPort();
            String coordinatorEnlistUrl = String.format("http://%s:%d/%s/%s",
                    coordinatorHost, coordinatorPort, LRAConstants.COORDINATOR_PATH_NAME,
                    URLEncoder.encode(lraId.toASCIIString(), StandardCharsets.UTF_8.name()));
            String linkTargetBase = "http://" + callbackServerHost + ":" + String.valueOf(callbackServerPort); // TODO: what about IPv6?
            String link = String.format("<%s/compensate-%s>;rel=\"compensate\", <%s/after-%s>;rel=\"after\"",
                    linkTargetBase, httpCallbackSuffix, linkTargetBase, httpCallbackSuffix);
            Response response = client.target(coordinatorEnlistUrl)
                    .request()
                    .header("Link", link)
                    .put(Entity.text(""));
            if (Response.Status.fromStatusCode(response.getStatus()).getFamily().equals(Response.Status.Family.CLIENT_ERROR)) {
                // We are fine here to return! Considering the LRA has been already finished.
                // We can't enlist, the LRA does not exist anymore or(!) yet. Either way we can't enlist the callback and wait for it. Ok.
            } else if (Response.Status.fromStatusCode(response.getStatus()).getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                // waiting for callback
                log.infof("Successfully enlisted the 'waitForCallback' with compensator content %s", link);
                long startTime = System.currentTimeMillis();
                while(!httpHandler.isAfterLRACalled()) {
                    if(startTime + TimeUnit.SECONDS.toMillis((long) timeout_s) < System.currentTimeMillis()) {
                        throw new RuntimeException("Callback with 'waitForCallback' was not received until timeout of "
                                + timeout_s + " seconds elapsed.");
                    }
                    Thread.yield();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot create HTTPServer to serve callbacks", e);
        } finally {
            if (server != null) {
                server.stop(timeout_s);
            }
            if (threadPoolExecutor != null) {
                threadPoolExecutor.shutdown();
                try {
                    if (!threadPoolExecutor.awaitTermination(timeout_s, TimeUnit.SECONDS)) {
                        threadPoolExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    threadPoolExecutor.shutdownNow();
                }
            }
        }
    }

    private static class MyHttpHandler implements HttpHandler {
        private volatile boolean afterLRACalled = false;
        private final String completeCompensatePath;

        public MyHttpHandler(String completeCompensatePath) {
            this.completeCompensatePath = completeCompensatePath;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue = null;
            log.infof(">>>>>> handle called with httpExchange: %s", httpExchange);
            if ("PUT".equals(httpExchange.getRequestMethod())) {
                log.info(">>>> PUT request received at: " + httpExchange.getRequestURI().toASCIIString());
                if(httpExchange.getRequestURI().toASCIIString().endsWith(completeCompensatePath)) {
                    afterLRACalled = true;
                }
            }
            // return OK (200) to caller
            handleResponse(httpExchange);
        }

        private void handleResponse(HttpExchange httpExchange) throws IOException {
            OutputStream outputStream = httpExchange.getResponseBody();
            httpExchange.sendResponseHeaders(200, 0);
            outputStream.flush();
            outputStream.close();
        }

        public boolean isAfterLRACalled() {
            return afterLRACalled;
        }
    }

    @Override
    public boolean waitForEndPhaseReplay(URI lraId) {
        String host = lraId.getHost();
        int port = lraId.getPort();
        if (!recoverLRAs(host, port, lraId)) {
            // first recovery scan probably collided with periodic recovery which started
            // before the test execution so try once more
            return recoverLRAs(host, port, lraId);
        }

        return true;
    }

    /**
     * Invokes LRA coordinator recovery REST endpoint and returns whether the recovery of intended LRAs happended
     *
     * @param host  the LRA coordinator host address
     * @param port  the LRA coordinator port
     * @param lraId the LRA id of the LRA that is intended to be recovered
     * @return true the intended LRA recovered, false otherwise
     */
    private boolean recoverLRAs(String host, int port, URI lraId) {
        // trigger a recovery scan
        Client recoveryCoordinatorClient = ClientBuilder.newClient();

        try {
            String recoveryCoordinatorUrl = String.format("http://%s:%d/%s/recovery",
                host, port, LRAConstants.RECOVERY_COORDINATOR_PATH_NAME);
            WebTarget recoveryTarget = recoveryCoordinatorClient.target(URI.create(recoveryCoordinatorUrl));

            // send the request to the recovery coordinator
            Response response = recoveryTarget.request().get();
            String json = response.readEntity(String.class);
            response.close();

            if (json.contains(lraId.toASCIIString())) {
                // intended LRA didn't recover
                return false;
            }

            return true;
        } finally {
            recoveryCoordinatorClient.close();
        }

    }
}
