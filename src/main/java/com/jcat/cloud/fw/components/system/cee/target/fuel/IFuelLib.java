package com.jcat.cloud.fw.components.system.cee.target.fuel;

import java.io.IOException;
import java.util.List;

import org.codehaus.jettison.json.JSONException;

/**
 * Used to interface the Fuel Master of the SUT Can be injected by: <code>
 *
 * @Inject IFuelLib mFuelLib; </code>
 */
public interface IFuelLib {
    /**
     * Get a specific Cluster by it numeric ID
     *
     * @param clusterId
     *            the id to match
     * @throws FuelAuthorizationException
     * @throws JSONException
     * @throws IOException
     */
    public FuelCluster getCluster(int clusterId) throws IOException, JSONException, FuelAuthorizationException;

    /**
     * Get a specific Cluster by its textual name
     *
     * @param clusterName
     *            the id to match
     * @throws FuelAuthorizationException
     * @throws JSONException
     * @throws IOException
     */
    public FuelCluster getCluster(String clusterName) throws IOException, JSONException, FuelAuthorizationException;

    /**
     * Get a list of all Clusters
     *
     * @throws FuelAuthorizationException
     * @throws JSONException
     * @throws IOException
     */
    public List<FuelCluster> getClusters() throws IOException, JSONException, FuelAuthorizationException;

    /**
     * Get a list of all all Nodes in a Cluster
     *
     * @param clusterId
     *            the Id of the wanted cluster
     * @throws FuelAuthorizationException
     * @throws JSONException
     * @throws IOException
     */
    public List<FuelNode> getNodesInCluster(int clusterId) throws IOException, JSONException,
            FuelAuthorizationException;

    /**
     * Get a list of all all Nodes in a Cluster
     *
     * @param clusterId
     *            the Id of the wanted cluster
     * @throws FuelAuthorizationException
     * @throws JSONException
     * @throws IOException
     */
    public List<FuelNode> getNodesInCluster(FuelCluster cluster) throws IOException, JSONException,
            FuelAuthorizationException;

    /**
     * Get a list of all Nodes in all Clusters
     *
     * @throws FuelAuthorizationException
     * @throws JSONException
     * @throws IOException
     */
    public List<FuelNode> getNodes() throws IOException, JSONException, FuelAuthorizationException;
}
