package loadbalancers

import "github.com/cloud-barista/ktcloudvpc-sdk-go"

const (
	rootPath       		= "?command=listLoadBalancers" // Temperary

	listCommand       	= "?command=listLoadBalancers"
	createCommand     	= "?command=createLoadBalancer"
	deleteCommand     	= "?command=deleteLoadBalancer"
	addServerCommand  	= "?command=addLoadBalancerWebServer"
	removeServerCommand = "?command=removeLoadBalancerWebServer"
	listLbServerCommand = "?command=listLoadBalancerWebServers"
	createTagCommand   	= "?command=createTag"
	resourcePath   		= ""
	statusPath     		= "status"
	statisticsPath 		= "stats"
	failoverPath   		= "failover"
)

// func rootURL(c *gophercloud.ServiceClient) string {
// 	return c.GetServiceURL(rootPath, resourcePath)
// }

func listNlbURL(c *gophercloud.ServiceClient) string {
	return c.GetServiceURL(listCommand, resourcePath)
}

func createNlbURL(c *gophercloud.ServiceClient) string {
	return c.GetServiceURL(createCommand, resourcePath)
}

func deleteNlbURL(c *gophercloud.ServiceClient) string {
	return c.GetServiceURL(deleteCommand, resourcePath)
}

func addServerURL(c *gophercloud.ServiceClient) string {
	return c.GetServiceURL(addServerCommand, resourcePath)
}

func removeServerURL(c *gophercloud.ServiceClient) string {
	return c.GetServiceURL(removeServerCommand, resourcePath)
}

func listLbServerURL(c *gophercloud.ServiceClient) string {
	return c.GetServiceURL(listLbServerCommand, resourcePath)
}

func createTagURL(c *gophercloud.ServiceClient) string {
	return c.GetServiceURL(createTagCommand, resourcePath)
}

func resourceURL(c *gophercloud.ServiceClient, id string) string {
	return c.GetServiceURL(rootPath, resourcePath, id)
}

func statusRootURL(c *gophercloud.ServiceClient, id string) string {
	return c.GetServiceURL(rootPath, resourcePath, id, statusPath)
}

func statisticsRootURL(c *gophercloud.ServiceClient, id string) string {
	return c.GetServiceURL(rootPath, resourcePath, id, statisticsPath)
}

func failoverRootURL(c *gophercloud.ServiceClient, id string) string {
	return c.GetServiceURL(rootPath, resourcePath, id, failoverPath)
}
