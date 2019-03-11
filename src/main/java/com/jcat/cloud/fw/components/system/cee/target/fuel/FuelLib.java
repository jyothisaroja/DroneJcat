package com.jcat.cloud.fw.components.system.cee.target.fuel;

/**
 * FuelLib implementation providing access to the fuel api.
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eolnans - 2014-12-10 - Initial version
 * @author eelimei - 2014-12-17 - Support for multiple roles and addition of hostname for the FuelNode
 * @author eedsla - 2015-02-09 - Updated getNodes method to get correct host name of fuel node
 * @author ehosmol - 2015-02-19 - catch exception and throw fuel exception in getToken()
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.components.system.cee.target.fuel.FuelNode.NodeRole;
import com.jcat.cloud.fw.infrastructure.resources.FuelResource;

/**
 * {@inheritDoc}
 */
public class FuelLib implements IFuelLib {
    /**
     * Used internally to signal when a token needs to re-issued
     */
    private class FuelTokenExpirationException extends Exception {
        private static final long serialVersionUID = -786544480283399109L;
    }

    private static final int KEYSTONE_RETRIES = 1;

    private String mCurrentToken = null;
    private final FuelResource mFuel;

    /**
     * Object meant to be injected using @Inject
     *
     * @param fuel
     *            instance of the fuel resource descriptor
     */
    @Inject
    public FuelLib(FuelResource fuel) {
        this.mFuel = fuel;
    }

    private JSONObject createTokenGenerationJsonParams() {
        JSONObject main = new JSONObject();
        try {
            JSONObject domain = new JSONObject();
            domain.put("id", "default");
            JSONObject user = new JSONObject();
            user.put("name", mFuel.getUser());
            user.put("password", mFuel.getPassword());
            user.put("domain", domain);

            JSONObject password = new JSONObject();
            password.put("user", user);
            JSONArray methods = new JSONArray();
            methods.put("password");
            JSONObject identity = new JSONObject();
            identity.put("methods", methods);
            identity.put("password", password);

            JSONObject auth = new JSONObject();
            auth.put("identity", identity);

            main.put("auth", auth);
        } catch (JSONException e) {
            throw new RuntimeException("Error while processing Keystone input JSON data", e);
        }
        return main;
    }

    private String getToken() throws IOException, FuelAuthorizationException {
        URL url = new URL(mFuel.getKeystoneUrl().toString() + "auth/tokens");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        // add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        String postParams = createTokenGenerationJsonParams().toString();
        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(con.getOutputStream());
        } catch (ConnectException e) {
            if (e.getMessage().contains("refused")) {
                throw new FuelAuthorizationException(
                        "Fuel connection failed. If you are connecting to Fuel via LXC, check required ports are forwarded on LXC");
            }
        }
        wr.writeBytes(postParams);
        wr.flush();
        wr.close();
        int response = con.getResponseCode();
        if (response != 201) {
            throw new FuelAuthorizationException("Could not get token from Fuel Keystone");
        }
        return con.getHeaderField("X-Subject-Token");
    }

    /**
     * Sends a GET request to the URL passed in and returns the response
     *
     * @param url
     * @return the reply
     * @throws IOException
     * @throws FuelTokenExpirationException
     */
    private String sendGet(String url) throws IOException, FuelTokenExpirationException {
        HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(Timeout.CONNECTION_TIMEOUT.getTimeoutInSeconds() * 1000);
        con.setReadTimeout(Timeout.READ_TIMEOUT.getTimeoutInSeconds() * 1000);
        // add request header
        con.setRequestProperty("x-auth-token", mCurrentToken);
        con.setDoOutput(true);

        con.connect();
        int responseCode = con.getResponseCode();
        if (responseCode == 401) {
            throw new FuelTokenExpirationException();
        } else if (responseCode != 200) {
            throw new IOException("Failed to get response");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    /**
     * Wrapper for {@link sendGet(String)} that re-issues a token if a HTTP
     * response indicates an expired token
     *
     * @param url
     * @return the reply
     * @throws IOException
     * @throws FuelAuthorizationException
     */
    private String sendGetWithTokenReauth(String url) throws IOException, FuelAuthorizationException {
        if (mCurrentToken == null) {
            mCurrentToken = getToken();
        }
        int retryCount = 0;
        while (retryCount <= KEYSTONE_RETRIES) {
            try {
                return sendGet(url);
            } catch (FuelTokenExpirationException e) {
                mCurrentToken = getToken();
            }
            retryCount++;
        }
        throw new IOException("No response from service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FuelCluster getCluster(int clusterId) throws IOException, JSONException, FuelAuthorizationException {
        List<FuelCluster> clusters = getClusters();
        for (FuelCluster cluster : clusters) {
            if (cluster.getClusterId() == clusterId) {
                return cluster;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FuelCluster getCluster(String clusterName) throws IOException, JSONException, FuelAuthorizationException {
        List<FuelCluster> clusters = getClusters();
        for (FuelCluster cluster : clusters) {
            if (cluster.getName().equals(clusterName)) {
                return cluster;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FuelCluster> getClusters() throws IOException, JSONException, FuelAuthorizationException {
        String url = mFuel.getFuelUrl().toString() + "clusters";
        String response = sendGetWithTokenReauth(url);
        JSONArray jsonResponse = new JSONArray(response);
        List<FuelCluster> returnList = new ArrayList<FuelCluster>(jsonResponse.length());

        for (int i = 0; i < jsonResponse.length(); i++) {
            JSONObject item = jsonResponse.getJSONObject(i);
            String name = item.getString("name");
            int releaseId = item.getInt("release_id");
            int pendingReleaseId = 0;
            if (!(item.isNull("pending_release_id"))) {
                pendingReleaseId = item.getInt("pending_release_id");
            }
            int clusterId = item.getInt("id");
            String status = item.getString("status");
            returnList.add(new FuelCluster(name, clusterId, releaseId, pendingReleaseId, status));
        }
        return returnList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FuelNode> getNodes() throws IOException, JSONException, FuelAuthorizationException {
        String url = mFuel.getFuelUrl().toString() + "nodes";

        String response = sendGetWithTokenReauth(url);

        JSONArray jsonResponse = new JSONArray(response);
        List<FuelNode> nodes = new ArrayList<FuelNode>(jsonResponse.length());

        for (int i = 0; i < jsonResponse.length(); i++) {
            JSONObject listItem = jsonResponse.getJSONObject(i);
            String clusterId = listItem.getString("cluster");
            String hostName = listItem.getString("name");
            String ip = listItem.getString("ip");
            String roles = listItem.getString("roles");
            String id = listItem.getString("id");
            Boolean onlineStatus = Boolean.valueOf(listItem.getString("online"));
            Set<NodeRole> nodeRoles = new HashSet<NodeRole>();
            for (NodeRole role : NodeRole.values()) {
                if (roles.contains(role.toString())) {
                    nodeRoles.add(role);
                }
            }
            FuelNode node = new FuelNode(id, hostName, clusterId, ip, nodeRoles, onlineStatus);
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FuelNode> getNodesInCluster(FuelCluster cluster) throws IOException, JSONException,
    FuelAuthorizationException {
        return getNodesInCluster(cluster.getClusterId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FuelNode> getNodesInCluster(int clusterId) throws IOException, JSONException,
    FuelAuthorizationException {
        List<FuelNode> nodes = getNodes();
        List<FuelNode> retNodes = new ArrayList<FuelNode>();
        for (FuelNode node : nodes) {
            if (node.getClusterId().equals(clusterId)) {
                retNodes.add(node);
            }
        }
        return retNodes;
    }
}
