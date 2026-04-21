package com.mcmp.o11ymanager.manager.service.cache;

import java.util.Objects;

/**
 * Cache key for a cb-spider monitoring query. {@link Scope} distinguishes VM vs cluster-node
 * variants, and the remaining fields carry whatever parameters cb-spider uses to fan out to the
 * underlying CSP API.
 */
public record CspCacheKey(
        Scope scope,
        String identifier,
        String measurement,
        String connectionName,
        String timeBeforeHour,
        String intervalMinute) {

    public enum Scope {
        VM,
        CLUSTER_NODE
    }

    public static CspCacheKey forVm(
            String vmName,
            String measurement,
            String connectionName,
            String timeBeforeHour,
            String intervalMinute) {
        return new CspCacheKey(
                Scope.VM,
                nullToEmpty(vmName),
                nullToEmpty(measurement),
                nullToEmpty(connectionName),
                nullToEmpty(timeBeforeHour),
                nullToEmpty(intervalMinute));
    }

    public static CspCacheKey forClusterNode(
            String clusterName,
            String nodeGroupName,
            String nodeNumber,
            String measurement,
            String connectionName,
            String timeBeforeHour,
            String intervalMinute) {
        String id =
                nullToEmpty(clusterName)
                        + "/"
                        + nullToEmpty(nodeGroupName)
                        + "/"
                        + nullToEmpty(nodeNumber);
        return new CspCacheKey(
                Scope.CLUSTER_NODE,
                id,
                nullToEmpty(measurement),
                nullToEmpty(connectionName),
                nullToEmpty(timeBeforeHour),
                nullToEmpty(intervalMinute));
    }

    private static String nullToEmpty(String s) {
        return Objects.toString(s, "");
    }
}
